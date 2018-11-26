package com.journaldev.searchview;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.journaldev.searchview.databinding.ActivityMainBinding;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gson.PIHeaders;
import gson.PIItems;
import gson.PIWOList;
import model.StorageBin;
import model.WarehouseOrderCount;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import util.HttpUtil;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding activityMainBinding;
    PIDocAdapter adapter;
    PIDoc piDocAdapterEntry;
    AlertDialog alertDialog;
    ProgressDialog progressDialog;

    //handle wolist httprequest
    int docListSize = 0;
    final List<PIWOList> woList = new ArrayList<>();

    //handle piItems List httprequest
    final ArrayList<WarehouseOrderCount> woCountList = new ArrayList<>();

    ArrayList<PIDoc> arrayList= new ArrayList<>();
    ArrayList<PIHeaders> piHeaders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*** 我们可以先用LayoutInflater把布局xml文件引入成View对象，再通过setContentView(View view)方法来切换视图。
         * 因为所有对View的修改都保存在View对象里，所以，当切换回原来的view时，就可以直接显示原来修改后的样子**
         * activityMainBinding: <T extends ViewDataBinding> is returned
         */
        //activityMainBinding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Intent intent = getIntent();
        String piListUrl = intent.getStringExtra("PIlist_Url");
        getPIList(piListUrl, false);

        //set toolbar
        Toolbar toolbar = activityMainBinding.toolbarPilist;
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar!= null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.header_nav_back);
        }

//        piDocAdapterEntry = new PIDoc("6000000639", 1);
//        arrayList.add(piDocAdapterEntry);

        //activityMainBinding.search.setActivated(true);
        //activityMainBinding.search.onActionViewExpanded();
        //activityMainBinding.search.setIconified(false);
        //activityMainBinding.search.clearFocus();
        //hideKeyboard();

        initDialog();
        initProcessDialog();

/*        activityMainBinding.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                adapter.getFilter().filter(newText);

                return false;
            }
        });*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_pilist, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search_pi).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconified(false);
        searchView.setSubmitButtonEnabled(true);
        searchView.setQueryRefinementEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    private void getPIList(String piListUrl, final boolean refreshIndicator) {
        HttpUtil.sendOkHttpRequest(piListUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("GetPIList", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();

                try {
                    final String responseData = responseBody.string();
                    JSONObject Jobject = new JSONObject(responseData);
                    JSONObject d_results = Jobject.getJSONObject("d");
                    JSONArray results = d_results.getJSONArray("results");
                    String wo_content;
                    PIHeaders piDoc;
                    piHeaders = new ArrayList<PIHeaders>();
                    for(int i = 0; i< results.length(); i++) {
                        wo_content = results.getJSONObject(i).toString();
                        piDoc = new Gson().fromJson(wo_content, PIHeaders.class);
                        piHeaders.add(piDoc);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!refreshIndicator) {
                                showResponseData();
                            } else {
                                refreshListData();
                            }

                        }
                    });
                }catch(Exception err) {
                    Log.d("Response", err.getMessage());
                }finally {
                    responseBody.close();
                }
            }
        });
    }

    private void refreshListData() {
        if(piHeaders.size() > 0){
            arrayList = new ArrayList<PIDoc>();
            for(PIHeaders piHeader: piHeaders) {
                PIDoc piDoc = new PIDoc(piHeader.PhysicalInventoryDocumentNumber, 1);
                arrayList.add(piDoc);
            }
            if(adapter != null) {
                adapter.refreshPIList(arrayList);
                adapter.notifyDataSetChanged();
            } else {
                adapter= new PIDocAdapter(MainActivity.this, R.layout.pidoclist_item, arrayList);
                activityMainBinding.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Intent intent = new Intent(MainActivity.this, WOListActivity.class);
                        String url = HttpUtil.getWOListUrl(piHeaders.get(i).PhysicalInventoryDocumentNumber);
                        intent.putExtra("url", url);
                        intent.putExtra("handleLocal", "false");
                        startActivity(intent);
                    }
                });
                activityMainBinding.listView.setAdapter(adapter);
            }
        } else {
            onResponseEmpty();
        }
    }

    private void onResponseEmpty() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.setMessage("No qualified PI Documents found.");
        alertDialog.show();
    }

    private void showResponseData() {
        arrayList = new ArrayList<PIDoc>();
        if(piHeaders.size() > 0) {
            for(PIHeaders piHeader: piHeaders) {
                PIDoc piDoc = new PIDoc(piHeader.PhysicalInventoryDocumentNumber, 1);
                arrayList.add(piDoc);
            }

            adapter= new PIDocAdapter(MainActivity.this, R.layout.pidoclist_item, arrayList);
            activityMainBinding.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(MainActivity.this, WOListActivity.class);
                    String url = HttpUtil.getWOListUrl(piHeaders.get(i).PhysicalInventoryDocumentNumber);
                    intent.putExtra("url", url);
                    intent.putExtra("handleLocal", "false");
                    startActivity(intent);
                }
            });
            activityMainBinding.listView.setAdapter(adapter);

        } else {
            onResponseEmpty();
        }

    }

    private void filterList(String query) {
        Log.d("Query", query);
        ArrayList<PIDoc> results = new ArrayList<>();
        if(query.equals("")) {
            results = arrayList;
        } else {
            Pattern p = Pattern.compile(query);
            for(int i = 0; i < arrayList.size(); i++) {
                Matcher matcher = p.matcher(arrayList.get(i).getPIDocNumber());
                if(matcher.find()) {
                    results.add(arrayList.get(i));
                }
            }
        }
        adapter = new PIDocAdapter(MainActivity.this, R.layout.pidoclist_item, results);
        activityMainBinding.listView.setAdapter(adapter);
    }

    public void initDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MainActivity.this, "No Button is pressed", Toast.LENGTH_SHORT).show();
                setTitle(R.string.app_name);
            }
        });
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MainActivity.this, "Yes Button is pressed successfully", Toast.LENGTH_LONG).show();
            }
        });
        alertDialog = builder.create();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 1:
                //Toast.makeText(getApplication(), "ONActivityResult", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void initProcessDialog(){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Retrieving PI Items from backend, please wait...");
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(true);
    }
}
