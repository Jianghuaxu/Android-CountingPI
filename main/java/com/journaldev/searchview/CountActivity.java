package com.journaldev.searchview;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.gson.Gson;
import com.journaldev.searchview.databinding.ActivityCountBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import gson.PIItems;
import model.PIItemsUrlLoad;
import model.StorageBin;
import model.WarehouseOrderCount;
import modelHelper.StorageBinHelper;
import modelHelper.WOCountHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import util.HttpUtil;
import util.LocalStorageUtil;
import util.Util;

import static modelHelper.StorageBinHelper.getBinArrayList;

public class CountActivity extends AppCompatActivity implements View.OnClickListener, CountItemAdapter.Callback{
    ActivityCountBinding countBinding;
    CountItemAdapter adapter;
    String woNumber;
    String piDocUuid;
    String piItemURL;

    //handle source activity
    Intent receivedIntent;

    //handle all piItems in current Warehouse Order
    ArrayList<PIItems> piItems = new ArrayList<>();

    //handle next button
    AlertDialog alertDialog;
    AlertDialog.Builder alertDialogBuilder;

    //handle current storage Bin
    StorageBin currentStorageBin;

    //handle save
    ProgressDialog progressDialog;
    String countDate;
    String countUser;
    int responseCount = 0;
    int numberOfPutRequest = 0;
    boolean hasError = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        countBinding = DataBindingUtil.setContentView(CountActivity.this, R.layout.activity_count);

        //set toolbar
        Toolbar toolbar = (Toolbar)countBinding.getRoot().findViewById(R.id.toolbar_count);
        setSupportActionBar(toolbar);

        receivedIntent = getIntent();
        /***
         * three source activity exist:
         * 1, normal mode: handle local unsaved WO
         * 2, normal mode: need to get PIItems for specific WONumber and PI Doc Guid
         * 3, Guided_mode: all data are received in WOCountHelper.
         */
        if(receivedIntent.getStringExtra("handleLocal").equals("true")) {
            //scenario 1
            String unsavedWOData = LocalStorageUtil.getUnsavedData(CountActivity.this, receivedIntent.getStringExtra("WO_Number"));
            WarehouseOrderCount warehouseOrderCount = new Gson().fromJson(unsavedWOData, WarehouseOrderCount.class);
            StorageBinHelper.setBinArrayList(warehouseOrderCount.binArrayList);
            woNumber = warehouseOrderCount.WarehouseOrderNumber;
            countDate = warehouseOrderCount.CountDate;
            showDefaultView();
            countBinding.binEmpty.setChecked(currentStorageBin.binEmpty);
        }else {
            if(receivedIntent.getStringExtra("isGuidedMode").equals("true")) {
                //scenario 3
                WarehouseOrderCount defaultWarehouseOrder = WOCountHelper.getNextWarehouseOrder();
                StorageBinHelper.setBinArrayList(defaultWarehouseOrder.binArrayList);
                woNumber = defaultWarehouseOrder.WarehouseOrderNumber;
                countDate = defaultWarehouseOrder.CountDate;
                showDefaultView();
                Log.d("CountMode", "Guided");
            } else {
                //Scenario 2
                woNumber = receivedIntent.getStringExtra("WO_Number");
                piDocUuid = receivedIntent.getStringExtra("PI_DOC_UUID");
                countUser = HttpUtil.getUserName();
                countDate = receivedIntent.getStringExtra("Count_date");
                piItemURL = HttpUtil.getPIItemURL(piDocUuid, woNumber);
                getPIItemsWithOKHttp(piItemURL);
            }
        }

