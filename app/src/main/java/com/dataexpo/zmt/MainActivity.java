package com.dataexpo.zmt;

import androidx.annotation.NonNull;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dataexpo.zmt.common.DBUtils;
import com.dataexpo.zmt.common.Utils;
import com.dataexpo.zmt.pojo.SaveData;
import com.dataexpo.zmt.readidcard.DynamicPermission;
import com.idata.fastscandemo.R;
import com.idata.ise.scanner.decoder.CamDecodeAPI;
import com.idata.ise.scanner.decoder.DecodeResult;
import com.idata.ise.scanner.decoder.DecodeResultListener;
import com.idatachina.imeasuresdk.IMeasureSDK;
import com.ivsign.android.IDCReader.IdentityCard;
import com.yishu.YSNfcCardReader.NfcCardReader;
import com.yishu.util.ByteUtil;
import com.zyao89.view.zloading.ZLoadingDialog;
import com.zyao89.view.zloading.Z_TYPE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends BascActivity implements DecodeResultListener, View.OnClickListener, TextWatcher {
    private final static String TAG = MainActivity.class.getName();
    public static final int STATUS_INIT = 0;
    public static final int STATUS_INPUT_CODEORID = 1;
    public static final int STATUS_INPUT_TEMPERATURE = 2;
    public static final int STATUS_INPUT_COMMIT = 3;

    public static final int INPUT_SUCCESS = 0;
    public static final int INPUT_HAVE_NET_ADDRESS = 1;
    public static final int INPUT_ONLY_NUM = 2;
    public static final int INPUT_CHECK_NET_ADDRESS = 3;
    public static final int INPUT_NULL = 4;
    public static final int INPUT_TOO_LONG = 5;

    SoundManager manager;

    private TextView scanStatus;
    //private TextView costTime;
    //private TextView barcodeType;
    private EditText tv_temp_value;
    private TextView tv_code_value;
    private EditText tv_idcard;
    private EditText tv_name_value;
    private TextView tv_sex_value;
    private TextView tv_n_value;
    private TextView tv_create_value;
    private TextView tv_addr_value;
    private TextView tv_plc_value;
    private TextView tv_data_value;
    private ImageView iv_head;
    private ImageView iv_menu;
    private Button btn_scan;
    private Button btn_last;

    private int scaning = 0;
    private MyHandler mHandler;
    private static NfcCardReader nfcCardReaderAPI;
    private DynamicPermission dynamicPermission;
    private boolean isActive = false;

    private static Intent thisIntent;
    private IMeasureSDK mIMeasureSDK;

    private volatile int readStatus = notReading;
    private final static  int reading = 1;
    private final static  int notReading = 0;
    private SaveData saveData = null;

    private volatile int mStatus = STATUS_INIT;

    private boolean bNFCInput = false;
    private Context mContext = null;
    private String usage_mode = "offline";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = MainActivity.this;
        DBUtils.getInstance().create(this);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        tv_code_value = findViewById(R.id.tv_code_value);
        tv_temp_value = findViewById(R.id.tv_temp_value);
        tv_idcard = findViewById(R.id.tv_id_value);
        tv_name_value = findViewById(R.id.tv_name_value);
        tv_sex_value = findViewById(R.id.tv_sex_value);
        tv_n_value = findViewById(R.id.tv_n_value);
        tv_create_value = findViewById(R.id.tv_create_value);
        tv_addr_value = findViewById(R.id.tv_addr_value);
        tv_plc_value = findViewById(R.id.tv_plc_value);
        tv_data_value = findViewById(R.id.tv_data_value);
        iv_head = findViewById(R.id.iv_head);
        iv_menu = findViewById(R.id.iv_menu);
        iv_menu.setOnClickListener(this);
        btn_scan = findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(this);
        btn_last = findViewById(R.id.btn_last);
        btn_last.setOnClickListener(this);

        tv_temp_value.addTextChangedListener(this);

        manager = new SoundManager(this);
        manager.initSound();
        mHandler = new MyHandler(MainActivity.this);
        nfcCardReaderAPI = new NfcCardReader(mHandler,MainActivity.this);

        //权限判断
        dynamicPermission = new DynamicPermission(MainActivity.this, new DynamicPermission.PassPermission() {
            @Override
            public void operation() {
            }
        });
        dynamicPermission.getPermissionStart();
        mHandler.sendEmptyMessage(ByteUtil.MESSAGE_VALID_NFCSTART);
        mIMeasureSDK = new IMeasureSDK(getBaseContext());
        mIMeasureSDK.init(initCallback);
        initView();
        CamDecodeAPI.getInstance(MainActivity.this)
                .SetOnDecodeListener(MainActivity.this);
        tv_temp_value.setHintTextColor(Color.WHITE);
        usage_mode = Utils.getUsageMode(mContext);
    }

    private void initView() {
        saveData = null;
        btn_last.clearFocus();
        btn_last.setEnabled(false);
        btn_scan.setText("扫码");
        tv_temp_value.setText("");
        tv_temp_value.setHint("--.--");
        tv_temp_value.setEnabled(false);
        tv_name_value.setText("");
        //tv_code_value.setText("");
        tv_idcard.setText("");

        tv_name_value.setEnabled(true);
        tv_idcard.setEnabled(true);
        tv_temp_value.setEnabled(false);

        iv_head.setImageDrawable(null);
        mStatus = STATUS_INPUT_CODEORID;
        bNFCInput = false;
        tv_idcard.requestFocus();
    }

    private IMeasureSDK.InitCallback initCallback = new IMeasureSDK.InitCallback() {
        @Override
        public void success() {
            Log.d(TAG, "success: 上电成功");
            //Toast.makeText(getBaseContext(), "上电成功", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void failed(int code, String msg) {
            Log.d(TAG, "failed: 上电失败，"+msg);
            //Toast.makeText(getBaseContext(), "上电失败[" + msg + "]", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void disconnect() {
            //Toast.makeText(getBaseContext(), "与测温服务断开", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "disconnect: 与测温服务断开");
            mIMeasureSDK.reconect();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        Log.i("onPause","enter onPause");
        isActive = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mHandler.getNfcInit()) {
            nfcCardReaderAPI.enabledNFCMessage();
//            thisIntent = getIntent();
//            mHandler.sendEmptyMessage(ByteUtil.MESSAGE_VALID_NFCBUTTON);
//            Log.i(TAG,"onResume to MESSAGE_VALID_NFCBUTTON " );
        }

        isActive = true;

        Intent intent = getIntent();
        String action = intent.getAction();
        Log.i(TAG,"onResume " + action);

        if("android.nfc.action.TECH_DISCOVERED".equals(action)){
            Log.i(TAG,"onResume 1");
            if(thisIntent == null){
//                if(leftViewOperation != null){
//                    leftViewOperation.setMode(2);
//                }
                Log.i(TAG,"onResume 2");
                mHandler.sendEmptyMessage(ByteUtil.MESSAGE_VALID_NFCSTART);
//                cardInfo.clearData();
//                main_tvContent_show.setText("");
                thisIntent = intent;
                mHandler.sendEmptyMessage(ByteUtil.MESSAGE_VALID_NFCBUTTON);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i("Info","enter onNewIntent");
        super.onNewIntent(intent);
        thisIntent = intent;
        mHandler.sendEmptyMessage(ByteUtil.MESSAGE_VALID_NFCBUTTON);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown  status: " + mStatus + " --- " + keyCode);
        if (600==keyCode||601==keyCode||602==keyCode) {
            //有code之后直接进行测温
            if (!"".equals(tv_idcard.getText().toString()) && !"提交".equals(btn_scan.getText().toString())) {
                //有值，进入测温
                mStatus = STATUS_INPUT_TEMPERATURE;
                btn_last.setEnabled(true);
                btn_scan.setText("测温");
                tv_temp_value.requestFocus();
            }

            if (mStatus == STATUS_INIT || mStatus == STATUS_INPUT_CODEORID) {
                //进行扫码
                scaning = 1;
                CamDecodeAPI.getInstance(MainActivity.this).ScanBarcode(
                        MainActivity.this);

            } else if (mStatus == STATUS_INPUT_TEMPERATURE) {
                //进入测温
                tv_temp_value.setEnabled(true);
                tv_name_value.setEnabled(false);
                tv_idcard.setEnabled(false);
                toMeasure();
            } else if (mStatus == STATUS_INPUT_COMMIT) {
                commit();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CamDecodeAPI.getInstance(this).Dispose();
    }

    @Override
    public void onDecodeResult(DecodeResult decodeResult) {
        scaning = 0;
        restCardValue();
        if (null != decodeResult){
            //扫码完成返回
            manager.playSoundAndVibrate(true, false);
            String code = new String(decodeResult.getBarcodeData());

            if (INPUT_SUCCESS == checkInput(code)) {
                //tv_code_value.setText(code);
                tv_idcard.setText(code);
                tv_name_value.setEnabled(false);
                tv_idcard.setEnabled(false);
                tv_temp_value.setEnabled(true);
                btn_scan.setText("测温");
                btn_last.setEnabled(true);
                mStatus = STATUS_INPUT_TEMPERATURE;
                tv_temp_value.requestFocus();
                tv_temp_value.setSelection(tv_temp_value.getText().toString().length());
                bNFCInput = false;
            } else {
                Toast.makeText(this, "扫描内容异常", Toast.LENGTH_SHORT).show();
            }
        }else {
            //scanStatus.setText("用户取消");
        }
    }

    private void restCardValue(){
        //tv_code_value.setText("");
        iv_head.setImageDrawable(null);
        tv_name_value.setText("");
        tv_idcard.setText("");
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick  status: " + mStatus);
        usage_mode = Utils.getUsageMode(mContext);
        switch (v.getId()) {
            case R.id.iv_menu:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final String[] module = {"查看数据", "导出数据到zmtRecord","后台连接设置","数据上传","设备编码：" + Utils.getSerialNumber(), "当前版本:V1.0"};
                //builder.setTitle("选择读取模式");
                //builder.setIcon(R.mipmap.ic_launcher);
                builder.setSingleChoiceItems(module, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                //查看数据
                                startActivity(new Intent(MainActivity.this, ScanRecordActivity.class));
                                break;
                            case 1:
                                //导出数据
                                List<SaveData> codes = DBUtils.getInstance().listAllOffLine();
                                if (codes.size() > 0) {
                                    final ZLoadingDialog zdialog = new ZLoadingDialog(mContext);
                                    zdialog.setLoadingBuilder(Z_TYPE.CIRCLE)//设置类型
                                            .setLoadingColor(Color.BLACK)//颜色
                                            .setHintText("正在导出到/sdcard/zmtRecord/...")
                                            .setCanceledOnTouchOutside(false)
                                            .show();
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
                                    long dateTime = new Date().getTime();
                                    String date = simpleDateFormat.format(dateTime);
                                    String fileName = date + ".txt";
                                    File file = new File("/sdcard/zmtRecord/");
                                    if (!file.exists()) {
                                        file.mkdirs();
                                    }
                                    file = new File(file, fileName);
                                    try {
                                        FileWriter fw = new FileWriter(file, true);
                                        BufferedWriter bw = new BufferedWriter(fw);
                                        PrintWriter printWriter = new PrintWriter(bw);
                                        for (SaveData code : codes) {
                                            String strContent = code.getName() + "&&" + code.getIdcard() + "&&" + code.getEucode() + "&&" + code.getTemp() + "&&" + code.getTime() +"\n";
                                            printWriter.println(strContent);
                                        }
                                        printWriter.close();
                                        bw.close();
                                        fw.close();
                                        zdialog.cancel();
                                        Toast.makeText(mContext, "导出成功, 目录是/sdcard/zmtRecord/", Toast.LENGTH_LONG).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        zdialog.cancel();
                                        Toast.makeText(mContext, "导出失败", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(mContext, "没有数据可导出", Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 2:
                                //连接设置在线或者离线
                                final androidx.appcompat.app.AlertDialog.Builder normalDialog =
                                        new androidx.appcompat.app.AlertDialog.Builder(mContext);

                                normalDialog.setMessage("当前模式： " + (usage_mode.equals("online") ? "在线模式" : "离线模式"));

                                normalDialog.setPositiveButton("设置在线",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Utils.saveUsageMode(mContext, "online");
                                                Toast.makeText(mContext, "当前已设置为在线模式", Toast.LENGTH_LONG).show();
                                                usage_mode = "online";
                                            }
                                        });
                                normalDialog.setNegativeButton("设置离线",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Utils.saveUsageMode(mContext, "offline");
                                                //Toast.makeText(mContext, "当前已设置为离线模式", Toast.LENGTH_LONG).show();
                                                Toast.makeText(mContext, "当前仅支持离线模式", Toast.LENGTH_LONG).show();
                                                //usage_mode = "offline";
                                            }
                                        });
                                // 显示
                                normalDialog.show();
                                break;
                            case 3:
                                //数据上传
                                startActivity(new Intent(mContext, UpLoadCheckExpoid.class));
                                break;
                            case 4:
                                //
                                break;
                            default: break;
                        }
                        dialog.dismiss();
                    }
                });
                builder.show();

                break;

            case R.id.btn_scan:
                if (mStatus == STATUS_INIT || mStatus == STATUS_INPUT_CODEORID) {
                    //去扫码
                    scaning = 1;
                    CamDecodeAPI.getInstance(MainActivity.this).ScanBarcode(
                            MainActivity.this);
                } else if (mStatus == STATUS_INPUT_TEMPERATURE) {
                    //去测温
                    toMeasure();
                    tv_name_value.setEnabled(false);
                    tv_idcard.setEnabled(false);
                    tv_temp_value.setEnabled(true);
                } else if (mStatus == STATUS_INPUT_COMMIT) {
                    commit();
                }
                break;

            case R.id.btn_last:
                if (mStatus == STATUS_INPUT_TEMPERATURE) {
                    mStatus = STATUS_INPUT_CODEORID;
                    btn_last.setEnabled(false);
                    btn_scan.setText("扫码");
                    tv_temp_value.setEnabled(false);
                    tv_name_value.setEnabled(true);
                    tv_idcard.setEnabled(true);
                    tv_temp_value.setEnabled(false);
                    tv_idcard.requestFocus();
                    tv_idcard.setSelection(tv_idcard.getText().toString().length());

                } else if (mStatus == STATUS_INPUT_COMMIT) {
                    mStatus = STATUS_INPUT_TEMPERATURE;
                    btn_scan.setText("测温");
                    tv_temp_value.setHint("--.--");
                    tv_temp_value.setEnabled(true);
                    tv_name_value.setEnabled(false);
                    tv_idcard.setEnabled(false);
                }
                break;
            default:
        }
    }

    private void commit() {
        saveData = new SaveData();
        //String code = tv_code_value.getText().toString();
        String name = tv_name_value.getText().toString();
        String idcard = tv_idcard.getText().toString();
        String temp = tv_temp_value.getText().toString();

        if ("".equals(name) && "".equals(idcard)) {
            Toast.makeText(this, "请输入证件码或者采集身份证数据", Toast.LENGTH_SHORT).show();
            btn_scan.setText("测温");
            tv_temp_value.setHint("--.--");
            tv_temp_value.setEnabled(true);
            tv_name_value.setEnabled(false);
            tv_idcard.setEnabled(false);
            btn_last.setEnabled(false);
            return;
        }

        if ("".equals(name)) {
            //名字是空的，大概率是手动输入了一个码
            saveData.setEucode(idcard);
        } else {
            //两个输入框都有，判断是姓名和身份证（nfc或者手动输入）
            saveData.setName(name);
            saveData.setIdcard(idcard);
        }
        saveData.setTemp(temp);
//
//        saveData.setEucode(tv_code_value.getText().toString());
//
//        if (!"".equals(tv_code_value.getText().toString()) &&
//                tv_idcard.getText().toString().equals(tv_code_value.getText().toString())) {
//
//        } else {
//            saveData.setEucode(tv_idcard.getText().toString());
//        }
//
//        saveData.setName(tv_name_value.getText().toString());
//        saveData.setTemp(tv_temp_value.getText().toString());
//        saveData.setIdcard(tv_idcard.getText().toString());
        //保存到数据库
        Toast.makeText(this, "eucode: " + saveData.getEucode() + " - name: " + saveData.getName() + " - temp: " + saveData.getTemp() +
                " - idcatd: " + saveData.getIdcard(), Toast.LENGTH_LONG).show();
        Log.i(TAG, "eucode: " + saveData.getEucode() + " - name: " + saveData.getName() + " - temp: " + saveData.getTemp() +
                " - idcatd: " + saveData.getIdcard());

        long dateTime = new Date().getTime();
        String date = Utils.formatTime(dateTime, "yyyy-MM-dd HH:mm:ss");
        saveData.setTime(date);
        saveData.setAddress(tv_addr_value.getText().toString());

        //判断模式
        if ("online".equals(usage_mode)) {
            saveData.setModeType(1);
        } else {
            saveData.setModeType(0);
        }

        DBUtils.getInstance().insertData(saveData);

        initView();
    }

    private void toMeasure() {
        if (notReading==readStatus) {
            readStatus = reading;
            mIMeasureSDK.read(new IMeasureSDK.TemperatureCallback() {
                @Override
                public void success(final double temp) {
                    readStatus = notReading;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(MainActivity.this,"温度："+temp,Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "success: "+temp);
                            DecimalFormat df = new DecimalFormat("#.00");
                            tv_temp_value.setText(df.format(temp));
                            //设置测温为提交
                            btn_scan.setText("提交");
                            mStatus = STATUS_INPUT_COMMIT;
                            tv_temp_value.setSelection(tv_temp_value.getSelectionEnd()+5);
                        }
                    });
                }

                @Override
                public void failed(int code, final  String msg) {
                    readStatus = notReading;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "测温失败[" + msg + "]", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "run: 测温失败,"+msg);
                        }
                    });
                }
            });
        }else{
            Toast.makeText(this,"正在读取温度，请稍等",Toast.LENGTH_SHORT).show();
        }
    }

    public static int checkInput(String input) {
        if (TextUtils.isEmpty(input)) {
            return INPUT_NULL;
        }

        if (input.contains("http")) {
            return INPUT_HAVE_NET_ADDRESS;
        }

        if (input.contains("www.")) {
            return INPUT_HAVE_NET_ADDRESS;
        }

        if (input.length() > 50) {
            return INPUT_TOO_LONG;
        }

        return INPUT_SUCCESS;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Log.i(TAG, "onTextChanged" + s.toString() + " start " + start + " befor " + before + " count " + count);
        String temp = s.toString();
        if (count == 1 && temp.length() == 2) {
            temp = temp + ".";
            tv_temp_value.setText(temp);
            tv_temp_value.setSelection(tv_temp_value.getSelectionEnd() + 3);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.i(TAG, "afterTextChanged" + s.toString());
//        if (s.toString().length() == 2) {
//            tv_temp_value.setText(s.toString() + ".");
//        }
    }

    private static class MyHandler extends Handler{
        private WeakReference<MainActivity> activityWeakReference;
        //蓝牙nfc初始化之后不再初始化
        private boolean nfcInit  = false;
        private boolean btInit = false;

        public boolean getNfcInit() {
            return nfcInit;
        }

        public MyHandler(MainActivity activity){
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final MainActivity activity = activityWeakReference.get();
            if(activity == null){
                return;
            }

            //activity.clearData();

            switch (msg.what){
                case ByteUtil.MESSAGE_VALID_NFCSTART:
                    Log.i("Info","enter MESSAGE_VALID_NFCSTART");
//                    activity.hideBtBtn();
//                    activity.setCurBtName("未连接");
//                    bluetoothReaderAPI.closeBlueTooth();
//
//                    main_btPic_show.setVisibility(View.GONE);
//                    main_nfcPic_show.setVisibility(View.VISIBLE);
//                    main_tvContent_show.setText("");

                    if(nfcInit){
                        break;
                    }

                    Boolean enabledNFC = false;
                    if(activity.isActive){
                        enabledNFC = nfcCardReaderAPI.enabledNFCMessage();
                    }
                    if(enabledNFC){
                        nfcInit = true;
//                        Toast.makeText(activity,"NFC初始化成功",Toast.LENGTH_SHORT).show();
                    }else{
//                        main_btPic_show.setVisibility(View.VISIBLE);
//                        main_nfcPic_show.setVisibility(View.GONE);
                        Toast.makeText(activity,"NFC初始化失败",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ByteUtil.MESSAGE_VALID_BTSTART:
                    Log.i("Info","enter MESSAGE_VALID_BTSTART");
//                    main_btPic_show.setVisibility(View.VISIBLE);
//                    main_nfcPic_show.setVisibility(View.GONE);
//                    main_tvContent_show.setText("");
//
//                    if(btInit){
//                        break;
//                    }
//                    if(bluetoothReaderAPI.checkBltDevice()){
//                        btInit = true;
////                        Toast.makeText(activity,"蓝牙初始化成功",Toast.LENGTH_SHORT).show();
//                    }else{
//                        main_btPic_show.setVisibility(View.GONE);
//                        main_nfcPic_show.setVisibility(View.VISIBLE);
//                        Toast.makeText(activity,"当前设备无蓝牙或者蓝牙未开启",Toast.LENGTH_SHORT).show();
//                    }

                    break;
                case ByteUtil.BLUETOOTH_CONNECTION_SUCCESS:
                    Log.i("Info","enter BLUETOOTH_CONNECTION_SUCCESS");
//                    activity.showBtBtn();
//
////                    Toast.makeText(activity,"设备连接成功",Toast.LENGTH_SHORT).show();
//                    BluetoothDevice device = (BluetoothDevice) msg.obj;
//                    btdevice = device;
//                    main_tvContent_show.setText("");
//                    activity.setCurBtName(device.getName());
//                    new SPUtil(activity).putConnBtDevice(device.getAddress());
                    break;
                case ByteUtil.BLUETOOTH_CONNECTION_FAILED:
                    Log.i("Info","enter BLUETOOTH_CONNECTION_FAILED");
//                    activity.hideBtBtn();
//                    activity.setCurBtName("未连接");
                    Toast.makeText(activity,"设备连接失败，请重新连接",Toast.LENGTH_SHORT).show();
                    break;
                case ByteUtil.MESSAGE_VALID_NFCBUTTON:
                    Log.i("Info","enter MESSAGE_VALID_NFCBUTTON");
                    //boolean isNFC = nfcCardReaderAPI.isNFC(thisIntent);
                    boolean isNFC = true;
                    if(isNFC){
                        nfcCardReaderAPI.CreateCard(thisIntent);
                    }else{
                        Toast.makeText(activity,"获取nfc失败",Toast.LENGTH_SHORT).show();
                    }
                    thisIntent = null;
                    break;
                case ByteUtil.MESSAGE_VALID_BTBUTTON:
                    Log.i("Info","enter MESSAGE_VALID_BTBUTTON");
//                    activity.btBtnDisabled();
//                    activity.btBtnDisabled();
//                    if(activity.getMode() == 3){
//                        Log.i("Info","enter OTG register");
//                        boolean otgInit = otgCardReaderAPI.registerOTGCard();
//                        if(otgInit){
//                            otgCardReaderAPI.readCard();
//                        }
//                        else {
//                            activity.btBtnEnabled();
//                            Toast.makeText(activity,"OTG初始化失败",Toast.LENGTH_SHORT).show();
//                        }
//                    }else{
//                        bluetoothReaderAPI.readCard();
//                    }
                    break;
                case ByteUtil.READ_CARD_START:
                    Log.i("Info","enter READ_CARD_START");
                    //main_tvContent_show.setText("开始读卡，请稍后...");
                    Toast.makeText(activity,"开始读卡",Toast.LENGTH_SHORT).show();
                    break;
                case ByteUtil.READ_CARD_FAILED:
                    Log.e("Info","enter READ_CARD_FAILED");
                    //activity.btBtnEnabled();

                    /*//显示读卡时间
                    System.out.println("蓝牙读取时间："+bluetoothReaderAPI.getTime());
                    System.out.println("NFC读取时间："+nfcCardReaderAPI.getTime());*/

                    //read failed (NFC)
                    if(78 !=nfcCardReaderAPI.getErrorFlag()){
                        String message = nfcCardReaderAPI.getMessage();

                        //when mode is bluetooth,message is ""
                        if(!("".equals(message))) {
                            //main_tvContent_show.setText("");
                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }

                    //Toast.makeText(activity,"读卡失败:"+msg.obj,Toast.LENGTH_SHORT).show();
                    //main_tvContent_show.setText("读卡失败："+msg.obj);
                    break;
                case ByteUtil.READ_CARD_SUCCESS:
                    Log.i("Info","enter READ_CARD_SUCCESS");

                    /*//显示读卡时间
                    System.out.println("蓝牙读取时间："+bluetoothReaderAPI.getTime());
                    System.out.println("NFC读取时间："+nfcCardReaderAPI.getTime());*/

//                    //read failed (NFC)
//                    if(78 !=nfcCardReaderAPI.getErrorFlag()){
//                        String message = nfcCardReaderAPI.getMessage();
//
//                        //when mode is bluetooth,message is ""
//                        if(!("".equals(message))) {
//                            main_tvContent_show.setText("");
//                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
//                            break;
//                        }
//                    }

                    //main_tvContent_show.setText("读卡成功！");
                    //在初始化或者输入编码时可以更改
                    if (activity.mStatus == activity.STATUS_INIT || activity.mStatus == activity.STATUS_INPUT_CODEORID) {
                        Toast.makeText(activity, "读卡成功", Toast.LENGTH_SHORT).show();
                        IdentityCard card = (IdentityCard) msg.obj;
                        if (card != null) {
                            final String name = card.getNameText();
                            final String sex = card.getSexText();
                            final String birthday = card.getBirthdayText();
                            final String nation = card.getMingZuText();
                            final String address = card.getAddressText();
                            final String number = card.getNumberText();
                            final String qianfa = card.getQianfaText();
                            final String effdate = card.getEffectiveDate();
                            Bitmap head = card.getImage();

                            if (head == null) {
                                Toast.makeText(activity, "头像读取失败", Toast.LENGTH_SHORT).show();
                            }

                            final Bitmap personImg = card.getImage();

                            Log.i("Info:", name + "\n" + sex + "\n" + birthday + "\n" + nation + "\n" + address + "\n" + number + "\n" + qianfa + "\n" + effdate + "\n");
                            //activity.setData(card);
                            activity.tv_name_value.post(new Runnable() {
                                @Override
                                public void run() {
                                    activity.tv_idcard.setText(number);
                                    activity.tv_name_value.setText(name);
//                                    activity.tv_sex_value.setText(sex);
//                                    activity.tv_n_value.setText(nation);
//                                    activity.tv_create_value.setText(birthday);
                                    activity.tv_addr_value.setText(address);
//                                    activity.tv_plc_value.setText(qianfa);
//                                    activity.tv_data_value.setText(effdate);
                                    activity.iv_head.setImageBitmap(personImg);
                                    //activity.tv_code_value.setText("");
                                    //设置进入测温
                                    activity.btn_scan.setText("测温");
                                    activity.btn_last.setEnabled(true);
                                    activity.mStatus = activity.STATUS_INPUT_TEMPERATURE;
                                    activity.tv_name_value.setEnabled(false);
                                    activity.tv_idcard.setEnabled(false);
                                    activity.tv_temp_value.setEnabled(true);
                                    activity.bNFCInput = true;
                                    activity.tv_temp_value.requestFocus();
                                }
                            });
                        }
                    }
                    break;
                case ByteUtil.MESSAGE_VALID_OTGSTART:
                    //activity.clearData();

                    Log.i("Info","enter MESSAGE_VALID_OTGSTART");
//                    activity.setCurBtName("未连接");
//                    bluetoothReaderAPI.closeBlueTooth();
//                    main_btPic_show.setVisibility(View.VISIBLE);
//                    main_nfcPic_show.setVisibility(View.GONE);
//                    main_tvContent_show.setText("");
//                    activity.showBtBtn();
                    break;
                case ByteUtil.BTREAD_BUTTON_ENABLED:
                    Log.i("Info","enter BTREAD_BUTTON_ENABLED");
                    //activity.btBtnEnabled();
                    break;
                default:break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        dynamicPermission.permissionRequestOperation(requestCode, permissions, grantResults);
    }


}
