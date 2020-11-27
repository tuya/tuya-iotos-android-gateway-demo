package com.tuya.smartpad.demo;

import android.content.Context;
import android.util.Log;

import com.tuya.testsuit.ZigbeeTestSuit;

public class ZigbeeTest {
    private static final  String TAG = "ZigbeeTest";
    private Context mContext;
    public ZigbeeTest(Context context) {
        mContext = context;
    }

    private String getTestString(int id) {
        if(id == ZigbeeTestSuit.TEST_OK) {
            return "test success";
        } else if(id == ZigbeeTestSuit.TEST_VERSION_FAILED) {
            return "failed to get version";
        } else if(id == ZigbeeTestSuit.TEST_SEND_FAILED) {
            return "send failed";
        } else if(id == ZigbeeTestSuit.TEST_RECV_FAILED) {
            return "recv failed";
        } else if(id == ZigbeeTestSuit.TEST_START_FAILED) {
            return "start service failed";
        } else if (id == ZigbeeTestSuit.TEST_PARAM_ERROR) {
            return "param invalid";
        }
        return "unkown error";
    }
    public interface OnTestCompletion {
        void onTestResult(String result);
    }
    private OnTestCompletion mListener = null;
    public void setOnTestCompletion(OnTestCompletion listener) {
        mListener = listener;
    }

    public void zigbeeTest() {
        Log.d(TAG, "zigbeeTest");
        ZigbeeTestSuit zigbeeTest = new ZigbeeTestSuit();
        zigbeeTest.setOnTestCompletion(new ZigbeeTestSuit.OnTestCompletion(){
            @Override
            public void onTestResult(final int result) {
                Log.d(TAG, "result " + getTestString(result));
                if(mListener != null) {
                    mListener.onTestResult(getTestString(result));
                }
                //do something
            }
        });

        String filedirs = mContext.getFilesDir().getAbsolutePath();
        Log.i(TAG, "file dir:" + filedirs);
        ZigbeeTestSuit.Config config = new ZigbeeTestSuit.Config();
        config.mPath = filedirs;//make sure directory is created and permissions of writting and reading are granted
        config.mProductKey = "firmwarekey";//firmware key or product id
        config.mUUID = "uuid";//uuid
        config.mAuthKey = "authkey";// authkey
        config.mVersion = "1.0.0";//software version
        config.mSerialPort = "/dev/ttyS5";//uart used communicating with zigbee module
        config.mTempDir = filedirs;//make sure directory is created and permissions of writting and reading are granted。
        config.mBinDir = filedirs;//make sure directory is created and permissions of writting and reading are granted
        config.mIsCTS = true; //
        config.mIsOEM = true;

        config.mZigbeeChannel = 12; //channel, stay same with zigbee dongle
        config.mPackageCount = 20;  //pakcages sent during test。
        config.mTestTime = 10; //how munch time the test will last by second。
        Log.i(TAG, "bin dir:" + config.mBinDir + ", file dir:" + filedirs);

        zigbeeTest.tuyaZigbeeTest(config);
    }

}
