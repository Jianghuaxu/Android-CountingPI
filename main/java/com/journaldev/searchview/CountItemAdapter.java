package com.journaldev.searchview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;


import java.util.ArrayList;

import gson.PIItems;
import modelHelper.StorageBinHelper;

public class CountItemAdapter extends ArrayAdapter<PIItems> implements View.OnClickListener{
    private ArrayList<PIItems> listData;
    private int resourceId;
    private Callback callback;
    public interface Callback{
         //void onRadioButtonClicked(View v);
         void onQuantityChanged(int itemPosition, String value);
         void onAddQuantity(View v);
         void onReduceQuantity(View v);
    }

    public CountItemAdapter(@NonNull Context context, int resource,  ArrayList<PIItems> piItemList, Callback callback) {
        super(context, resource, piItemList);
        this.callback = callback;
        resourceId = resource;
        listData = piItemList;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    public ArrayList<PIItems> getListData(){
        return listData;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.initViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        PIItems item_line = listData.get(position);
        viewHolder.ProductView.setText(item_line.Product);
        viewHolder.ProductDescriptionView.setText(item_line.ProductDescription);
        viewHolder.QuantityView.setText(item_line.ProductQuantity);
        viewHolder.UOMView.setText(item_line.ProductQuantityUoM);
        /*viewHolder.HUEmptyView.setTag(position);
        viewHolder.HUEmptyView.setOnClickListener(this);
        viewHolder.HUMissingView.setOnClickListener(this);
        viewHolder.HUMissingView.setTag(position);*/
        viewHolder.AddQuantityView.setOnClickListener(this);
        viewHolder.AddQuantityView.setTag(position);
        viewHolder.ReduceQuantityView.setOnClickListener(this);
        viewHolder.ReduceQuantityView.setTag(position);
        viewHolder.QuantityView.setTag(position);
//        viewHolder.QuantityView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                quantityPosition = (int)v.getTag();
//            }
//        });
        viewHolder.QuantityView.addTextChangedListener(new TextSwitcher(viewHolder));
        //dynamically show content of each view
        if(item_line.Product.equals("")) {
            //in case no product exist in this line, then no change quantity is possible
            viewHolder.ChangeQuantityLayout.setVisibility(View.INVISIBLE);
        }
        if(!item_line.Batch.equals("")) {
            viewHolder.BatchLayout.setVisibility(View.VISIBLE);
            String batchProperty = item_line.Batch;
            viewHolder.BatchView.setText(batchProperty);
        } else {
            viewHolder.BatchLayout.setVisibility(View.GONE);
        }
        if(!item_line.Owner.equals("") && !StorageBinHelper.isOwnerSame(listData)) {
            viewHolder.OwnerLayout.setVisibility(View.VISIBLE);
            String batchProperty = item_line.Owner;
            viewHolder.OwnerView.setText(batchProperty);
        } else {
            viewHolder.OwnerLayout.setVisibility(View.GONE);
        }
        return convertView;
    }

    class TextSwitcher implements TextWatcher {
        private ViewHolder mHolder;

        public TextSwitcher(ViewHolder mHolder) {
            this.mHolder = mHolder;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            int position = (int) mHolder.QuantityView.getTag();//取tag值
            callback.onQuantityChanged(position, s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    @Override
    public void onClick(View v) {
        //callback.onRadioButtonClicked(v);
        switch (v.getId()) {
            case R.id.add_quantity:
                callback.onAddQuantity(v);
                break;
            case R.id.reduce_quantity:
                callback.onReduceQuantity(v);
                break;
        }
        notifyDataSetChanged();
    }

    public void refreshStorageBin(ArrayList<PIItems> items) {
        addAll(items);
        notifyDataSetChanged();
    }

    class ViewHolder {
        TextView HUView;
        TextView ProductView;
        TextView ProductDescriptionView;
        EditText QuantityView;
        TextView UOMView;
        FloatingActionButton AddQuantityView;
        FloatingActionButton ReduceQuantityView;
        TextView ProductPropertyView;
        LinearLayout BatchLayout;
        TextView BatchView;
        LinearLayout OwnerLayout;
        TextView OwnerView;
        /*LinearLayout layout_product;
        RadioButton HUMissingView;
        RadioButton HUEmptyView;
        LinearLayout layout_quantity;
        LinearLayout layout_countItem;*/
        LinearLayout ChangeQuantityLayout;
        Switch BinEmptyView;

        public void initViewHolder(View view){
            //HUView = (TextView) view.findViewById(R.id.hu);
            ProductView = (TextView) view.findViewById(R.id.product);
            ProductDescriptionView = (TextView) view.findViewById(R.id.productdesc);
            QuantityView = (EditText) view.findViewById(R.id.quantity);
            UOMView = (TextView) view.findViewById(R.id.uom);
            BinEmptyView = (Switch) view.findViewById(R.id.bin_empty);
            AddQuantityView = (FloatingActionButton) view.findViewById(R.id.add_quantity);
            ReduceQuantityView = (FloatingActionButton) view.findViewById(R.id.reduce_quantity);
            ChangeQuantityLayout = (LinearLayout) view.findViewById(R.id.change_quantity);
            BatchLayout = (LinearLayout) view.findViewById(R.id.batch_layout);
            BatchView = (TextView) view.findViewById(R.id.batch);
            OwnerLayout = (LinearLayout) view.findViewById(R.id.owner_layout);
            OwnerView = (TextView) view.findViewById(R.id.owner);
            /*HUMissingView = (RadioButton) view.findViewById(R.id.hu_missing);
            HUEmptyView = (RadioButton) view.findViewById(R.id.hu_empty);*/

        }
    }
}
