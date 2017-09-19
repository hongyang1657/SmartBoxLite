package fitme.ai.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.gson.Gson;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SynthesizerListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.sai.commnpkg.DirectorBaseMsg;
import org.sai.commnpkg.saiAPI_wrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import fitme.ai.MyApplication;
import fitme.ai.R;
import fitme.ai.Tools;
import fitme.ai.bean.MessageGet;
import fitme.ai.bean.Music;
import fitme.ai.model.BLControlConstants;
import fitme.ai.model.BLControl;
import fitme.ai.model.YeelightControl;
import fitme.ai.service.MusicPlayerService;
import fitme.ai.setting.api.ApiManager;
import fitme.ai.utils.L;
import fitme.ai.utils.Mac;
import fitme.ai.utils.NetworkStateUtil;
import fitme.ai.utils.SignAndEncrypt;
import fitme.ai.utils.WordsToVoice;
import fitme.ai.view.impl.IGetYeelight;
import okhttp3.ResponseBody;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity implements IGetYeelight {

    //UI相关
    private ImageView ivLight1,ivLight2,ivLight3,ivLight4,ivLight5,ivLight6,ivLight7
            ,ivLight8,ivLight9,ivLight10,ivLight11,ivLight12;
    private List<ImageView> ivLightList = new LinkedList<>();
    private Button bt1,bt2,bt3,bt4;

    private static final int TIMER = 1;   //跑马灯计时器
    private static final int CLEAR_ALL = 2;  //熄灭所有灯
    private int timer = 0;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case TIMER:
                    lightUp(timer);
                    Log.i("result", "handleMessage: "+timer);
                    if (timer==12){
                        timer = 0;
                    }
                    break;
                case CLEAR_ALL:
                    clearAllLight();
                    break;
                default:
                    break;
            }
        }
    };


    static {
        System.loadLibrary("saiAPIs");
    }

    private static final String TAG = "hy_debug_message";

    //private static final String result_path = "/sdcard/sai_config/";
    //private File asr_result = null;

    private saiAPI_wrap mSaiSDK;
    private TextView mWkpCountTxv;
    private SaiCallback mSaiCallback;
    private TextView mAsrTxv;
    private TextView mVersionTxv;

    //讯飞TTS
    private WordsToVoice wordsToVoice;
    private MyApplication app;

    //播放短音效
    private SoundPool soundPool;
    private int soundid;

    //博联控制类
    private BLControl blControl;
    private AudioManager mAudioManager;
    private int maxVolume;
    private int currentVolume;

    private int wkp_count = 0;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            int what = message.what;
            switch (what) {
                case 0:
                    //获取ASR
                    String asr = message.obj.toString();
                    mAsrTxv.setText(asr);

                    break;
                case 1:
                    //唤醒成功
                    float angle = (float) message.obj;
                    int a = (int) angle;
                    L.i("唤醒角度："+angle);
                    wakeUpByAngle(a);
                    handler.sendEmptyMessageDelayed(CLEAR_ALL,3000);
                    mAsrTxv.setText("");
                    //播放唤醒声
                    soundPool.play(soundid, 1.0f, 1.0f, 0, 0, 1.0f);
                    //mWkpCountTxv.setText(message.arg1 + "");
                    break;
                case 2:

                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initSaiSDK(this);
        wordsToVoice = new WordsToVoice(MainActivity.this);
        app = (MyApplication) getApplication();
        //new BLControl.LoginWithoutNameTask(app).execute();    //初始化博联
        YeelightControl.getInstance(app,this).searchDevice();    //初始化Yeelight
        //绑定设备
        bindDevice();

        //初始化短音效
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        soundid = soundPool.load(MainActivity.this, R.raw.siri, 1);
        //音量控制,初始化定义
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //最大音量
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //当前音量
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mHandler.sendEmptyMessageDelayed(2,10000);
        //asr_result = new File(result_path + "result_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".txt");
    }

    private void initSaiSDK(Context context) {
        Tools.copyAssets2Sdcard(context);
        mSaiSDK = new saiAPI_wrap();
        mSaiCallback = new SaiCallback();
        int initCode = mSaiSDK.init_system(0.47, "/sdcard/sai_config", mSaiCallback);
        int startCode = mSaiSDK.start_service(1, 6000);
        Log.d(TAG, "initSaiSDK: " + initCode + "/" + startCode);
        mVersionTxv.setText(mSaiSDK.get_version());
        Log.d(TAG, "initSaiSDK: version = " + mSaiSDK.get_version());
    }

    private void initUI() {
        mAsrTxv = (TextView) findViewById(R.id.asr_txv);
        mVersionTxv = (TextView) findViewById(R.id.version_txv);
        mWkpCountTxv = (TextView) findViewById(R.id.wkp_count_txv);
        ivLight1 = (ImageView) findViewById(R.id.iv_light_1);
        ivLight2 = (ImageView) findViewById(R.id.iv_light_2);
        ivLight3 = (ImageView) findViewById(R.id.iv_light_3);
        ivLight4 = (ImageView) findViewById(R.id.iv_light_4);
        ivLight5 = (ImageView) findViewById(R.id.iv_light_5);
        ivLight6 = (ImageView) findViewById(R.id.iv_light_6);
        ivLight7 = (ImageView) findViewById(R.id.iv_light_7);
        ivLight8 = (ImageView) findViewById(R.id.iv_light_8);
        ivLight9 = (ImageView) findViewById(R.id.iv_light_9);
        ivLight10 = (ImageView) findViewById(R.id.iv_light_10);
        ivLight11 = (ImageView) findViewById(R.id.iv_light_11);
        ivLight12 = (ImageView) findViewById(R.id.iv_light_12);
        ivLightList.add(ivLight1);
        ivLightList.add(ivLight2);
        ivLightList.add(ivLight3);
        ivLightList.add(ivLight4);
        ivLightList.add(ivLight5);
        ivLightList.add(ivLight6);
        ivLightList.add(ivLight7);
        ivLightList.add(ivLight8);
        ivLightList.add(ivLight9);
        ivLightList.add(ivLight10);
        ivLightList.add(ivLight11);
        ivLightList.add(ivLight12);

        bt1 = (Button) findViewById(R.id.bt_1);
        bt2 = (Button) findViewById(R.id.bt_2);
        bt3 = (Button) findViewById(R.id.bt_3);
        bt4 = (Button) findViewById(R.id.bt_4);
    }

    /**
          UI相关
     *
     */
    public void click(View view){
        switch (view.getId()){
            case R.id.bt_1:
                powerOn();
                break;
            case R.id.bt_2:
                wakeUpByAngle(30);
                handler.sendEmptyMessageDelayed(CLEAR_ALL,3000);
                break;
            case R.id.bt_3:
                break;
            case R.id.bt_4:
                Intent intent=new Intent(Intent.ACTION_REBOOT);
                intent.putExtra("nowait", 1);
                intent.putExtra("interval", 1);
                intent.putExtra("window", 0);
                sendBroadcast(intent);
                break;
            default:
                break;
        }
    }

    private void powerOn(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                while (timer<12){
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessage(TIMER);
                    timer++;
                }
            }
        }.start();
    }

    //根据唤醒的角度亮灯
    private void wakeUpByAngle(int angle){
        if (angle>=0&&angle<30){
            ivLight1.setImageResource(R.drawable.cycler_shape_green);
            ivLight2.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=30&&angle<60){
            ivLight2.setImageResource(R.drawable.cycler_shape_green);
            ivLight3.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=60&&angle<90){
            ivLight3.setImageResource(R.drawable.cycler_shape_green);
            ivLight4.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=90&&angle<120){
            ivLight4.setImageResource(R.drawable.cycler_shape_green);
            ivLight5.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=120&&angle<150){
            ivLight5.setImageResource(R.drawable.cycler_shape_green);
            ivLight6.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=150&&angle<180){
            ivLight6.setImageResource(R.drawable.cycler_shape_green);
            ivLight7.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=180&&angle<210){
            ivLight7.setImageResource(R.drawable.cycler_shape_green);
            ivLight8.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=210&&angle<240){
            ivLight8.setImageResource(R.drawable.cycler_shape_green);
            ivLight9.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=240&&angle<270){
            ivLight9.setImageResource(R.drawable.cycler_shape_green);
            ivLight10.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=270&&angle<300){
            ivLight10.setImageResource(R.drawable.cycler_shape_green);
            ivLight11.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=300&&angle<330){
            ivLight11.setImageResource(R.drawable.cycler_shape_green);
            ivLight12.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=330&&angle<=360){
            ivLight12.setImageResource(R.drawable.cycler_shape_green);
            ivLight1.setImageResource(R.drawable.cycler_shape_green);
        }
    }

    //熄灭所有灯
    private void clearAllLight(){
        for (int i=0;i<ivLightList.size();i++){
            ivLightList.get(i).setImageResource(R.drawable.cycler_shape_white);
        }
    }

    //点亮某一灯
    private void lightUp(int index){
        for (int i=0;i<ivLightList.size();i++){
            if (index==i+1){
                ivLightList.get(i).setImageResource(R.drawable.cycler_shape_blue);
            }else{
                ivLightList.get(i).setImageResource(R.drawable.cycler_shape_white);
            }

        }
    }


    private void saveData2Local(File file, byte[] buffer) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
            fos.write(buffer, 0, buffer.length);
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Yeelight回调
     * @param devices
     */

    @Override
    public void getDevices(List<HashMap<String, String>> devices) {

    }

    @Override
    public void getResponse(String response) {

    }


    class SaiCallback extends DirectorBaseMsg {

        @Override
        public void outter_wakeup_now(int wakeup_result, float angle, String key_word) {
            Log.d(TAG, "outter_wakeup_now: angle = " + angle);
            wkp_count++;
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = angle;
            msg.arg1 = wkp_count;
            mHandler.sendMessageDelayed(msg, 5);
        }

        @Override
        public void outter_get_asr(String asr_rslt) {
            Log.d(TAG, "outter_get_asr: asr = " + asr_rslt);
            try {
                JSONObject root = new JSONObject(asr_rslt);
                String asr = root.getString("asr");
                String id = root.getString("id");
                Log.d(TAG, "outter_get_asr: asr = " + asr);
                Log.d(TAG, "outter_get_asr: id = " + id);
                //saveData2Local(asr_result, (id + "  " + asr + "\n").getBytes());

                //发送消息请求
                //messageCreat(Mac.getMac(),String.valueOf(1200020190),String.valueOf(302902090),"device_text",asr,"13145");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = asr_rslt;
            mHandler.sendMessageDelayed(msg, 5);
        }

        @Override
        public void outter_get_status(int status_code, String message) {
            Log.d(TAG, "outter_get_status: status_code = " + status_code);
        }

        @Override
        public void outter_get_vad_status(int vad_status) {
            Log.d(TAG, "outter_get_vad_status: vad_status = " + vad_status);
        }
    }


    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
            L.i("语音合成回调监听-----------"+"开始播放");
            playingmusic(MusicPlayerService.REDUCE_MUSIC_VOLUME,"");
        }

        @Override
        public void onSpeakPaused() {
            // showTip("暂停播放");
            L.i("语音合成回调监听-----------"+"暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            //showTip("继续播放");
            L.i("语音合成回调监听-----------"+"继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                L.i("语音合成回调监听-----------"+"播放完成");
                playingmusic(MusicPlayerService.RECOVER_MUSIC_VOLUME,"");   //恢复音乐音量
            } else if (error != null) {
                //showTip(error.getPlainDescription(true));
                L.i("语音合成回调监听-------错误----"+error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    //发送客户消息请求
    private void messageCreat(String userId, String x, String y, final String messageType, String content, String password){
        String timeStamp = SignAndEncrypt.getTimeStamp();

        Gson gson = new Gson();
        HashMap<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("x", x);
        params.put("y", y);
        params.put("message_type", messageType);
        HashMap<String, Object> map = new HashMap<>();
        map.put("content", content);
        params.put("message_body", map);
        params.put("password",password);
        L.i("---------发出的json-"+gson.toJson(params));

        LinkedHashMap<String, Object> par = new LinkedHashMap<>();
        par.put("method", "message/from_customer/create");
        par.put("api_key", ApiManager.api_key);
        par.put("timestamp", timeStamp);
        par.put("http_body", gson.toJson(params));
        String sign = SignAndEncrypt.signRequest(par, ApiManager.api_secret);
        ApiManager.fitmeApiService.messageCreateVB(ApiManager.api_key, timeStamp, sign,params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<MessageGet>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("错误信息："+e.toString());
                        wordsToVoice.startSynthesizer("小秘正在开小差",mTtsListener);

                    }
                    @Override
                    public void onNext(MessageGet messageGet) {
                        /*try {
                            L.logE("json:"+messageGet.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/

                        L.logE("收到回复的消息:"+new Gson().toJson(messageGet));
                        mWkpCountTxv.setText(new Gson().toJson(messageGet));
                        //成功收到回复的消息
                        if (null!=messageGet.getStatus()&&"success".equals(messageGet.getStatus())){
                            L.logE("成功收到回复的消息");
                            if ("text".equals(messageGet.getMessages()[0].getMessage_type())){        //单句回复
                                L.logE("单句回复");
                                wordsToVoice.startSynthesizer(messageGet.getMessages()[0].getMessage_body().getContent(),mTtsListener);
                            } else if ("multiline_text".equals(messageGet.getMessages()[0].getMessage_type())){         //多句回复
                                L.logE("多句回复");
                                doMultilineText(messageGet.getMessages()[0].getMessage_body().getContents());
                            }else if ("task_result".equals(messageGet.getMessages()[0].getMessage_type())){        //控制命令或音乐
                                L.logE("控制命令或音乐");

                                if ("query_music".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){     //查询音乐
                                    L.logE("查询音乐");
                                    String speechText = messageGet.getMessages()[0].getMessage_body().getTask_result_speech_text();
                                    int musicsNum = messageGet.getMessages()[0].getMessage_body().getTask_result_body().getMusics().size();
                                    musicList = new LinkedList<Music>();
                                    for (int i=0;i<musicsNum;i++){
                                        musicList.add(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getMusics().get(i));
                                    }
                                    //初始化歌单
                                    initMusicList(musicList);
                                    playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
                                    isPlayingMusic = true;
                                }else if ("command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    //控制
                                    L.logE("控制");
                                    playingmusic(MusicPlayerService.RECOVER_MUSIC_VOLUME,"");   //恢复音乐音量

                                    int devicesLength = messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().size();
                                    for (int i=0;i<devicesLength;i++){
                                        String deviceType = messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getDevice_type();
                                        if ("20045".equals(deviceType)){  //杜亚窗帘
                                            L.logE("杜亚窗帘");
                                            blControl.dnaControlSet("curtain",messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getCommand_code(),"curtain_work");
                                            //blControl.curtainControl(Integer.parseInt(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand_code()));
                                        }else if ("10026".equals(deviceType) || "10039".equals(deviceType)){     //RM红外遥控
                                            L.logE("RM红外遥控");
                                            blControl.commandRedCodeDevice(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getCommand_code(),
                                                    messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getDid());
                                        }else if ("30014".equals(deviceType)){     //SP系列wifi开关
                                            blControl.dnaControlSet("sp","1","val");
                                        }else if ("20149".equals(deviceType)){        //四位排插

                                        }
                                    }

                                }else if ("music_command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    commandMusicPlayer(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand());
                                }else if ("tv_command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    //控制电视播放
                                    wordsToVoice.startSynthesizer("正在电视设备上为您播放："+messageGet.getMessages()[0].getMessage_body().getTask_result_body().getFilm_name(),mTtsListener);
                                }else if ("box_command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    if ("next_page".equals(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand())){
                                        //下一页
                                        L.i("电视盒子下一页");
                                        blControl.commandRedCodeDevice(BLControlConstants.TV_BOX_NEXT_PAGE, BLControlConstants.RM_MINI_DID);
                                    }else if ("prev_page".equals(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand())){
                                        //上一页
                                        L.i("电视盒子上一页");
                                        blControl.commandRedCodeDevice(BLControlConstants.TV_BOX_PRE_PAGE, BLControlConstants.RM_MINI_DID);
                                    }
                                }
                            }
                        }


                    }
                });
    }

    //发送指令到音乐播放的service
    private List<Music> musicList;
    private boolean isPlayingMusic;
    private void playingmusic(int type,String songUrl) {
        //判断是否放新一曲
        if (type== MusicPlayerService.PLAT_MUSIC||type== MusicPlayerService.NEXT_MUSIC){
            String strMusicInfo = musicList.get(currentPlaySongIndex).getSinger()+","+musicList.get(currentPlaySongIndex).getName();
            wordsToVoice.startSynthesizer("正在为您播放："+strMusicInfo,mTtsListener);
        }
        //启动服务，播放音乐
        Intent intent = new Intent(this,MusicPlayerService.class);
        intent.putExtra("type",type);
        intent.putExtra("songUrl",songUrl);
        startService(intent);
    }

    //初始化音乐列表
    private List<String> musicUrl = null;
    private int musicListSize = 0;
    private int currentPlaySongIndex = 0;
    private void initMusicList(List<Music> musicList){
        musicUrl = new LinkedList<>();
        musicListSize = musicList.size();
        currentPlaySongIndex = 0;     //当前播放的歌曲在歌单中的位置
        for (int i=0;i<musicListSize;i++){
            musicUrl.add(musicList.get(i).getSong_url());
        }
    }

    //处理多个回复
    private void doMultilineText(String[] multiline_text){
        wordsToVoice.startSynthesizer(multiline_text[0],mTtsListener);
        //后续还要处理
    }

    //控制音乐播放器
    private void commandMusicPlayer(String command){
        L.i("控制音乐播放器:"+command);
        switch (command){
            case "next":      //下一曲
                if (currentPlaySongIndex==(musicListSize-1)){
                    currentPlaySongIndex = 0;
                }else {
                    currentPlaySongIndex++;
                }
                Toast.makeText(this, "哪一首："+currentPlaySongIndex, Toast.LENGTH_SHORT).show();
                playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
                break;
            case "prev":      //上一曲
                if (currentPlaySongIndex==0){
                    currentPlaySongIndex = musicListSize-1;
                }else {
                    currentPlaySongIndex--;
                }
                Toast.makeText(this, "哪一首："+currentPlaySongIndex, Toast.LENGTH_SHORT).show();
                playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
                break;
            case "down":
                L.i("音量减");
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Toast.makeText(this, "当前音量"+currentVolume, Toast.LENGTH_SHORT).show();
                break;
            case "up":
                L.i("音量加");
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Toast.makeText(this, "当前音量"+currentVolume, Toast.LENGTH_SHORT).show();
                break;
            case "pause":
                playingmusic(MusicPlayerService.PAUSE_MUSIC,"");
                break;
            case "stop":
                playingmusic(MusicPlayerService.STOP_MUSIC,"");
                break;
            case "play":
                playingmusic(MusicPlayerService.RESUME_MUSIC,"");
                playingmusic(MusicPlayerService.RECOVER_MUSIC_VOLUME,"");   //恢复音乐音量
                break;
            case "max":
                currentVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND);
                break;
            case "mini":
                currentVolume = 0;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND);
                break;
        }
    }


    //绑定设备
    private void bindDevice(){

        String mac = Mac.getMac();
        L.i("mac地址为："+mac+"---------"+NetworkStateUtil.getLocalMacAddressFromWifiInfo(MainActivity.this));

        String timeStamp = SignAndEncrypt.getTimeStamp();
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("method", "account/device/create");
        params.put("api_key", ApiManager.api_key);
        params.put("timestamp", timeStamp);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        List<LinkedHashMap> devices = new ArrayList<>();

        LinkedHashMap<String, Object> mapDevices = new LinkedHashMap<>();
        mapDevices.put("identifier",mac);
        mapDevices.put("did","");
        mapDevices.put("nickname","声智开发板");
        mapDevices.put("pid","");
        mapDevices.put("mac",mac);
        mapDevices.put("device_name","fitmeSound");
        mapDevices.put("device_lock","");
        mapDevices.put("device_type","");
        mapDevices.put("category","");
        mapDevices.put("command","");
        mapDevices.put("command_code","");
        mapDevices.put("user_group","客厅");
        devices.add(mapDevices);

        map.put("user_id", "1067");  //1067
        map.put("devices", devices);

        Gson gson = new Gson();
        params.put("http_body", gson.toJson(map));
        L.i("http_body:"+gson.toJson(map));
        String sign = SignAndEncrypt.signRequest(params, ApiManager.api_secret);
        ApiManager.fitmeApiService.deviceBind(ApiManager.api_key, timeStamp, sign, map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            L.i("服务器回复："+responseBody.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode()==KeyEvent.KEYCODE_BACK){
            finish();
            System.exit(0);
        }
        return super.onKeyDown(keyCode, event);
    }
}
