package com.journaldev.searchview;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.journaldev.searchview.databinding.ActivityMainBinding;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import gson.PIHeaders;
import gson.PIWOList;
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
        getPIList(piListUrl);

//        piDocAdapterEntry = new PIDoc("6000000639", 1);
//        arrayList.add(piDocAdapterEntry);

        //activityMainBinding.search.setActivated(true);
        activityMainBinding.search.setQueryHint("Search PI Document: ");
        activityMainBinding.search.onActionViewExpanded();
        activityMainBinding.search.setIconified(false);
        //activityMainBinding.search.clearFocus();

        initDialog();
        initProcessDialog();

        activityMainBinding.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                adapter.getFilter().filter(newText);

                return false;
            }
        });
    }

    private void getPIList(String piListUrl) {
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
                    for(int i = 0; i< results.length(); i++) {
                        wo_content = results.getJSONObject(i).toString();
                        piDoc = new Gson().fromJson(wo_content, PIHeaders.class);
                        piHeaders.add(piDoc);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showResponseData();
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

    private void showResponseData() {
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
                    startActivity(intent);
                }
            });
            activityMainBinding.listView.setAdapter(adapter);

        } else {
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

    public void initProcessDialog(){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Retrieving PI Items from backend, please wait...");
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 1:
                Toast.makeText(getApplication(), "ONActivityResult", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void showDialog(String arg) {
        if(alertDialog == null) {
            return;
        }
        alertDialog.setTitle("PI Doc: " + arg + " is clicked, do you want to count it?");
        alertDialog.show();
        setTitle(arg);
    }

    }
