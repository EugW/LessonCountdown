package pro.eugw.lessoncountdown;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File prePath = new File(Environment.getExternalStorageDirectory(), "LessonCountdown");
        File table = new File(prePath, "table.json");
        try {
            if (!prePath.exists())
                if (prePath.mkdirs())
                    System.out.println("crt");
            if (!table.exists()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.getApplicationContext().getResources().openRawResource(R.raw.table)));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(table)));
                String rd;
                while ((rd = reader.readLine()) != null) {
                    writer.write(rd);
                    writer.newLine();
                }
                reader.close();
                writer.flush();
                writer.close();
            }
            JsonObject object = new JsonParser().parse(new FileReader(table)).getAsJsonObject();
            if (!object.has("Version")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.getApplicationContext().getResources().openRawResource(R.raw.table)));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(table)));
                String rd;
                while ((rd = reader.readLine()) != null) {
                    writer.write(rd);
                    writer.newLine();
                }
                reader.close();
                writer.flush();
                writer.close();
            } else if (object.get("Version").getAsInt() != BuildConfig.VERSION_CODE) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.getApplicationContext().getResources().openRawResource(R.raw.table)));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(table)));
                String rd;
                while ((rd = reader.readLine()) != null) {
                    writer.write(rd);
                    writer.newLine();
                }
                reader.close();
                writer.flush();
                writer.close();
            }
            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            List<MLesson> list = new ArrayList<>();
            MAdapter adapter = new MAdapter(list);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            for (JsonElement element : object.get(new SimpleDateFormat("EEEE", Locale.US).format(new Date())).getAsJsonArray()) {
                String[] s = element.getAsString().split("-");
                list.add(new MLesson(s[2], s[0] + "-" + s[1]));
            }
            adapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, MService.class);
        startService(intent);
        findViewById(R.id.buttonStart).setOnClickListener(view -> startService(intent));
        findViewById(R.id.buttonStop).setOnClickListener(view -> stopService(intent));
    }

}
