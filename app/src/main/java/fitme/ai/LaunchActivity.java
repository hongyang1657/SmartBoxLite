package fitme.ai;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by hongy on 2017/9/18.
 */

public class LaunchActivity extends Activity{

    private MyApplication application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        init();
    }

    private void init(){

    }
}
