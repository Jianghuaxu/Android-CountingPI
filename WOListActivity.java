package com.journaldev.searchview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.journaldev.searchview.databinding.ActivityWolistBinding;

import gson.PIWOList;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import util.HttpUtil;

public class WOListActivity extends AppCompatActivity {
    ArrayList<String> arrayList = new ArrayList<>();
    ActivityWolistBinding woBinding;
    ImageView warehouse_pic;
    String url;
    String username;
    String password;

    OkHttpClient client;

    WOListItemAdapter<String> adapter;

    final List<PIWOList> woList = new ArrayList<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        woBinding = DataBindingUtil.setContentView(WOListActivity.this, R.layout.activity_wolist);

        //get intent data from last activity
        Intent intent = getIntent();
        url = intent.getStringExtra("url") + "&$format=json";
        sendRequestsWithOkHttp(url);

        woBinding.secondListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //need to get the relevant key for current selected item
                PIWOList wo_selected = woList.get(i);
                String woNumber = wo_selected.WarehouseOrder;
                String piDocUuid = String.valueOf(wo_selected.PhysicalInventoryDocumentGUID);
                Intent intent = new Intent(WOListActivity.this, CountActivity.class);
                intent.putExtra("WO_Number", woNumber);
                intent.putExtra("PI_DOC_UUID", piDocUuid);
                intent.putExtra("Count_date", wo_selected.CountDate);
                startActivityForResult(intent, 1);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 1:
                Toast.makeText(WOListActivity.this, "Counting finished", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendRequestsWithOkHttp(String url){

        HttpUtil.sendOkHttpRequest( url, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("Response", "OK");
                final String responseData = response.body().string();
                Log.d("Response", "responseData");
                try {
                    JSONObject Jobject = new JSONObject(responseData);
                    JSONObject d_results = Jobject.getJSONObject("d");
                    JSONArray results = d_results.getJSONArray("results");
                    String wo_content;
                    PIWOList wo;
                    for(int i = 0; i< results.length(); i++) {
                        wo_content = results.getJSONObject(i).toString();
                        wo = new Gson().fromJson(wo_content, PIWOList.class);
                        woList.add(wo);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showResponseData(woList);
                        }
                    });
                }catch(Exception err) {
                    Log.d("Response", err.getMessage());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
            }
        });
    }

    public void showResponseData(List<PIWOList> list) {
        String WONumber;
        for(int i = 0; i < list.size(); i++) {
            WONumber = list.get(i).WarehouseOrder;
            arrayList.add(WONumber);
        }
        adapter = new WOListItemAdapter<String>(WOListActivity.this, R.layout.wolist_item, arrayList);
        woBinding.secondListView.setAdapter(adapter);
    }




}