        //add listeners
        countBinding.nextBin.setOnClickListener(this);
        countBinding.previousBin.setOnClickListener(this);
        countBinding.binEmpty.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentStorageBin.binEmpty = isChecked;
            }
        });

        initDialog();
        countBinding.headerWo.setText(woNumber);

    }

    /*@Override
    public void onRadioButtonClicked(View v) {
        int itemPosition = (int) v.getTag();
        currentStorageBin = StorageBinHelper.onHUStatusChanged(currentStorageBin, piItems.get(itemPosition), v);
    }*/

    @Override
    public void onQuantityChanged(int position, String s) {
        currentStorageBin = StorageBinHelper.onQuantityChange(currentStorageBin, position, s);
    }

    @Override
    public void onAddQuantity(View v) {
        currentStorageBin = StorageBinHelper.onAddQuantity(currentStorageBin, v);
        View view = countBinding.binList.getChildAt((int)v.getTag());
        CountItemAdapter.ViewHolder viewHolder = (CountItemAdapter.ViewHolder) view.getTag();
        viewHolder.QuantityView.setText(currentStorageBin.piItemsInBin.get((int) v.getTag()).ProductQuantity);
    }

    @Override
    public void onReduceQuantity(View v){
        View view = countBinding.binList.getChildAt((int)v.getTag());
        CountItemAdapter.ViewHolder viewHolder = (CountItemAdapter.ViewHolder) view.getTag();
        try{
            currentStorageBin = StorageBinHelper.onReduceQuantity(currentStorageBin, v);
            viewHolder.QuantityView.setText(currentStorageBin.piItemsInBin.get((int) v.getTag()).ProductQuantity);
        } catch (Exception e) {
            alertDialog.setMessage(e.getMessage());
            alertDialog.show();
        }

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.next_bin:
                /***
                 * check Icon is save, if yes, then save, else go to next not-counted Item
                 */
                goToNextBin();
                break;
            case R.id.previous_bin:
                //TODO: go back to previous bin
                break;
        }
    }

    public void goToNextBin() {
        if(!checkCurrentStorageBinComplete()) {
            alertDialog.setTitle("current storage bin is not complete");
            alertDialog.show();
            return;
        }
        if(!currentStorageBin.binCounted) {
            StorageBinHelper.updateProgress();
            currentStorageBin.binCounted = true;
        }
        currentStorageBin = StorageBinHelper.getNextStorageBin(currentStorageBin);
        if(currentStorageBin == null) {
            countBinding.bar.setProgress(countBinding.bar.getMax());
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(CountActivity.this);
            dialogBuilder.setMessage("Warehouse Order: " + woNumber + " is counted, where do you want to save?");
            dialogBuilder.setPositiveButton("Backend", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handleSaveBinItems();
                }
            });
            dialogBuilder.setNegativeButton("Local", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handleSaveToLocal();
                }
            });
            dialogBuilder.create().show();
            return;
        }
        adapter.clear();
        adapter.refreshStorageBin(currentStorageBin.piItemsInBin);
        refreshHeaderInfo();
        countBinding.previousBin.setVisibility(View.VISIBLE);
    }

    private void refreshHeaderInfo() {
        countBinding.headerBin.setText(currentStorageBin.storageBin);
        if(currentStorageBin.piItemsInBin.get(0).StorageBinEmpty) {
            currentStorageBin.binEmpty = true;
            countBinding.binEmpty.setChecked(true);
        } else {
            countBinding.binEmpty.setChecked(false);
        }
        //handle the horizontal progress bar
        int newProgress = StorageBinHelper.getProgress(countBinding.bar.getMax());
        countBinding.bar.setProgress(newProgress);
        int itemPosition = StorageBinHelper.getItemPosition(currentStorageBin);
        countBinding.itemPosition.setText(itemPosition + "/" + StorageBinHelper.getNumberOfItems());
    }

    public void initDialog(){
        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        });
        alertDialog =  alertDialogBuilder.create();
    }

    public void getPIItemsWithOKHttp(String url) {

        HttpUtil.sendOkHttpRequest(url, new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                try {
                    if(response.isSuccessful()) {

                    }
                    Log.d("Wolist", response.toString());
                    final String responseData = response.body().string();
                    JSONObject Jobject = new JSONObject(responseData);
                    JSONObject d_results = Jobject.getJSONObject("d");
                    JSONArray results = d_results.getJSONArray("results");
                    String piItem_content;
                    PIItems piItem;
                    for(int i = 0; i< results.length(); i++) {
                        piItem_content = results.getJSONObject(i).toString();
                        piItem = new Gson().fromJson(piItem_content, PIItems.class);
                        piItem.ProductQuantity = "";
                        piItems.add(piItem);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleResponseData();
                        }
                    });
                }catch(Exception err) {
                    Log.d("Wolist", err.getMessage());
                    err.printStackTrace();
                }finally {
                    responseBody.close();
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("Wolist", e.getMessage());
                e.printStackTrace();
            }
        });

    }

    public void handleResponseData() {
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
        //get all PIItems separated by their storage Bin number, however no sorting by WO number is done here.
        StorageBinHelper.setBinArrayList(storageBins);
        showDefaultView();

    }

    public void showDefaultView () {
        /***
         * 1. get first open storage bin
         */
        StorageBin defaultStorageBin = StorageBinHelper.getDefaultStorageBin();
        currentStorageBin = defaultStorageBin;
        StorageBinHelper.resetCountedStorageBins();
        refreshHeaderInfo();
        countBinding.previousBin.setVisibility(View.INVISIBLE);
        adapter = new CountItemAdapter(CountActivity.this, R.layout.count_item, defaultStorageBin.piItemsInBin, this);
        countBinding.binList.setAdapter(adapter);
        countBinding.binList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("binList", String.valueOf(position));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_count, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_quantity:
                //handleSaveBinItems();
                break;
        }
        return true;
    }

    private void handleSaveBinItems() {

        /***
         * following check should be done:
         * 1, current storage bin is complete and relevant data is updated, if not => pop-up indicate finish current counting firstly
         * 2, all storage bins are counted = > if not pop-up that warehouse order is incomplete
         */
        ArrayList<PIItemsUrlLoad> piItemsUrlLoadArrayList = StorageBinHelper.preparePutRequestLoad(countDate);
        numberOfPutRequest = piItemsUrlLoadArrayList.size();

        responseCount = 0;
        for(PIItemsUrlLoad piItemsUrlLoad: piItemsUrlLoadArrayList) {
            Log.d("Url", piItemsUrlLoad.url);
            sendPostRequest(piItemsUrlLoad.url, piItemsUrlLoad.requestBody);
        }
    }

    private void handleSaveToLocal() {
        ArrayList<StorageBin> binArrayListSaveLocal = StorageBinHelper.getBinArrayList();
        WarehouseOrderCount warehouseOrderCount = new WarehouseOrderCount();
        warehouseOrderCount.WarehouseOrderNumber = woNumber;
        warehouseOrderCount.CountDate = countDate;
        warehouseOrderCount.binArrayList = binArrayListSaveLocal;
        LocalStorageUtil.saveWOToFile(CountActivity.this, woNumber, new Gson().toJson(warehouseOrderCount, WarehouseOrderCount.class));
        //get the next storage bin, just like the final step while saving to backend.
        Toast.makeText(CountActivity.this, "Save to local done", Toast.LENGTH_LONG).show();
        WarehouseOrderCount nextWarehouseOrderCount = null;
        if(WOCountHelper.getIsGuidedMode()) {
            //only in this mode, there would be multiple WOs
            nextWarehouseOrderCount = WOCountHelper.getNextWarehouseOrder();
        }

        if(nextWarehouseOrderCount != null) {
            StorageBinHelper.setBinArrayList(nextWarehouseOrderCount.binArrayList);
            //continue to count next warehouse order!
            showDefaultView();
            return;
        }
        //all tasks are finished!
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(CountActivity.this);
        alertDialogBuilder.setMessage("All WO are saved successfully, do you want to continue? ");
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(CountActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                System.exit(0);
            }
        });
        alertDialogBuilder.create().show();
        alertDialog.setMessage("All tasks finished, you wanna quit? ");
        return;
    }

    private Boolean checkCurrentStorageBinComplete () {
        /***
         * case 1: bin empty, then only first entry is needed to send to backend.
         * case 2: each item has either hu status selected or quantity is not empty
         */
        Boolean isBinIncomplete = false;
        if(currentStorageBin.binEmpty) {
            return true;
        }
        //loop all items for current storageBin
        for(PIItems item : currentStorageBin.piItemsInBin) {
            if(item.HandlingUnitEmpty || item.HandlingUnitMissing || !item.ProductQuantity.equals("")) {
                continue;
            } else {
                isBinIncomplete = true;
                break;
            }
        }
        if(isBinIncomplete) {
            return false;
        } else {
            return true;
        }
    }


    private void sendPostRequest(String postUri, String postLoad) {

        Log.d("postLoad", postLoad);
        HttpUtil.saveOkHttpPostRequest(postUri, postLoad, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("Failure", e.getMessage());
                handlePostResponseData(true);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("Status_code", String.valueOf(response.code()));
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
                    Toast.makeText(CountActivity.this, "Save failure", Toast.LENGTH_LONG).show();
                }
            });
        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LocalStorageUtil.deleteWO(CountActivity.this, woNumber);
                    WarehouseOrderCount warehouseOrderCount = null;
                    Toast.makeText(CountActivity.this, "Save successfully", Toast.LENGTH_LONG).show();
                    //TODO: in Guided_Mode, check if other WO exists, if yes then set arraylist in StorageBinHelper class
                    if(WOCountHelper.getIsGuidedMode()) {
                        warehouseOrderCount = WOCountHelper.getNextWarehouseOrder();
                    }

                    if(warehouseOrderCount != null) {
                        StorageBinHelper.setBinArrayList(warehouseOrderCount.binArrayList);
                        showDefaultView();
                        return;
                    }
                    //all tasks are finished!
                    android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(CountActivity.this);
                    alertDialogBuilder.setMessage("All WO are saved successfully, do you want to continue? ");
                    alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(CountActivity.this, SearchActivity.class);
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
                    alertDialog.setMessage("All tasks finished, you wanna quit? ");
                    return;
                }
            });
        }
    }

}
