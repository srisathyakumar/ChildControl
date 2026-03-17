package com.child.app.child;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.child.app.R;
import java.util.List;

public class AppResultAdapter extends ArrayAdapter<String> {
    public AppResultAdapter(Context context, List<String> data) {
        super(context, 0, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.app_item, parent, false);
        }
        TextView txtAppName = convertView.findViewById(R.id.txtAppName);
        TextView txtRisk = convertView.findViewById(R.id.txtRisk);
        TextView txtStatus = convertView.findViewById(R.id.txtStatus);
        
        String value = getItem(position);
        if (value != null) {
            String[] parts = value.split(",");
            if (parts.length >= 3) {
                String packageName = parts[0];
                String risk = parts[1];
                String status = parts[2];
                txtAppName.setText(packageName);
                txtRisk.setText("Risk: " + risk + "%");
                txtStatus.setText("Status: " + status);
                if (status.equals("MALICIOUS")) {
                    txtStatus.setTextColor(Color.RED);
                } else {
                    txtStatus.setTextColor(Color.parseColor("#2E7D32"));
                }
            } else {
                txtAppName.setText(value);
                txtRisk.setText("");
                txtStatus.setText("");
            }
        }
        return convertView;
    }
}
