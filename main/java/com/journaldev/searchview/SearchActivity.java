package com.journaldev.searchview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.allenliu.badgeview.BadgeFactory;
import com.allenliu.badgeview.BadgeView;
import com.google.gson.Gson;
import com.journaldev.searchview.databinding.ActivitySearchBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import gson.PIHeaders;
import gson.PIItems;
import gson.PIWOList;
import model.StorageBin;
import model.WOResponseModel;
import model.WarehouseOrderCount;
import modelHelper.WOCountHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import util.HttpUtil;
import util.LocalStorageUtil;

import static util.LocalStorageUtil.getUnsavedWOs;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    private ActivitySearchBinding activitySearchBinding;
    private DrawerLayout drawerLayout;

    //handle httpRequest
    ArrayList<PIHeaders> piHeaders = new ArrayList<>();
    int piDocListSize = 0;
    final ArrayList<PIWOList> woList = new ArrayList<>();
    final ArrayList<String> woListResponseData = new ArrayList<>();
    int woListSize = 0;
    ProgressDialog progressDialog;

    //handle unsaved tasks
    int unsavedWOSize;
    boolean isGuidedMode = false;

    //handle menu
    private Menu menu;

    //handle piItems List httpRequest
    final ArrayList<WarehouseOrderCount> woCountList = new ArrayList<>();
    final ArrayList<WOResponseModel> woCountListResponseData = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySearchBinding = DataBindingUtil.setContentView(this, R.layout.activity_search);
        activitySearchBinding.searchPi.setOnClickListener(this);

        //set toolbar
        Toolbar toolbar = activitySearchBinding.toolbar;
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!= null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.menu_btn);
        }

        //add listener to switch(select if guidedMode)
        Switch guidedModeSwitch = (Switch) activitySearchBinding.getRoot().findViewById(R.id.guided_mode);
        guidedModeSwitch.setOnCheckedChangeListener(this);

        //initialize drawerlayout
        drawerLayout = activitySearchBinding.drawerLayout;

        //read from local file
        checkUnsavedWO();

        activitySearchBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                if(unsavedWOSize > 0 && item.getItemId() == R.id.unsavedTasks) {
                    Intent intent = new Intent(SearchActivity.this, WOListActivity.class);
                    intent.putExtra("handleLocal", "true");
                    startActivityForResult(intent, 1);
                }
                drawerLayout.closeDrawers();

                return true;
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()) {
            case R.id.guided_mode:
                isGuidedMode = isChecked;
        }
    }

    private void checkUnsavedWO() {
        ArrayList<String> unsavedWOList = LocalStorageUtil.getUnsavedWOs(SearchActivity.this);
        if(unsavedWOList != null) {
            unsavedWOSize = unsavedWOList.size();
            Menu menu = activitySearchBinding.navView.getMenu();
            MenuItem unsavedTasksMenuItem = menu.findItem(R.id.unsavedTasks);
            unsavedTasksMenuItem.setTitle("Unsaved Tasks (" + unsavedWOSize + ")");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if(resultCode == 1) {
                    //all save tasks are done.
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_pi:
                attemptSearchPIList();
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                BadgeFactory.create(this)
                        .setWidthAndHeight(1,1)
                        .setBadgeBackground(Color.RED)
                        .setShape(BadgeView.SHAPE_CIRCLE)
                        .bind(findViewById(item.getItemId()));
                drawerLayout.openDrawer(activitySearchBinding.navView);
                checkUnsavedWO();
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        return true;
    }

    private void attemptSearchPIList() {
        //handle warehouse number input, which is mandatory
        String whn = activitySearchBinding.warehouseNumber.getText().toString();
        if(whn.equals("")) {
            activitySearchBinding.warehouseNumber.requestFocus();
            return;
        }
        hideKeyboard();
        HttpUtil.setWareHouseNumber(whn);
        String woNumber = activitySearchBinding.warehouseOrder.getText().toString();
        String storageType = activitySearchBinding.storageType.getText().toString();
        String aisle = activitySearchBinding.aisle.getText().toString();
        String piListUrl = HttpUtil.getPIListUrl(woNumber, storageType, aisle);

        WOCountHelper.setIsGuidedMode(isGuidedMode);

        if(isGuidedMode) {
            getPIList(piListUrl);
        } else {
            countInNormalMode(piListUrl);
        }

    }

    private void countInNormalMode(String piListUrl) {
        Intent intent = new Intent(SearchActivity.this, MainActivity.class);
        intent.putExtra("PIlist_Url", piListUrl);
        intent.putExtra("handleLocal", "false");
        startActivity(intent);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm.isActive()) {
            if (this.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    private void getPIList(String piListUrl) {
        showProcessDialog(true);
        HttpUtil.sendOkHttpRequest(piListUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showProcessDialog(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                try {
                    if(response.isSuccessful()) {
                        final String responseData = responseBody.string();
                        JSONObject Jobject = new JSONObject(responseData);
                        JSONObject d_results = Jobject.getJSONObject("d");
                        JSONArray results = d_results.getJSONArray("results");
                        String wo_content;
                        PIHeaders piDoc;
                        for(int i = 0; i< results.length(); i++) {
                            wo_content = results.getJSONObject(i).toString();
                            piDoc = new Gson().fromJson(wo_content, PIHeaders.class);
                            Log.d("NumberOfPIDocReceived", String.valueOf(piHeaders.size()));
                            piHeaders.add(piDoc);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onDocListReceived();
                            }
                        });
                    } else {
                        showProcessDialog(false);
                        HttpUtil.afterRequestFailed(SearchActivity.this);

                    }

                }catch(Exception err) {
                    showProcessDialog(false);
                    Log.d("Response", err.getMessage());
                }finally {
                    if(piHeaders.size()> 0) {
                        responseBody.close();
                    }
                }
            }
        });
    }

    private void onDocListReceived() {
        Log.d("PIDoc", String.valueOf(piHeaders.size()));
        if(piHeaders.size() > 0) {
            piDocListSize = 0;
            for(PIHeaders piHeader: piHeaders) {
                Log.d(piHeader.PhysicalInventoryDocumentNumber, "request send");
                getWOList(piHeader.PhysicalInventoryDocumentNumber);
            }
        } else {
            showProcessDialog(false);
            //case no qualified PI documents found
            Toast.makeText(SearchActivity.this, "No qualified PI documents found, please refine your search criteria", Toast.LENGTH_SHORT).show();
        }
    }

    private void getWOList(String piDocNumber) {
        final String pi_doc_number = piDocNumber;
        HttpUtil.sendOkHttpRequest(HttpUtil.getWOListUrl(piDocNumber) + "&$format=json", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleWOListResponse(true, null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseData = response.body().string();
                    handleWOListResponse(false, responseData);
                } else {
                    handleWOListResponse(true, null);
                }
                response.body().close();
            }
        });
    }

    private void handleWOListResponse(boolean error, String responseData) {
        piDocListSize++;
        if(!error) {
            woListResponseData.add(responseData);
        }
        if(piDocListSize == piHeaders.size()) {
            runFinalGetWOListRequest();
        }
    }

    private void runFinalGetWOListRequest() {
        try{
            for(String responseData: woListResponseData) {
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
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        String piItemURL;
        for(PIWOList wo: woList) {
            //get all the pi items in current wo
            piItemURL = HttpUtil.getPIItemURL(String.valueOf(wo.PhysicalInventoryDocumentGUID), wo.WarehouseOrder);
            getPIItemsWithOKHttp(piItemURL, wo.WarehouseOrder, wo.CountDate);
        }
    }

    private void getPIItemsWithOKHttp (String url, String woNumber, String countDate) {
        final String wo_number = woNumber;
        final String wo_count_date = countDate;
        // get all PI Items(Storage Bins) in one WO, results could be saved in class WarehouseOrderCount
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handlePIItemsListResponse(true, null, null, null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseData = response.body().string();
                    handlePIItemsListResponse(false, responseData, wo_number, wo_count_date);
                } else {
                    handlePIItemsListResponse(true, null, null, null);
                }
                response.body().close();
            }
        });
    }

    private void handlePIItemsListResponse(boolean error, String responseData, String woNumber , String countDate) {
        //get all warehouse Orders with their storage bins (containing all pi Items for counting)
        woListSize++;
        if(!error) {
            WOResponseModel woResponseData = new WOResponseModel();
            woResponseData.warehouseNumber = woNumber;
            woResponseData.countDate = countDate;
            woResponseData.responseData = responseData;
            woCountListResponseData.add(woResponseData);
        }
        if(woListSize == woList.size()) {
            runFinalGetPIItemsListRequest();
        }
    }

    private void runFinalGetPIItemsListRequest() {
        try{
            for(WOResponseModel woResponse: woCountListResponseData) {
                final ArrayList<PIItems> piItemList = new ArrayList<>();
                JSONObject Jobject = new JSONObject(woResponse.responseData);
                JSONObject d_results = Jobject.getJSONObject("d");
                JSONArray results = d_results.getJSONArray("results");
                String wo_content;
                PIItems piItem;
                for(int i = 0; i< results.length(); i++) {
                    wo_content = results.getJSONObject(i).toString();
                    piItem = new Gson().fromJson(wo_content, PIItems.class);
                    piItem.ProductQuantity = "";
                    piItemList.add(piItem);
                }
                ArrayList<StorageBin> storageBins = convertPIItemsToStorageBins(piItemList);
                WarehouseOrderCount woC = new WarehouseOrderCount();
                woC.WarehouseOrderNumber = woResponse.warehouseNumber;
                woC.CountDate = woResponse.countDate;
                woC.binArrayList = storageBins;
                woCountList.add(woC);
            }

        }catch(Exception e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showProcessDialog(false);
                WOCountHelper.setWarehouseOrderForCounting(woCountList);
                Intent intent= new Intent(SearchActivity.this, CountActivity.class);
                intent.putExtra("isGuidedMode", "true");
                intent.putExtra("handleLocal", "false");
                startActivity(intent);
            }
        });
    }

    private ArrayList<StorageBin> convertPIItemsToStorageBins(ArrayList<PIItems> piItems) {
        ArrayList<StorageBin> storageBins = new ArrayList<>();
        ArrayList<PIItems> itemsTmp = new ArrayList<>();
        ArrayList<String> binNumbers = new ArrayList<>();
        String binNumber = null;
        for(PIItems item : piItems) {
            binNumber = item.StorageBin;
            if(!binNumbers.contains(binNumber)){
                if(itemsTmp.size() > 0){
                    ArrayList<PIItems> itemsForNewBin = new ArrayList<>();
                    itemsForNewBin.addAll(itemsTmp);
                    StorageBin binNew = new StorageBin();
                    binNew.binEmpty = itemsForNewBin.get(0).StorageBinEmpty;
                    binNew.storageBin = binNumbers.get(binNumbers.size() - 1);
                    binNew.piItemsInBin = itemsForNewBin;
                    storageBins.add(binNew);
                    itemsTmp.clear();
                }
                binNumbers.add(binNumber);
            }
            itemsTmp.add(item);
        }
        if(itemsTmp.size() > 0 && binNumber != null) {
            StorageBin binLast = new StorageBin();
            //the last storage bin should also be added into list storageBins
            binLast.storageBin = binNumber;
            binLast.piItemsInBin = itemsTmp;
            storageBins.add(binLast);
        }
        return storageBins;
    }

    private void showProcessDialog(Boolean showFlag) {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Retrieving data from backend, please wait...");
            progressDialog.setCancelable(true);
        }
        if(showFlag) {
            progressDialog.show();
        } else {
            if(progressDialog.isShowing()) {
                progressDialog.hide();
            }
        }
    }

}
