package pro.eugw.lessoncountdown;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

class MHolder extends RecyclerView.ViewHolder {

    TextView name;
    TextView time;

    MHolder(View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.textName);
        time = itemView.findViewById(R.id.textTime);
    }

}
