package com.journaldev.searchview;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import util.HttpUtil;

public class LogonActivity extends AppCompatActivity {
    //UI References
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    //handle http request
    String username, password;
    ProgressDialog processDialog;

    //handle server selection
    String serverName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_logon);
        setClient(this);

        Spinner serverList = (Spinner) findViewById(R.id.server_list);
        ArrayList<String> servers = new ArrayList<>();
        servers.add("WMT210-QKX101");
        servers.add("UYR210-QM7710");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.server_list_item, servers);
        serverList.setAdapter(adapter);
        serverList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view;
                serverName = textView.getText().toString();
                setServerHostName(serverName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_prgress);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(serverName != null && (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_NULL)) {
                    attempLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLogonButton = (Button) findViewById(R.id.email_sign_in_button);
        mLogonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attempLogin();
            }
        });

        Button registerButton = (Button) findViewById(R.id.register_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://dlm/cockpit/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

    }

    private void setClient(Context context) {
        Client clientObj = new Client(context);
        HttpUtil.setClient(clientObj.getClient());
    }

    public void setServerHostName(String serverName) {
        switch (serverName) {
            case "WMT210-QKX101":
                this.serverName = "https://wmt210-qkx101.wdf.sap.corp/sap/opu/odata/SCWM/RECORD_INVENTORY_SRV/";
                break;
            case "UYR210-QM7710":
                this.serverName = "https://ldciuyr.wdf.sap.corp:44300/sap/opu/odata/SCWM/RECORD_INVENTORY_SRV/";
                break;
        }
        HttpUtil.setHostName(this.serverName);
    }

    public void attempLogin(){
        //reset errors
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        username = mUsernameView.getText().toString();
        password = mPasswordView.getText().toString();

        HttpUtil.setCredential(username, password);


        boolean cancel = false;
        View focusView = null;

        //check if valid password
        if(TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            if(TextUtils.isEmpty(password)) {
                focusView = mPasswordView;
            } else {
                focusView = mUsernameView;
            }
            cancel = true;
        }

        if(cancel) {
            focusView.requestFocus();
        } else {
            showProcessDialog(true);
            HttpUtil.sendOkHttpRequestLogon(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("logon", e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUsernameView.requestFocus();
                            showProcessDialog(false);
                        }
                    });

                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    ResponseBody respBody = response.body();
                    try{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                HttpUtil.setToken(response.header("x-csrf-token"));
                                showProcessDialog(false);
                                onLogonSuccess();
                            }
                        });
                    } finally {
                        respBody.close();
                    }

                }
            });

        }
    }

    private void showProcessDialog(Boolean showFlag) {
        if(processDialog == null) {
            processDialog = new ProgressDialog(this);
        }
        if(showFlag) {
            processDialog.show();
        } else {
            if(processDialog.isShowing()) {
                processDialog.hide();
            }
        }

    }

    private void onLogonSuccess() {
        Intent intent = new Intent(LogonActivity.this, SearchActivity.class);
        intent.putExtra("User_name",username);
        intent.putExtra("User_password", password);
        startActivityForResult(intent, 1);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 1:
                //do something here
                Toast.makeText(LogonActivity.this, "Back Button pressed", Toast.LENGTH_LONG).show();
                break;
                default:
                    //
        }
    }

    public boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

}
