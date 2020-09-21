package com.dataexpo.zmt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.dataexpo.zmt.common.HttpCallback;
import com.dataexpo.zmt.common.HttpService;
import com.dataexpo.zmt.common.URLs;
import com.dataexpo.zmt.common.Utils;
import com.dataexpo.zmt.pojo.MsgBean;
import com.google.gson.Gson;
import com.idata.fastscandemo.R;

import java.util.HashMap;

import okhttp3.Call;

public class UpLoadCheckExpoid extends BascActivity implements View.OnClickListener {
    private final String TAG = UpLoadCheckExpoid.class.getSimpleName();
    private Context mContext;
    private String expoId = "";
    private String address = "";
    private EditText et_expoId;
    private EditText et_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_check_expoid);
        mContext = this;
        initView();
    }

    private void initView() {
        findViewById(R.id.btn_upload_check_expo_back).setOnClickListener(this);
        et_expoId = findViewById(R.id.et_upload_check_expo_id);
        et_address = findViewById(R.id.et_upload_check_expo_address);
        findViewById(R.id.tv_upload_check_expo_confirm).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_upload_check_expo_back:
                finish();
                break;
            case R.id.tv_upload_check_expo_confirm:
                check();
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        if (!et_expoId.hasFocus()) {
            et_expoId.requestFocus();
        }
        super.onResume();
    }

    private void check() {
        if (!checkInput()) {
            return;
        }
        String url = URLs.VerifyExpo;
        final HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("expoId", expoId);
        hashMap.put("address", address);
        Log.i(TAG, "Expo_id: " + expoId + " Add: " + address);

        HttpService.getWithParams(mContext, url, hashMap, new HttpCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "网络异常，请确认网络连接", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onResponse(String response, int id) {
                final MsgBean msgBean = new Gson().fromJson(response, MsgBean.class);
                Log.i(TAG, "online check expoid response: " + response);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (msgBean.code == 200) {
                            Intent intent = new Intent();

                            intent.putExtra("Expo_id", expoId);
                            intent.putExtra("Add", address);
                            intent.setClass(mContext, UploadActivity.class);

                            startActivity(intent);
                            finish();

                        } else {
                            et_address.setText("");
                            et_expoId.setText("");
                            address = "";
                            expoId = "";
                            Toast.makeText(mContext, "展会ID不存在，请输入正确的展会ID", Toast.LENGTH_SHORT).show();
                            et_expoId.requestFocus();
                        }
                    }
                });
            }
        });
    }

    private boolean checkInput() {
        expoId = et_expoId.getText().toString().replaceAll("\n", "").trim();
        address = et_address.getText().toString().replaceAll("\n", "").trim();

        if (Utils.checkInput(expoId) != Utils.INPUT_SUCCESS) {
        //if (TextUtils.isEmpty(expoId)) {
            Toast.makeText(mContext, "请输入正确的展会ID", Toast.LENGTH_SHORT).show();
            et_expoId.setText("");
            expoId = "";
            et_expoId.requestFocus();
            return false;
        }
        if (Utils.checkInput(address) != Utils.INPUT_SUCCESS) {
            et_address.setText("");
            address = "";
            et_address.requestFocus();
            Toast.makeText(mContext, "门禁地点输入有误", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i(TAG, " event:" + event.toString());

        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!TextUtils.isEmpty(expoId) && !TextUtils.isEmpty(address)) {
                check();
            } else if (et_expoId.hasFocus()) {
                expoId = et_expoId.getText().toString().replaceAll("\n", "").trim();

                if (Utils.checkInput(expoId) != Utils.INPUT_SUCCESS) {
                    Toast.makeText(mContext, "展会ID输入有误", Toast.LENGTH_LONG).show();
                    expoId = "";
                    et_expoId.setText("");
                    return false;
                }

                if (TextUtils.isEmpty(address)) {
                    et_address.requestFocus();
                }

                if (!TextUtils.isEmpty(expoId) && !TextUtils.isEmpty(address)) {
                    check();
                }
            } else if (et_address.hasFocus()) {
                address = et_address.getText().toString().replaceAll("\n", "").trim();

                if (Utils.checkInput(address) != Utils.INPUT_SUCCESS) {
                    Toast.makeText(mContext, "门禁地点输入有误", Toast.LENGTH_LONG).show();
                    address = "";
                    et_address.setText("");
                    return false;
                }

                if (TextUtils.isEmpty(expoId)) {
                    et_expoId.requestFocus();
                }

                if (!TextUtils.isEmpty(expoId) && !TextUtils.isEmpty(address)) {
                    check();
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}