package com.journaldev.searchview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class WOListItemAdapter<String> extends ArrayAdapter {
    int ResourceID;
    View bin_view;
    List<String> list;

    public WOListItemAdapter(@NonNull Context context, int resource, @NonNull List objects) {
        super(context, resource, objects);
        ResourceID = resource;
        list = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View bin_view2 = parent;
        final String bin_number = list.get(position);
        bin_view= LayoutInflater.from(getContext()).inflate(ResourceID, parent, false);
        TextView textView = (TextView) bin_view.findViewById(R.id.bin_number);
        textView.setText(java.lang.String.valueOf(bin_number));

        return bin_view;
    }
}
