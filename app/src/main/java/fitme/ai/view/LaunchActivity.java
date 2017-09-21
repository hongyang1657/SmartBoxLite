package fitme.ai.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import fitme.ai.MyApplication;
import fitme.ai.R;
import fitme.ai.utils.L;
import fitme.ai.utils.NetworkStateUtil;
import fitme.ai.utils.WordsToVoice;

/**
 * Created by hongy on 2017/9/18.
 */

public class LaunchActivity extends Activity{

    private MyApplication application;
    private Intent intent;
    private Thread thread;
    private boolean tomain = true;

    //讯飞TTS
    private WordsToVoice wordsToVoice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        initAndroidFunctionState();
        init();
    }

    private void init(){

        /*thread = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    sleep(5000);
                    if (tomain){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                intent = new Intent(LaunchActivity.this,MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();*/
    }

    public void launchClick(View v){
        switch (v.getId()){
            case R.id.bt_to_main:
                tomain = false;
                intent = new Intent(this,MainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.bt_to_config:
                tomain = false;
                intent = new Intent(this,ConfigActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    private void initAndroidFunctionState(){
        //判断网络状态
        if (!NetworkStateUtil.isNetworkAvailable(this)){
            L.i("当前没网络");
            wordsToVoice = new WordsToVoice(LaunchActivity.this);
            wordsToVoice.startSynthesizer("未连接网络",null);
            @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            if(wifiManager != null && wifiManager.getWifiState()==wifiManager.WIFI_STATE_DISABLED){


                /*L.i("开启wifi");
                wifiManager.setWifiEnabled(true);*/

                /*new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        while (isWifiClose){
                            try {
                                sleep(1000);
                                if (wifiManager.getWifiState()==wifiManager.WIFI_STATE_ENABLED){
                                    isWifiClose = false;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        *//*L.i("连接指定的网络");
                        WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager(wifiManager);
                        wifiAutoConnectManager.connect("FITME-GUEST","28230152", WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);*//*
                    }
                }.start();*/
            }else if (wifiManager != null && wifiManager.getWifiState()==wifiManager.WIFI_STATE_ENABLED){
                /*L.i("连接指定的网络");
                WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager(wifiManager);
                wifiAutoConnectManager.connect("FITME-GUEST","28230152", WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);*/
            }
        }else {
            //有网络,初始化其他组件
            L.i("有网络");
            wordsToVoice = new WordsToVoice(LaunchActivity.this);
            wordsToVoice.startSynthesizer("欢迎回来，想我了吗？",null);
        }


    }
}
