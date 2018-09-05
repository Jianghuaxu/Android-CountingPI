package com.journaldev.searchview;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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
import model.PIItemsUrlLoad;
import model.WarehouseOrderCount;
import modelHelper.StorageBinHelper;
import modelHelper.WOCountHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import util.HttpUtil;
import util.LocalStorageUtil;
import util.Util;

public class WOListActivity extends AppCompatActivity  implements View.OnClickListener{
    ArrayList<String> arrayList = new ArrayList<>();
    ActivityWolistBinding woBinding;
    ImageView warehouse_pic;
    String url;
    String username;
    String password;

    //handle saveAll WOs
    int responseCount = 0;
    int numberOfPutRequest = 0;
    boolean hasError = false;
    ProgressDialog progressDialog;

    //handle source activity
    private Intent receivedIntent;

    //handle unsaved WOs
    boolean isUnsavedWOScenario = false;

    OkHttpClient client;

    WOListItemAdapter<String> adapter;

    final List<PIWOList> woList = new ArrayList<>();


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.save_unsaved_wo:
                //handle save all the wo to backend!
                saveAllWO();
        }
    }

    private void saveAllWO() {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        Util.showProgressDialog(progressDialog, true);
        WarehouseOrderCount woCount = new Gson().fromJson(LocalStorageUtil.getUnsavedData(WOListActivity.this, arrayList.get(0)), WarehouseOrderCount.class);
        StorageBinHelper.setBinArrayList(woCount.binArrayList);
        ArrayList<PIItemsUrlLoad> piItemsUrlLoadArrayList = StorageBinHelper.preparePutRequestLoad(woCount.CountDate);
        numberOfPutRequest = piItemsUrlLoadArrayList.size();
        responseCount = 0;
        for(PIItemsUrlLoad piItemsUrlLoad: piItemsUrlLoadArrayList) {
            sendPostRequest(piItemsUrlLoad.url, piItemsUrlLoad.requestBody);
        }
    }

    private void sendPostRequest(String postUri, String postLoad) {

        HttpUtil.saveOkHttpPostRequest(postUri, postLoad, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handlePostResponseData(true);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    handlePostResponseData(false);
                }else {
                    handlePostResponseData(true);
                }

            }
        });
    }

    private void handlePostResponseData(boolean error) {
        responseCount++;
        if(error) {
            hasError = true;
        }
        if(responseCount == numberOfPutRequest) {
            onFinalSaveResponseReceived();
        }

    }

    private void onFinalSaveResponseReceived() {
        if(hasError) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Util.showProgressDialog(progressDialog, false);
                    Toast.makeText(WOListActivity.this, "Save failure", Toast.LENGTH_LONG).show();
                }
            });
        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Util.showProgressDialog(progressDialog, false);
                    adapter.clear();
                    LocalStorageUtil.deleteWO(WOListActivity.this, arrayList.get(0));
                    Toast.makeText(WOListActivity.this, "Save successfully", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(WOListActivity.this);
                    alertDialogBuilder.setMessage("All wo are saved successfully, do you want to continue? ");
                    alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(WOListActivity.this, SearchActivity.class);
                            startActivity(intent);
                        }
                    });
                    alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    alertDialogBuilder.create().show();

                }
            });
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        woBinding = DataBindingUtil.setContentView(WOListActivity.this, R.layout.activity_wolist);

        //set toolbar
        Toolbar toolbar = woBinding.toolbarWolist;
        setSupportActionBar(toolbar);

        //get intent data from last activity
        receivedIntent = getIntent();
        Button saveAllButton = woBinding.saveUnsavedWo;
        if(receivedIntent.getStringExtra("handleLocal").equals("true")) {
            isUnsavedWOScenario = true;
            //handle unsaved WO
            showUnsavedWOData();
            saveAllButton.setVisibility(View.VISIBLE);
            saveAllButton.setOnClickListener(this);
            woBinding.woListTitle.setText("Unsaved Warehouse Orders(" + arrayList.size() + ")");
        } else {
            if(saveAllButton.getVisibility() == View.VISIBLE) {
                saveAllButton.setVisibility(View.INVISIBLE);
            }
            url = receivedIntent.getStringExtra("url") + "&$format=json";
            sendRequestsWithOkHttp(url);
        }



        woBinding.secondListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(WOListActivity.this, CountActivity.class);
                intent.putExtra("WO_Number", arrayList.get(i));
                //we differentiate between unsaved-Scenario & normal Scenario
                if(isUnsavedWOScenario) {
                    intent.putExtra("handleLocal", "true");
                } else {
                    intent.putExtra("handleLocal", "false");
                    //for normal scenario WONumber and PI Guid is needed to get piItems from backend, whereas for unsavedWO, only WONumber is necessary
                    PIWOList wo_selected = woList.get(i);
                    String piDocUuid = String.valueOf(wo_selected.PhysicalInventoryDocumentGUID);
                    intent.putExtra("PI_DOC_UUID", piDocUuid);
                    intent.putExtra("Count_date", wo_selected.CountDate);
                }
                intent.putExtra("isGuidedMode", "false");

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
                ResponseBody responseBody = response.body();
                try {
                    Log.d("Response", "OK");
                    final String responseData = responseBody.string();
                    Log.d("Response", responseData);
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
                }finally {
                    responseBody.close();
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
        woBinding.woListTitle.setText("Warehouse Orders(" + arrayList.size() + ")");
        adapter = new WOListItemAdapter<String>(WOListActivity.this, R.layout.wolist_item, arrayList);
        woBinding.secondListView.setAdapter(adapter);
    }

    public void showUnsavedWOData() {
        ArrayList<String> unsavedWOs = LocalStorageUtil.getUnsavedWOs(WOListActivity.this);
        arrayList.addAll(unsavedWOs);
        adapter = new WOListItemAdapter<String>(WOListActivity.this, R.layout.wolist_item, arrayList);
        woBinding.secondListView.setAdapter(adapter);

    }




}
