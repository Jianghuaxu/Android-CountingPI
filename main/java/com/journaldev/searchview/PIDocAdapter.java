package com.journaldev.searchview;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PIDocAdapter extends ArrayAdapter<PIDoc> implements Filterable{
    private int resourceId;
    private ViewHolder viewHolder;
    ArrayList<PIDoc> list;
    ArrayList<PIDoc> filterList;



    public PIDocAdapter(Context context, int resource, ArrayList<PIDoc> objects) {
        super(context, resource, objects);
        resourceId = resource;
        filterList = objects;
        list = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        PIDoc piDoc = getItem(position);

        if(convertView != null) {
            view = convertView;
        } else {
            //LayoutInflater是用来找res/layout/下的xml布局文件，并且实例化；而findViewById()是找xml布局文件下的具体widget控件
            //ViewDataBinding view2 =  DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), resourceId, parent, false);
            //view = view2.getRoot();
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) view.findViewById(R.id.image_view);
            viewHolder.textView = (TextView) view.findViewById(R.id.stringName);
            view.setTag(viewHolder);
        }
        viewHolder.imageView.setImageResource(piDoc.getImageResourceById());
        viewHolder.textView.setText(String.valueOf(piDoc.getPIDocNumber()));

        return view;
    }

    @Nullable
    @Override
    public PIDoc getItem(int position) {
        return list.get(position);
    }

    public String getWONumber(int index) {
        return getItem(index).getPIDocNumber();
    }

    public int getImageId(int index) {
        try{
            return getItem(index).getImageId();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return 0;

    }


    @Override
    public int getCount() {
        return list.size();
    }

    //object to contain multiple views in ItemView: R.id.stringName
    class ViewHolder{
        ImageView imageView;
        TextView textView;
    }

    @Override
    public void add(@Nullable PIDoc object) {
        list.add(object);
        notifyDataSetChanged();
    }

    public ArrayList<PIDoc> getList() {
        return list;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new ItemFilter();
    }

    private class ItemFilter extends Filter{
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            if (constraint != null && constraint.length() > 0) {
                ArrayList<PIDoc> filterList2 = new ArrayList<>();
                for (int i = 0; i < filterList.size(); i++) {
                    if((String.valueOf(filterList.get(i).getPIDocNumber()).toUpperCase()).contains(constraint.toString().toUpperCase())) {
                        filterList2.add(filterList.get(i));
                    }
                }
                filterResults.count = filterList2.size();
                filterResults.values = filterList2;
            } else {
                filterResults.count = filterList.size();
                filterResults.values = filterList;
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            list = (ArrayList<PIDoc>) filterResults.values;
            Integer listSize = getCount();
            notifyDataSetChanged();
        }
    }

    public void refreshPIList(ArrayList<PIDoc> piDocs) {
        filterList = piDocs;
        list = piDocs;
    }
}
