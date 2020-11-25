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


import com.tuya.smart.android.demo.speech.SpeechTestActivity;
import com.tuya.smart.iotgateway.gateway.TuyaIotGateway;
import com.tuya.smart.iotgateway.logutils.LogDaemon;
import com.tuya.smart.iotgateway.upgrade.UpgradeEventCallback;
import com.tuya.smart.iotgateway.utils.ProcessUtils;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        //############################################
        mIotGateway = TuyaIotGateway.getInstance();
        mIotGateway.setGatewayListener(mGatewayListener);

        //OTA升级回调
        mIotGateway.setUpgradeCallback(new UpgradeEventCallback() {
            @Override
            public void onUpgradeInfo(String version) {
                // TODO: 2020-02-28 收到新版本信息 主动开始下载
                mIotGateway.confirmUpgradeDownload();
            }

            @Override
            public void onUpgradeDownloadStart() {
                // TODO: 2020-02-18 开始升级文件下载
            }

            @Override
            public void onUpgradeDownloadUpdate(int progress) {
                // TODO: 2020-02-18 下载进度
            }

            @Override
            public void upgradeFileDownloadFinished(boolean success) {
                // TODO: 2020-02-18 下载完成 主动触发安装
                if (success) {
                    mIotGateway.confirmUpgradeInstall();
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
        // 恢复设置，需要杀死进程，当然也可以重启设备
        /**
         * 重启应用或者直接重启设备doReboot
         * 方法一、用android.os.Process.killProcess，封装在doRebootApplicai依次杀掉子进程
         * 方法二、用 am force-stop，可以杀子进程，但是需要system app才有权限。
         */
        doRebootApplicaion();
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

    private TuyaIotGateway mIotGateway;

    private void startGateway() {
        String filedirs = getFilesDir().getAbsolutePath();
        Log.i(TAG, "file dir:" + filedirs);

        TuyaIotGateway.Config config = new TuyaIotGateway.Config();

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
        config.mFirmwareKey =  "ddddd";
        config.mUUID =  "d3465e05ee453bea";
        config.mAuthKey ="5sGSglzY6QQJzq8UEUMwHym3TCfKDx7z";
        config.mVersion = "1.0.0";
        config.mPackageName = getPackageName();
        config.mSerialPort = "/dev/ttyS3";

        config.mTempDir = tmpDir;
        config.mBinDir = binDir;

        config.mIsCTS = true;
        config.mIsOEM = true;

        mIotGateway.tuyaIotStart(this, config);
        Log.v(TAG, "onStartClick over");

        findViewById(R.id.button_start).setEnabled(false);
        findViewById(R.id.button_test).setEnabled(false);

        findViewById(R.id.button_reset).setEnabled(true);
        findViewById(R.id.speach_test).setEnabled(true);
    }

    TuyaIotGateway.GatewayListener mGatewayListener  = new TuyaIotGateway.GatewayListener() {
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
            // 重启设备或者重启进程，注意如果重启进程，请确保底层网关库会 停用zigbee 子进程。
            finish();
        }

        @Override
        public void onDataPointCommand(int type, int dttType, String cid, String multicastId,
                                       TuyaIotGateway.DataPoint[] dataPoint) {
            Log.v(TAG, "onDataPointCommand " + type);
            Log.v(TAG, "type:" + type + ", dttType:" + dttType + ", cid:" + cid + ", multicast Id:" + multicastId);
            for (int i = 0; i < dataPoint.length; i++) {
                dumpDataPoint(dataPoint[i]);
            }
        }

        @Override
        public void onNetworkStatus(int status) {
            Log.i(TAG, "onNetworkStatus " + status);
            if (status == TuyaIotGateway.GatewayListener.NETWORK_STATUS_CLOUD_CONNECTED) {
                Log.v(TAG, "connected to cloud");

                String id = mIotGateway.tuyaIotGetId();
                Log.d(TAG, "gateway id is " + id);
            } else if (status == TuyaIotGateway.GatewayListener.NETWORK_STATUS_LAN_CONNECTED) {
                Log.v(TAG, "network connected");
            } else {
            }
        }

        @Override
        public void onCloudMedia(TuyaIotGateway.MediaAttribute[] mediaAttributes) {
            Log.v(TAG, "onCloudMedia");
            for (TuyaIotGateway.MediaAttribute mediaAttribute : mediaAttributes) {
                dumpMediaAttribute(mediaAttribute);
            }
        }

        @Override
        public String onGetIP() {
            String ipAddress = getLocalIpAddress();
//                Log.v(TAG, "onGetIP " + ipAddress);
            return ipAddress;
        }
        @Override
        public void onStartSuccess() {
            Log.v(TAG, "onStartSuccess");
            getToken();

            // start成功后抓当前进程和子进程的日志。
//                mLogDaemon.setExpectation(android.os.Process.myPid(),  null, true);
//                mLogDaemon.start();
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
            // zigbee 服务挂了，重启app或者重启设备
            Log.d(TAG, "onZigbeeServiceDied");
            doRebootApplicaion();
        }

        @Override
        public void onZigbeeError() {
            //zigbee 有问题，找硬件同事瞅瞅, app上提示一下。
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "zigbee 出错，请检查设备", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private void setupLog() {
        String logPath = getExternalFilesDir("log").getAbsolutePath();
        //logPath/gateawy.log
        mLogDaemon = new LogDaemon(logPath, 10, 3 * 1024, "gateway");

        // 用法1. 收集当前进程和子进程的日志，每个进程一份日志，对于网关还说，tuyaIotStart之后底层库起了一个新进程，所以
        // mLogDaemon.start() 应该在tuyaIotStart的回调成功之后再调用，但是那时候再start会丢失应用启动的一部分日志。
        // mLogDaemon.setExpectation(android.os.Process.myPid(), null, false);

        // 用法2. 收集当前进程中，tag为tuya或者ty_zb的log，一份日志。
        // mLogDaemon.setExpectation(android.os.Process.myPid(), "tuya,ty_zb", false);

        // 用法3. 收集当前进程和子进程以及tag为tuya或者ty_zb的log，每个进程一份日志，外加tag一份日志。
        // 这个畸形涉及为了解决 用法1中的缺陷，启动后可以抓到全部日志。
        // mLogDaemon.setExpectation(android.os.Process.myPid(), "tuya,ty_zb", true);

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
            mIotGateway.tuyaIotReset();
        } else if(v == findViewById(R.id.button_reboot)) {
            Log.v(TAG, "onRebootClick");
            doReboot();
        } else if(v == findViewById(R.id.button_getlog)) {
            Log.v(TAG, "onSignOutClick");
            Log.d(TAG, "id: " + mIotGateway.tuyaIotGetId());
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

        mIotGateway.confirmUpgradeInstall();
//        UpgradeHelper helper = new UpgradeHelper(this, "", "", null);
//        helper.upgradeROM("1598609170", "/sdcard/update.zip");
    }

    private void getToken() {
        String token = ""; //获取token
        Log.v(TAG, "token is " + token);
        mIotGateway.tuyaIotBindToken(token);
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

    private void dumpDataPoint(TuyaIotGateway.DataPoint dataPoint) {
        Log.d(TAG, "id         :" + dataPoint.mId);
        Log.d(TAG, "type       :" + dataPoint.mType);
        Log.d(TAG, "timestamp  :" + dataPoint.mTimeStamp);
        Log.d(TAG, "data:" + dataPoint.mData);
        if(dataPoint.mType == TuyaIotGateway.DataPoint.TYPE_BOOL) {
            Log.d(TAG, "bool       :" + dataPoint.mData );
        } else if(dataPoint.mType == TuyaIotGateway.DataPoint.TYPE_STRING) {
            Log.d(TAG, "string     :" + dataPoint.mData);
        } else {
            Log.d(TAG, "value      :" + dataPoint.mData + "(" + dataPoint.mData + ")");
        }
    }

    private void dumpMediaAttribute(TuyaIotGateway.MediaAttribute attribute) {
        if (attribute == null) {
            return;
        }
        Log.d(TAG, "mId             :" + attribute.mId);
        Log.d(TAG, "mDecodeType     :" + attribute.mDecodeType);
        Log.d(TAG, "mLength         :" + attribute.mLength);
        Log.d(TAG, "mDuration       :" + attribute.mDuration);
        Log.d(TAG, "mMediaType      :" + attribute.mMediaType);
        Log.d(TAG, "mUrl            :" + attribute.mUrl);
        Log.d(TAG, "mFollowAction   :" + attribute.mFollowAction);
        Log.d(TAG, "mSessionId      :" + attribute.mSessionId);
        Log.d(TAG, "mHttpMethod     :" + attribute.mHttpMethod);
        Log.d(TAG, "mRequestBody    :" + attribute.mRequestBody);
        Log.d(TAG, "mTaskType       :" + attribute.mTaskType);
        Log.d(TAG, "mCallbackValue  :" + attribute.mCallbackValue);
        Log.d(TAG, "mPhoneNumber    :" + attribute.mPhoneNumber);
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
