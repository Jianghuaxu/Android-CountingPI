package com.journaldev.searchview;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
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
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.allenliu.badgeview.BadgeFactory;
import com.allenliu.badgeview.BadgeView;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journaldev.searchview.databinding.ActivitySearchBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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

import static com.journaldev.searchview.LogonActivity.MY_PREFERENCES;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    private ActivitySearchBinding activitySearchBinding;
    private DrawerLayout drawerLayout;

    //handle save warehouse number
    private final String WAREHOUSE_NUMBER = "WAREHOUSE_NUMBER";
    private final String STORAGE_TYPE = "STORAGE_TYPE";

    //handle httpRequest
    ArrayList<PIHeaders> piHeaders = new ArrayList<>();
    int piDocListSize = 0;
    final ArrayList<PIWOList> woList = new ArrayList<>();
    final ArrayList<String> woListResponseData = new ArrayList<>();
    int woListSize = 0;
    ProgressDialog progressDialog;

    //handle navigation view image change
    private static int RESULT_LOAD_IMAGE = 10;
    private ImageView navHeaderImage;
    private static final int WRITE_PERMISSION = 0x01;
    //save select image, used as profile later
    private static final String SAVE_PIC_PATH = Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)
            ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/mnt/sdcard/chenxh/mytestApp";//判断sd卡
    private static final String SAVE_REAL_PATH = SAVE_PIC_PATH+  "/res/user_profile";
    private static final String PROFILE_PIC_FILE_NAME = "profile.jpeg";
    private static final String PROFILE_SUBFOLDER = "/userProfile";

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

        activitySearchBinding.warehouseOrderBarcodeScanner.setOnClickListener(this);

        //add listener to switch(select if guidedMode)
        Switch guidedModeSwitch = activitySearchBinding.guidedMode;

        guidedModeSwitch.setOnCheckedChangeListener(this);
        //initialize drawerlayout
        drawerLayout = activitySearchBinding.drawerLayout;

        //read from local file
        checkUnsavedWO();
        SharedPreferences sp = this.getSharedPreferences(MY_PREFERENCES, 0);
        String warehouse_number = sp.getString(WAREHOUSE_NUMBER, "");
        String storage_type = sp.getString(STORAGE_TYPE, "");
        if(!warehouse_number.equals("")) {
            activitySearchBinding.warehouseNumber.setText(warehouse_number);
            activitySearchBinding.warehouseOrder.requestFocus();
        }
        if(!storage_type.equals("")) {
            activitySearchBinding.storageType.setText(storage_type);
            activitySearchBinding.aisle.requestFocus();
        }
        TextView navHeaderName = (TextView) activitySearchBinding.navView.getHeaderView(0).findViewById(R.id.nav_header_name);
        //navHeaderName.setText(HttpUtil.getUserName().toUpperCase());
        navHeaderImage = (ImageView) activitySearchBinding.navView.getHeaderView(0).findViewById(R.id.nav_header_img);

        navHeaderImage.setOnClickListener(this);
        updateProfilePic();
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

    private void updateProfilePic() {
        //saveImage(profilePic,PROFILE_PIC_FILE_NAME,PROFILE_SUBFOLDER);
        String subForder = SAVE_REAL_PATH + PROFILE_SUBFOLDER;
        File profilePic = new File(subForder, PROFILE_PIC_FILE_NAME);
        if (!profilePic.exists()) {
            return;
        }
        try{
            BufferedInputStream bos = new BufferedInputStream(new FileInputStream(profilePic));
            Bitmap bm2 = BitmapFactory.decodeStream(bos);
            navHeaderImage.setImageBitmap(bm2);
            bos.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
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
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                //Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                activitySearchBinding.warehouseOrder.setText(result.getContents());
            }
        } else {
            //handle other activities
            if(requestCode == 1 && resultCode == 1) {
                //all save tasks are done;
                Toast.makeText(this, "All saved tasks are finished", Toast.LENGTH_LONG).show();
                return;
            }
            if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data){
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                //查询我们需要的数据
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap profilePic = BitmapFactory.decodeFile(picturePath);
                navHeaderImage.setImageBitmap(profilePic);

                try {
                    //createFolder();
                    saveImage(profilePic,PROFILE_PIC_FILE_NAME,PROFILE_SUBFOLDER);
                } catch (IOException e) {

                }
            }

        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_pi:
                //save searchCriteria
                SharedPreferences sp = this.getSharedPreferences(MY_PREFERENCES, 0);
                //使用Editor接口修改SharedPreferences中的值并提交。
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(WAREHOUSE_NUMBER, activitySearchBinding.warehouseNumber.getText().toString());
                editor.putString(STORAGE_TYPE, activitySearchBinding.storageType.getText().toString());
                editor.apply();
                attemptSearchPIList();
                break;
            case R.id.warehouse_order_barcode_scanner:
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setOrientationLocked(true);
                integrator.initiateScan();
                break;
            case R.id.nav_header_img:
                requestWritePermission();
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == WRITE_PERMISSION){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //这里要传一个整形的常量RESULT_LOAD_IMAGE到startActivityForResult()方法。
                startActivityForResult(intent, RESULT_LOAD_IMAGE);
                Toast.makeText(this, "You must allow permission write external storage to your mobile device.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestWritePermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_PERMISSION);
        } else {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //这里要传一个整形的常量RESULT_LOAD_IMAGE到startActivityForResult()方法。
            startActivityForResult(intent, RESULT_LOAD_IMAGE);
        }
    }

    //保存图片到本地路径
    public static void saveImage(Bitmap bm, String fileName, String path) throws IOException {
        String subForder = SAVE_REAL_PATH + path;
        File foder = new File(subForder);
        if (!foder.exists()) {
            foder.mkdirs();
        }
        File myCaptureFile = new File(subForder, fileName);
        if (!myCaptureFile.exists()) {
            myCaptureFile.createNewFile();
        }
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
        bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        bos.flush();
        bos.close();
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
        String woNumber = activitySearchBinding.warehouseOrder.getText().toString().toUpperCase();
        String storageType = activitySearchBinding.storageType.getText().toString().toUpperCase();
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
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
