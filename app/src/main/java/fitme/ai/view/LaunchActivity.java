package fitme.ai.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import fitme.ai.MyApplication;
import fitme.ai.R;

/**
 * Created by hongy on 2017/9/18.
 */

public class LaunchActivity extends Activity{

    private MyApplication application;
    private Intent intent;
    private Thread thread;
    private boolean tomain = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        init();
    }

    private void init(){
        thread = new Thread(){
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
        thread.start();
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
}
