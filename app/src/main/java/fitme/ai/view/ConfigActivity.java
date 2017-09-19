package fitme.ai.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoiceRecognition;

import fitme.ai.MyApplication;
import fitme.ai.R;
import fitme.ai.utils.L;

/**
 * 配置入网
 * Created by hongy on 2017/9/19.
 */

public class ConfigActivity extends Activity implements SinVoiceRecognition.Listener{

    private MyApplication application;
    //接收音频
    private SinVoiceRecognition mRecognition;
    private final static int[] TOKENS = { 32, 32, 32, 32, 32, 32 };
    private final static int TOKEN_LEN = TOKENS.length;

    static {
        System.loadLibrary("sinvoice");
        LogHelper.d("hy_debug_message", "sinvoice jnicall loadlibrary");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        init();
    }

    private void init(){
        mRecognition = new SinVoiceRecognition();
        mRecognition.init(this);
        mRecognition.setListener(this);
        mRecognition.start(TOKEN_LEN, false);
    }

    /**
     * 声波识别回调
     */
    @Override
    public void onSinVoiceRecognitionStart() {
        L.i("开始识别");
    }

    @Override
    public void onSinVoiceRecognition(char ch) {
        L.i("开始识别");
    }

    @Override
    public void onSinVoiceRecognitionEnd(int result) {
        L.i("结束识别");
    }
}
