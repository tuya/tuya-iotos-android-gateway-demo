package com.tuya.smartpad.demo;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


public class PermissionUtils implements  ActivityCompat.OnRequestPermissionsResultCallback {
    private  static final String TAG = "PermissionUtils";


    public static boolean mAccessStorageGranted = false;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    private static OnRequestPermissionsResult mOnRequestPermissionsResult = null;
    public static void setOnRequestPermissionsResult(OnRequestPermissionsResult onRequestPermissionsResult) {
        mOnRequestPermissionsResult = onRequestPermissionsResult;
    }
    public static void verifyStoragePermissions(Activity activity) {

        Log.d(TAG, "verifyStoragePermissions");
        try {
            //check if write permission is granted
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            Log.d(TAG, "permission:" + permission +", " + PackageManager.PERMISSION_GRANTED);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                // write permission is not granted, show a dialog to request for it
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            } else {
                mAccessStorageGranted = true;
                if(mOnRequestPermissionsResult != null) {
                    mOnRequestPermissionsResult.onRequestPermissionsResult(mAccessStorageGranted);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(REQUEST_EXTERNAL_STORAGE == requestCode) {
            boolean granted = true;
            for(int i = 0; i < permissions.length; i ++) {
                if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    granted = false;
                }
            }
            mAccessStorageGranted = granted;
            Log.d(TAG, "permission " + (granted ? " granted " : " not granted "));
        }
        if(mOnRequestPermissionsResult != null) {
            mOnRequestPermissionsResult.onRequestPermissionsResult(mAccessStorageGranted);
        }
    }
}

interface OnRequestPermissionsResult{
    void onRequestPermissionsResult(boolean result);
}

