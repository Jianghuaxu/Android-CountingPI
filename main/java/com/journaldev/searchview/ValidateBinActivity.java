package com.journaldev.searchview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ValidateBinActivity extends Activity implements View.OnClickListener{
    private String bin;
    private EditText newBin;
    private Button confirmButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.validate_storage_bin);

        Intent intent = getIntent();
        bin = intent.getStringExtra("bin");

        TextView currentBin = (TextView) findViewById(R.id.original_sb);
        currentBin.setText(bin);

        ImageView binScanner = (ImageView) findViewById(R.id.sb_scanner);
        binScanner.setOnClickListener(this);

        newBin = (EditText) findViewById(R.id.new_sb);
        newBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(!newBin.getText().toString().equals("") && (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_NULL)) {
                    validateSB();
                    return true;
                }
                return false;
            }
        });

        confirmButton = (Button) findViewById(R.id.confirm_sb);
        confirmButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirm_sb:
                validateSB();
                break;
            case R.id.sb_scanner:
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setOrientationLocked(true);
                integrator.initiateScan();
                break;

        }
    }

    private void validateSB() {
        hideKeyboard();
        if(!bin.equals(newBin.getText().toString())) {
            newBin.requestFocus();
            return;
        }
        Intent intent;
        //should back to the previous activity
        intent = new Intent();
        ValidateBinActivity.this.setResult(RESULT_OK, intent);
        //关闭Activity
        ValidateBinActivity.this.finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent;
        //should back to the previous activity
        intent = new Intent();
        //把返回数据存入Intent
        intent.putExtra("validation_cancelled", "1");
        //设置返回数据
        ValidateBinActivity.this.setResult(RESULT_CANCELED, intent);
        //关闭Activity
        ValidateBinActivity.this.finish();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm.isActive()) {
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
                newBin.setText(result.getContents());
                //validateSB();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
