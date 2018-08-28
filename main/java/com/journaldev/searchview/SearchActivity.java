package com.journaldev.searchview;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.journaldev.searchview.databinding.ActivitySearchBinding;

import util.HttpUtil;


public class SearchActivity extends AppCompatActivity implements View.OnClickListener{
    private ActivitySearchBinding activitySearchBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySearchBinding = DataBindingUtil.setContentView(this, R.layout.activity_search);
        activitySearchBinding.searchPi.setOnClickListener(this);

        //initialize drawerlayout


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_pi:
                attemptSearchPIList();
                break;
        }
    }

    private void attemptSearchPIList() {
        //handle warehouse number input, which is mandatory
        String whn = activitySearchBinding.warehouseNumber.getText().toString();
        if(whn.equals("")) {
            activitySearchBinding.warehouseNumber.requestFocus();
            return;
        }
        HttpUtil.setWareHouseNumber(whn);
        String woNumber = activitySearchBinding.warehouseOrder.getText().toString();
        String storageType = activitySearchBinding.storageType.getText().toString();
        String aisle = activitySearchBinding.aisle.getText().toString();
        String piListUrl = HttpUtil.getPIListUrl(woNumber, storageType, aisle);

        Intent intent = new Intent(SearchActivity.this, MainActivity.class);
        intent.putExtra("PIlist_Url", piListUrl);
        startActivity(intent);
    }


}
