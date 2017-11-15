package pro.eugw.lessoncountdown;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MService extends Service {

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    Boolean running;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (running != null ? running : false) {
            Toast.makeText(this, "Service already started", Toast.LENGTH_SHORT).show();
            return START_STICKY;
        }
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        running = true;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ID = "LessonChannel2";
        mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LessonCountdown")
                .setContentText("LessonCountdown")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        else {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "LessonChannel", NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        try {
            JsonObject object = new JsonParser().parse(new FileReader(new File(Environment.getExternalStorageDirectory() + File.separator +  "LessonCountdown", "table.json"))).getAsJsonObject();
            String dayOfWeek = new SimpleDateFormat("EEEE", Locale.US).format(new Date());
            JsonArray epochArr = new JsonArray();
            for (JsonElement element : object.get(dayOfWeek).getAsJsonArray()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                SimpleDateFormat yrr = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                String startS = yrr.format(new Date()) + " " + element.getAsString().split("-")[0];
                Date startD = sdf.parse(startS);
                Long start = startD.getTime();
                String endS = yrr.format(new Date()) + " " + element.getAsString().split("-")[1];
                Date endD = sdf.parse(endS);
                Long end = endD.getTime();
                String lesson = element.getAsString().split("-")[2];
                epochArr.add(start + "-" + end + "-" + lesson);
            }
            new Thread(() -> {
                Long time = System.currentTimeMillis();
                while (running && time <= Long.parseLong(epochArr.get(epochArr.size() - 1).getAsString().split("-")[1])) {
                    for (Integer i = 0; i < epochArr.size(); i++) {
                        String[] element = epochArr.get(i).getAsString().split("-");
                        Long start = Long.parseLong(element[0]);
                        Long end = Long.parseLong(element[1]);
                        Long current = System.currentTimeMillis();
                        time = current;
                        if (start <= current && current <= end) {
                            StringBuilder title = new StringBuilder("Current: " + element[2]);
                            if (i < epochArr.size() - 1)
                                title.append(" - Next: ").append(epochArr.get(i + 1).getAsString().split("-")[2]);
                            mBuilder.setContentText(((end - current) / 60000) + "min " + new DecimalFormat("##").format(((end - current) / 1000) % 60) + "sec");
                            mBuilder.setContentTitle(title);
                        } else if (i < epochArr.size() - 1) {
                            String[] nexEl = epochArr.get(i + 1).getAsString().split("-");
                            Long nexS = Long.parseLong(nexEl[0]);
                            if (end < current && current < nexS) {
                                mBuilder.setContentText(((nexS - current) / 60000) + "min " + new DecimalFormat("##").format(((nexS - current) / 1000) % 60) + "sec");
                                mBuilder.setContentTitle("Next: " + nexEl[2]);
                            }
                        }
                        mNotificationManager.notify(0, mBuilder.build());
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                stopSelf();
            }).start();
        } catch (FileNotFoundException | ParseException e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        running = false;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBuilder.setContentTitle("LessonCountdown");
        mBuilder.setContentText("Service stopped");
        mBuilder.setOngoing(false);
        mNotificationManager.notify(0, mBuilder.build());
        super.onDestroy();
    }

}
