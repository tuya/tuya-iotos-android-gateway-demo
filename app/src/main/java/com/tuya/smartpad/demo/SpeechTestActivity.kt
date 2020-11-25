package com.tuya.smart.android.demo.speech

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.tuya.smart.iotgateway.gateway.TuyaIotGateway
import com.tuya.smart.iotgateway.speech.OnSpeechCallback
import com.tuya.smart.iotgateway.speech.SpeechHelper
import com.tuya.smartpad.demo.R
import kotlinx.android.synthetic.main.activity_speach_test.*

class SpeechTestActivity : Activity() {
    var helper: SpeechHelper? = null

    companion object {
        val TAG: String = "SpeechTestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speach_test)

        init()
    }

    private fun init() {
        try {
            helper = SpeechHelper(this, "/sdcard/tuya_speech_config/", object : OnSpeechCallback {

                override fun onDeInitComplete() {
                    TODO("语音助手关闭成功")
                }

                override fun onInitComplete() {
                    TODO("语音助手开启成功")
                }

                override fun onDeInitError(errMsg: String) {
                    TODO("语音助手关闭报错：errMsg 错误信息")
                }

                override fun onCommand(command: String, data: String) {
                    TODO("收到离线命令： command 命令名称；data：命令数据")
                }

                override fun getCommands(): Array<String> {
                    TODO("离线命令注册")
                }

                override fun onInitError(errMsg: String) {
                    TODO("开启失败：errMsg 错误信息")
                }

                override fun onASRError(errMsg: String) {
                    TODO("识别错误：errMsg 错误信息")
                }

                override fun onPermissionDenied() {
                    TODO("权限不足")
                }

                override fun onStartListening() {
                    Log.d(TAG, "onStartListening");
                    TODO("正在聆听语音")
                }

                override fun onWakeup(): Boolean {
                    Log.d(TAG, "onWakeup");
                    TODO("是否拦截唤醒后的操作：true 拦截 false不拦截；使用场景，根据需求动态控制语音响应，开启拦截语音唤醒后无响应")
                }

                override fun onSpeechBeginning(errCode: Int) {
                    TODO("Not yet implemented")
                }

                override fun onSpeechEnd(errCode: Int) {
                    TODO("Not yet implemented")
                }

                override fun onResponse(success: Boolean, isDialog: Boolean, audioPath: ArrayList<String>?) {
                    TODO("收到云端回复： success 是否成功回复；audioPath 回复音频文件的路径")
                }
            }, 10000, "onlyonekey")
        } catch (e: Exception) {
            Log.e(TAG, e.message)
            return
        }

        helper?.start()

        TuyaIotGateway.getInstance().setSpeechHandler(helper?.getHandler())
    }

    override fun onDestroy() {
        super.onDestroy()

        helper?.stop()
    }

    private fun setText(text: String) {
        runOnUiThread {
            text_view.text = text
        }
    }
}
