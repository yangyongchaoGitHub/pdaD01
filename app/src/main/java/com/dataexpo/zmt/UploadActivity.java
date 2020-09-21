package com.dataexpo.zmt;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.dataexpo.zmt.common.DBUtils;
import com.dataexpo.zmt.common.HttpCallback;
import com.dataexpo.zmt.common.HttpService;
import com.dataexpo.zmt.common.URLs;
import com.dataexpo.zmt.common.Utils;
import com.dataexpo.zmt.pojo.MsgBean;
import com.dataexpo.zmt.pojo.SaveData;
import com.google.gson.Gson;
import com.idata.fastscandemo.R;

import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

public class UploadActivity extends BascActivity implements View.OnClickListener {
    private static final String TAG = UploadActivity.class.getSimpleName();

    private static final int UPLOAD_WAIT = 0;
    private static final int UPLOAD_ING = 1;
    private static final int UPLOAD_STOP = 2;
    private Context mContext;
    private TextView tv_total;
    private TextView tv_success;
    private TextView tv_wait;
    private TextView tv_upload_warning;
    private List<SaveData> codes;
    private String expo_id;
    private String address;
    private HttpCallback callback;
    private int success;
    private int wait;
    private int total;
    private volatile int uploadStatus = UPLOAD_STOP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_upload);
        initView();
        initData();
        //tryUpload();
    }

    private void initData() {
        codes = DBUtils.getInstance().listAllOffLine();
        success = 0;
        total = wait = codes.size();
        tv_total.setText(String.valueOf(total));
        tv_success.setText("0");
        tv_wait.setText(String.valueOf(wait));

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            expo_id = bundle.getString("Expo_id");
            address = bundle.getString("Add");
        }
    }

    private void initView() {
        tv_total = findViewById(R.id.tv_upload_scan_total_value);
        tv_success = findViewById(R.id.tv_upload_success_total_value);
        tv_wait = findViewById(R.id.tv_upload_wait_total_value);
        tv_upload_warning = findViewById(R.id.tv_upload_warning_text);
        tv_upload_warning.setOnClickListener(this);
        findViewById(R.id.btn_stop_upload).setOnClickListener(this);
        findViewById(R.id.btn_upload_check_expo_back).setOnClickListener(this);
    }

    private void tryUpload() {
        if (uploadStatus == UPLOAD_ING) {
            return;
        }
        uploadStatus = UPLOAD_ING;
        tv_upload_warning.setText(R.string.upload_no_close);

        new Thread(new Runnable() {
            @Override
            public void run() {
                upLoadData();
                Log.i(TAG, "upload exit!!!!!!");
                uploadStop();
            }
        }).start();
    }

    private void uploadStop() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_upload_warning.setText(R.string.upload_success);
                UploadActivity.this.finish();
                uploadEnd();
            }
        });
    }

    private void upLoadData() {
        String url = URLs.offLineUploadCT;
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("expoId", expo_id);
        hashMap.put("address", address);
        String serial = Utils.getSerialNumber();

        for (final SaveData code:codes) {
            if (uploadStatus == UPLOAD_STOP) {
                break;
            }

            if (null != code.getEucode() && !"".equals(code.getEucode()) && !"null".equals(code.getEucode())) {
                Log.i(TAG, "----------------------- eucode " + code.getEucode());
                hashMap.put("eucode", code.getEucode());
            }
            if (null != code.getTime() && !"".equals(code.getTime()) && !"null".equals(code.getTime())) {
                hashMap.put("time", code.getTime() + "");
            }
            if (null != code.getName() && !"".equals(code.getName()) && !"null".equals(code.getName())) {
                hashMap.put("name", code.getName() + "");
            }
            if (null != code.getIdcard() && !"".equals(code.getIdcard()) && !"null".equals(code.getIdcard())) {
                hashMap.put("idcard", code.getIdcard() + "");
            }
            if (null != code.getAddress() && !"".equals(code.getAddress()) && !"null".equals(code.getAddress())) {
                hashMap.put("useraddress", code.getAddress() + "");
            }
            if (null != code.getTemp() && !"".equals(code.getTemp()) && !"null".equals(code.getTemp())) {
                hashMap.put("temperature", code.getTemp() + "");
            }
            if (null != code.getModeType()) {
                hashMap.put("modeType", code.getModeType() + "");
            }
            hashMap.put("deviceKey", serial + "");

            Log.i(TAG, "euCode: " + code.getEucode() + " euFileCode: " + code.getIdcard() + " " + hashMap.get("eucode"));
            synchronized (this) {
                //HttpService.postWithParams(mContext, url, hashMap, callback);
                HttpService.postWithParams(mContext, url, hashMap, new HttpCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                    }
                    @Override
                    public void onResponse(String response, int id) {
                        final MsgBean result = new Gson().fromJson(response, MsgBean.class);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_wait.setText(String.valueOf(--wait));

                                if (result.code == 200) {
                                    tv_success.setText(String.valueOf(++success));

                                    DBUtils.getInstance().delData(code.getId());
                                } else {
                                    //tv_success.setText(String.valueOf(++success));
                                    //tv_wait.setText(String.valueOf(++wait));
                                    //Toast.makeText(mContext, "上传失败id: " + code.eufilecode, Toast.LENGTH_SHORT).show();
                                }
                                Log.i(TAG, "total:" + total + " success:" + success);
                                if(wait == 0){
                                    uploadEnd();
                                }
                            }});
                    }
                });
            }
        }
    }

    private void uploadEnd() {
        Intent intent = new Intent();

        intent.putExtra("success_count", success);
        intent.setClass(mContext, UploadSuccessActivity.class);

        startActivity(intent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (codes != null && codes.size() > 0) {
                tryUpload();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_upload_warning_text:
                tryUpload();
                break;

            case R.id.btn_stop_upload:
                uploadStatus = UPLOAD_STOP;
                break;
            case R.id.btn_upload_check_expo_back:
                finish();
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        uploadStatus = UPLOAD_STOP;
        super.onResume();
    }

    private static class UploadAnsync extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }
}