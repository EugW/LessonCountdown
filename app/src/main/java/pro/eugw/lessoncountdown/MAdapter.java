package pro.eugw.lessoncountdown;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;


public class MAdapter extends RecyclerView.Adapter<MHolder> {

    private List<MLesson> list = Collections.emptyList();

    MAdapter(List<MLesson> l) {
        list = l;
    }

    @Override
    public MHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lesson_view, parent, false);
        return new MHolder(view);
    }

    @Override
    public void onBindViewHolder(MHolder holder, int position) {
        String nameS = position + ". " + list.get(position).name;
        String timeS = list.get(position).time;
        holder.name.setText(nameS);
        holder.time.setText(timeS);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}
