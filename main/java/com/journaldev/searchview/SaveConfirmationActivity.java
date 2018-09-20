package com.journaldev.searchview;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class SaveConfirmationActivity extends Activity implements View.OnClickListener{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_confirm);

        Intent intent = getIntent();
        String woNumber = intent.getStringExtra("wo_number");
        TextView message = (TextView) findViewById(R.id.save_confirm_msg);
        message.setText("Current Warehouse Order " + woNumber +" has been counted.");
        Button saveLocalButton = (Button) findViewById(R.id.save_unsaved_wo);
        saveLocalButton.setOnClickListener(this);
        Button saveBackendButton = (Button) findViewById(R.id.save_backend);
        saveBackendButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.save_unsaved_wo:
                intent = new Intent();
                //把返回数据存入Intent
                intent.putExtra("save_local", "1");
                //设置返回数据
                SaveConfirmationActivity.this.setResult(RESULT_OK, intent);
                //关闭Activity
                SaveConfirmationActivity.this.finish();
                break;
            case R.id.save_backend:
                intent = new Intent();
                //把返回数据存入Intent
                intent.putExtra("save_local", "0");
                //设置返回数据
                SaveConfirmationActivity.this.setResult(RESULT_OK, intent);
                //关闭Activity
                SaveConfirmationActivity.this.finish();
                break;
        }
    }
}
