package com.tuya.smartpad.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tuya.libgateway.TuyaGatewaySdk;
import com.tuya.libgateway.interfaces.GatewayCallbacks;
import com.tuya.libgateway.model.GatewayConfig;
import com.tuya.libgateway.upgrade.UpgradeEventCallback;
import com.tuya.libiot.TuyaIotSdk;
import com.tuya.libiot.model.DataPoint;
import com.tuya.smart.ai.common.utils.LogDaemon;
import com.tuya.smart.ai.common.utils.ProcessUtils;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IotGatewayActivity extends Activity {

    private static final String TAG = "IotGatewayActivity";
    private LogDaemon mLogDaemon = null;

    private Context mContext;
    private Context getContext() {
        return mContext;
    }

    private TuyaGatewaySdk mGateway;
    private TuyaIotSdk mIoT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        mIoT = TuyaIotSdk.getInstance();

        //############################################
        mGateway = TuyaGatewaySdk.getInstance();
        mGateway.setGatewayCallbacks(mGatewayCallbacks);

        //OTA upgrade callbacks
        mGateway.setUpgradeCallback(new UpgradeEventCallback() {
            @Override
            public void onUpgradeInfo(String version) {
                // need to upgrade
                mGateway.confirmUpgradeDownload("/sdcard/tuya_gateway/");
            }

            @Override
            public void onUpgradeDownloadStart() {
                // start to download upgrade package
            }

            @Override
            public void onUpgradeDownloadUpdate(int progress) {
                // download processing
            }

            @Override
            public void upgradeFileDownloadFinished(boolean success) {
                // download completed, install it
                if (success) {
                    mGateway.confirmUpgradeInstall();
                }
            }

            @Override
            public void onUpgradeFail(String msg) {

            }
        });

        setupLog();
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if(mLogDaemon != null) {
            mLogDaemon.stop();
        }
        finishAffinity();
    }


    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }


    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    private void startGateway() {
        String filedirs = getFilesDir().getAbsolutePath();
        Log.i(TAG, "file dir:" + filedirs);

        GatewayConfig config = new GatewayConfig();

        String storageDir = filedirs + File.separator + "storage" + File.separator;
        File file = new File(storageDir);
        if (!file.exists()) {
            file.mkdir();
        }
        String tmpDir = filedirs + File.separator + "tmp" + File.separator;
        file = new File(tmpDir);
        if (!file.exists()) {
            file.mkdir();
        }
        String binDir = filedirs + File.separator + "bin" + File.separator;
        Log.i(TAG, "bin dir:" + config.mBinDir + ", file dir:" + filedirs);
        file = new File(binDir);
        if (!file.exists()) {
            file.mkdir();
        }

        config.mPath = storageDir;
        config.mFirmwareKey =  "firmwarekey";
        config.mUUID =  "uuid";
        config.mAuthKey ="authkey";
        config.mVersion = "1.0.0";
        config.mPackageName = getPackageName();
        config.mSerialPort = "/dev/ttyS3";

        config.mTempDir = tmpDir;
        config.mBinDir = binDir;

        config.mIsCTS = true;
        config.mIsOEM = true;

        mGateway.gatewayStart(this, config);
        Log.v(TAG, "onStartClick over");

        findViewById(R.id.button_start).setEnabled(false);
        findViewById(R.id.button_test).setEnabled(false);

        findViewById(R.id.button_reset).setEnabled(true);
        findViewById(R.id.speach_test).setEnabled(true);
    }

    GatewayCallbacks mGatewayCallbacks  = new GatewayCallbacks() {
        @Override
        public void onStatusChanged(int status) {
            Log.v(TAG, "onStatusChanged " + status);
        }

        @Override
        public void onReboot() {
            Log.v(TAG, "onReboot");
            doReboot();
        }

        @Override
        public void onReset(int type) {
            Log.v(TAG, "onReset " + type);
            finish();
        }

        @Override
        public void onDataPointCommand(int type, int dttType, String cid, String multicastId,
                                       DataPoint[] dataPoint) {
            Log.v(TAG, "onDataPointCommand " + type);
            Log.v(TAG, "type:" + type + ", dttType:" + dttType + ", cid:" + cid + ", multicast Id:" + multicastId);
            for (int i = 0; i < dataPoint.length; i++) {
                dumpDataPoint(dataPoint[i]);
            }
        }

        @Override
        public void onNetworkStatus(int status) {
            Log.i(TAG, "onNetworkStatus " + status);
            if (status == TuyaGatewaySdk.Const.NETWORK_STATUS_CLOUD_CONNECTED) {
                Log.v(TAG, "connected to cloud");

                String id = mIoT.getId();
                Log.d(TAG, "gateway id is " + id);
            } else if (status == TuyaGatewaySdk.Const.NETWORK_STATUS_LAN_CONNECTED) {
                Log.v(TAG, "network connected");
            } else {
            }
        }

        @Override
        public String onGetIP() {
            String ipAddress = getLocalIpAddress();
            return ipAddress;
        }
        @Override
        public void onStartSuccess() {
            Log.v(TAG, "onStartSuccess");
            getToken();
        }

        @Override
        public void onStartFailure(int err) {
            Log.v(TAG, "onStartFailure:" + err);
        }

        @Override
        public String onGetLogFile() {
            Log.d(TAG, "onGetLogFile");
            if(mLogDaemon != null) {
                return mLogDaemon.getZippedLogFile();
            }
            return null;
        }

        @Override
        public String onGetMacAddress() {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            String address = info.getMacAddress();
            return address;
        }

        @Override
        public void onZigbeeServiceDied() {
            // zigbee has died, restart app
            Log.d(TAG, "onZigbeeServiceDied");
            doRebootApplicaion();
        }

        @Override
        public void onZigbeeError() {
            //zigbee went wrong
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "zigbee error, please check zigbee module", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private void setupLog() {
        String logPath = getExternalFilesDir("log").getAbsolutePath();
        //logPath/gateawy.log
        mLogDaemon = new LogDaemon(logPath, 10, 3 * 1024, "gateway");

        PermissionUtils.setOnRequestPermissionsResult(new OnRequestPermissionsResult() {
            @Override
            public void onRequestPermissionsResult(boolean success) {
                Log.d(TAG, "onRequestPermissionsResult " + success);
                if (success) {
                    mLogDaemon.setExpectation(android.os.Process.myPid(), "ServiceManager,ActivityManager,DEBUG,libc,crash_dump32,crash_dump64", true);
                    mLogDaemon.start();
                }
            }
        });
        PermissionUtils.verifyStoragePermissions(this);
    }

    private void onZigbeeTest(View v) {
        findViewById(R.id.button_start).setEnabled(false);

        ZigbeeTest zigbeeTest = new ZigbeeTest(this);
        final TextView tv = findViewById(R.id.text_test_result);
        zigbeeTest.setOnTestCompletion(new ZigbeeTest.OnTestCompletion() {
            @Override
            public void onTestResult(String result) {
                tv.setText(result);
            }
        });
        zigbeeTest.zigbeeTest();
    }

    public void onClick(View v) {
        if(v == findViewById(R.id.button_start)) {
            Log.v(TAG, "onStartClick");
            startGateway();
        } else if(v == findViewById(R.id.button_reset)) {
            Log.v(TAG, "onResetClick");
            findViewById(R.id.speach_test).setEnabled(false);
            mGateway.gatewayReset();
        } else if(v == findViewById(R.id.button_reboot)) {
            Log.v(TAG, "onRebootClick");
            doReboot();
        } else if(v == findViewById(R.id.button_getlog)) {
            Log.v(TAG, "onSignOutClick");
            Log.d(TAG, "id: " + mIoT.getId());
            mLogDaemon.getLogDir();
            ArrayList<String> files = mLogDaemon.getLogFiles();
            for(String str:files) {
                Log.d(TAG, "log file:" + str);
            }
            String f = mLogDaemon.getZippedLogFile();
            TextView tv = findViewById(R.id.log_file);
            tv.setText(f);
            Log.d(TAG, "file is " + f);
        } else if(v == findViewById(R.id.button_test)) {
            onZigbeeTest(v);
        }
    }

    private void doReboot() {
        Intent reboot = new Intent(Intent.ACTION_REBOOT);
        reboot.putExtra("nowait", 1);
        reboot.putExtra("interval", 1);
        reboot.putExtra("window", 0);
        sendBroadcast(reboot);
    }

    private void doRebootApplicaion() {
        Log.d(TAG, "doRebootApplicaion");
        List<Integer> pids = ProcessUtils.getChildProcess(android.os.Process.myPid());
        for(int pid : pids) {
            Log.d(TAG, "killing child process:" + pid);
            android.os.Process.killProcess(pid);
        }
        Log.d(TAG, "killing app process:" + android.os.Process.myPid());
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void onUpgradeClick(View v) {
//        Log.v(TAG, "onUpgradeClick");
//        String apkPath = "/sdcard/Download/app-gw.apk";
//        String apkPackage = "com.tuya.iotgateway";
//
//        mIotGateway.getUpgradeHelper().upgradeAPP(apkPath, apkPackage);

        mGateway.confirmUpgradeInstall();
//        UpgradeHelper helper = new UpgradeHelper(this, "", "", null);
//        helper.upgradeROM("1598609170", "/sdcard/update.zip");
    }

    private void getToken() {
        // TODO: get token from server and pass it to gateway sdk
        String token = "";
        Log.v(TAG, "token is " + token);
        mGateway.gatewayBindToken(token);
    }


    protected String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

    private void dumpDataPoint(DataPoint dataPoint) {
        Log.d(TAG, "id         :" + dataPoint.mId);
        Log.d(TAG, "type       :" + dataPoint.mType);
        Log.d(TAG, "timestamp  :" + dataPoint.mTimeStamp);
        Log.d(TAG, "data:" + dataPoint.mData);
        if(dataPoint.mType == DataPoint.TYPE_BOOL) {
            Log.d(TAG, "bool       :" + dataPoint.mData );
        } else if(dataPoint.mType == DataPoint.TYPE_STRING) {
            Log.d(TAG, "string     :" + dataPoint.mData);
        } else {
            Log.d(TAG, "value      :" + dataPoint.mData + "(" + dataPoint.mData + ")");
        }
    }

    private static void hexdump(byte[] data) {
        int offset = 0;
        int length = data.length;
        while (offset < length) {
            String line = String.format("%08x:  ", offset);
            for (int i = 0; i < 16; ++i) {
                if (i == 8) {
                    line += " ";
                }
                if (offset + i >= length) {
                    line += "   ";
                } else {
                    line += String.format("%02x ", data[offset + i]);
                }
            }

            line += " ";

            for (int i = 0; i < 16; ++i) {
                if (offset + i >= length) {
                    break;
                }

                if (data[offset + i] >= 32 && data[offset + i] < 127) {
                    line += String.format("%c", (char) data[offset + i]);
                } else {
                    line += ".";
                }
            }

            Log.v(TAG, line);

            offset += 16;
        }
    }

    public void jumpToSpeach(View view) {
        Intent intent = new Intent(this, SpeechTestActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent " + event);
        return super.onTouchEvent(event);
    }
}
