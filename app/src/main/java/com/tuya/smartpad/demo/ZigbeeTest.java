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
            return "测试成功";
        } else if(id == ZigbeeTestSuit.TEST_VERSION_FAILED) {
            return "获取版本失败";
        } else if(id == ZigbeeTestSuit.TEST_SEND_FAILED) {
            return "发送失败";
        } else if(id == ZigbeeTestSuit.TEST_RECV_FAILED) {
            return "接收失败";
        } else if(id == ZigbeeTestSuit.TEST_START_FAILED) {
            return "网关启动失败";
        } else if (id == ZigbeeTestSuit.TEST_PARAM_ERROR) {
            return "参数错误";
        }
        return "未知错误";
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
        //启动大概要将近3秒时间，因此设计成异步的，测试结果在回调中返回。
        //启动之后在10s内发送接收zigbee数据包，如果能发送能接收就判定功能OK，否则测试不过。

        String filedirs = mContext.getFilesDir().getAbsolutePath();
        Log.i(TAG, "file dir:" + filedirs);
        ZigbeeTestSuit.Config config = new ZigbeeTestSuit.Config();
        config.mPath = filedirs;//路径保证可读写，测试可能不用，但是初始化复用了产品代码，可能会做检测。
        config.mProductKey = "keyuqugcq8rrtadw";//按实际填写
        config.mUUID = "tuyae2f120f1963c5482";//修改成设备uuid
        config.mAuthKey = "QakBUcyw4le9hetp3K6CfKIhgInYfji2";//修改成设备authkey
        config.mVersion = "1.0.0";//按实际填写
        config.mSerialPort = "/dev/ttyS5";//按实际接线修改一下。
        config.mTempDir = filedirs;//路径保证可读写，测试可能不用，但是初始化复用了产品代码，可能会做检测。
        config.mBinDir = filedirs;//路径保证可读写，测试可能不用，但是初始化复用了产品代码，可能会做检测。
        config.mIsCTS = true; //带流控设置成true，否则false。
        config.mIsOEM = true;

        config.mZigbeeChannel = 12; //信道，和zigbee dongle保持一致
        config.mPackageCount = 20;  //发送多少个测试包。
        config.mTestTime = 10; //测试时长,按秒计算。
        Log.i(TAG, "bin dir:" + config.mBinDir + ", file dir:" + filedirs);

        zigbeeTest.tuyaZigbeeTest(config);
    }

}
