package fitme.ai.model;

import android.os.AsyncTask;
import android.os.SystemClock;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import cn.com.broadlink.econtrol.dataparse.BLNetWorkDataParser;
import cn.com.broadlink.econtrol.dataparse.data.A1Info;
import cn.com.broadlink.econtrol.dataparse.data.S1AlarmState;
import cn.com.broadlink.econtrol.dataparse.data.S1SensorAlarmState;
import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.constants.account.BLAccountErrCode;
import cn.com.broadlink.sdk.constants.controller.BLControllerErrCode;
import cn.com.broadlink.sdk.data.controller.BLDNADevice;
import cn.com.broadlink.sdk.data.controller.BLStdData;
import cn.com.broadlink.sdk.interfaces.controller.BLDeviceScanListener;
import cn.com.broadlink.sdk.param.controller.BLStdControlParam;
import cn.com.broadlink.sdk.result.account.BLLoginResult;
import cn.com.broadlink.sdk.result.controller.BLDownloadScriptResult;
import cn.com.broadlink.sdk.result.controller.BLPairResult;
import cn.com.broadlink.sdk.result.controller.BLPassthroughResult;
import cn.com.broadlink.sdk.result.controller.BLProfileStringResult;
import cn.com.broadlink.sdk.result.controller.BLStdControlResult;
import fitme.ai.MyApplication;
import fitme.ai.bean.DeviceBean;
import fitme.ai.utils.L;

/**
 * Created by hongy on 2017/6/23.
 */

public class BLControl {

    private Map blDNADevicesMap = null;
    private BLDNADevice bldnaDevice;

    public BLControl(Map blDNADevicesMap) {
        this.blDNADevicesMap = blDNADevicesMap;
    }

    public BLControl() {
    }

    //免登录
    public static class LoginWithoutNameTask extends AsyncTask<String, Void, BLLoginResult> {

        MyApplication application;

        public LoginWithoutNameTask(MyApplication application){
            this.application = application;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected BLLoginResult doInBackground(String... params) {
            String thirdId = "290900";
            return BLLet.Account.thirdAuth(thirdId);
        }

        @Override
        protected void onPostExecute(BLLoginResult loginResult) {
            super.onPostExecute(loginResult);
            if(loginResult != null && loginResult.getError() == BLAccountErrCode.SUCCESS){
                //保存登录信息
                L.i("登录信息:"+loginResult.getUserid());
                //登录成功后，搜索设备
                searchBLDevices(application);

            }
        }
    }


    private static void searchBLDevices(final MyApplication application){

        //开启设备扫描
        BLLet.Controller.startProbe();

        //回调获取设备信息
        BLLet.Controller.setOnDeviceScanListener(new BLDeviceScanListener() {
            @Override
            public void onDeviceUpdate(BLDNADevice bldnaDevice, boolean b) {


                DeviceBean deviceBean = new DeviceBean();
                deviceBean.setBrand("BroadLink");
                deviceBean.setDid(bldnaDevice.getDid());
                deviceBean.setIdentifier(bldnaDevice.getDid());
                deviceBean.setDevice_type_name(bldnaDevice.getName());
                deviceBean.setNickname(bldnaDevice.getName());
                deviceBean.setMac(bldnaDevice.getMac());
                deviceBean.setPid(bldnaDevice.getPid());
                deviceBean.setDevice_type_number(bldnaDevice.getType()+"");
                deviceBean.setDevice_lock(bldnaDevice.isLock());
                application.setBldnaDevice(deviceBean);


                L.i("设备信息："+new Gson().toJson(bldnaDevice));

                //L.i("设备did："+did+"----对应的设备状态:"+state+"---是否新设备："+b);
                //扫描得到设备后，添加到SDK
                BLLet.Controller.addDevice(bldnaDevice);


                //设备配对，用于获取设备控制密匙
                BLPairResult blPairResult = BLLet.Controller.pair(bldnaDevice);
                //L.i("设备控制密匙:"+blPairResult.getKey()+"---设备控制id"+blPairResult.getId());


                //下载脚本
                BLDownloadScriptResult blDownloadScriptResult = BLLet.Controller.downloadScript(bldnaDevice.getPid());
                String scriptSavePath = blDownloadScriptResult.getSavePath();

                //L.i("-------下载的脚本地址："+scriptSavePath+"下载脚本msg:"+blDownloadScriptResult.getMsg()+blDownloadScriptResult.getStatus());

                //查询设备profile
                BLProfileStringResult blProfileStringResult = BLLet.Controller.queryProfile(bldnaDevice.getDid());
                L.i("查询设备profile:"+blProfileStringResult.getProfile());


            }
        });

        /*new Thread(){
            @Override
            public void run() {
                super.run();
                while (true){
                    //开启设备扫描
                    BLLet.Controller.startProbe();
                    try {
                        sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    L.i("停止设备扫描");
                    //移除所有设备，下次重新搜索
                    BLLet.Controller.removeAllDevice();
                    BLLet.Controller.stopProbe();
                }
            }
        }.start();*/


    }

    //查询设备的网络状态
    private String querydeviceState(String did){
        int state = BLLet.Controller.queryDeviceState(did);
        if (state==0){
            return "还未获取到设备状态";
        }else if (state==1){
            return "设备和手机在同一局域网";
        }else if (state==2){
            return "设备连接到服务器，和手机不在同一局域网";
        }else {
            return "设备未连接服务器，不在线";
        }

    }

    //控制sp设备(wifi开关)
    public static String getSPControlState(String did){
        //控制设备参数
        BLStdData.Value value = new BLStdData.Value();

        ArrayList<BLStdData.Value> pwrVals = new ArrayList<>();
        pwrVals.add(value);

        BLStdControlParam ctrlParam = new BLStdControlParam();
        ctrlParam.setAct("get");
        ctrlParam.getParams().add("pwr");
        ctrlParam.getVals().add(pwrVals);

        if (did!=null){
            final BLStdControlResult blStdControlResult = BLLet.Controller.dnaControl(did, null, ctrlParam);
            L.i("blStdControlResult:"+new Gson().toJson(blStdControlResult));

            return new Gson().toJson(blStdControlResult.getData());
        }else {
            return "";
        }

    }

    //控制杜亚窗帘
    public void curtainControl(int pwr){
        bldnaDevice = (BLDNADevice) blDNADevicesMap.get("curtain");
        //控制设备参数
        BLStdData.Value value = new BLStdData.Value();
        value.setVal(pwr);

        ArrayList<BLStdData.Value> Vals = new ArrayList<>();
        Vals.add(value);

        BLStdControlParam ctrlParam = new BLStdControlParam();
        ctrlParam.setAct("set");
        ctrlParam.getParams().add("curtain_work");
        ctrlParam.getVals().add(Vals);

        if (bldnaDevice!=null){
            L.i("sssssssssssssssss"+bldnaDevice.getName()+"----"+bldnaDevice.getDid());
            final BLStdControlResult blStdControlResult = BLLet.Controller.dnaControl(bldnaDevice.getDid(), null, ctrlParam);
            L.i("blStdControlResult:"+blStdControlResult.getMsg()+"控制错误码："+blStdControlResult.getStatus());
        }else {
            L.i("没有找到该设备");
        }
    }

    //deviceName 设备名称，val 设定的值，param 键名
    public static void dnaControlSet(String did, String val, String param){

        //bldnaDevice = (BLDNADevice) blDNADevicesMap.get(deviceName);
        new DnaControlSetTask(did).execute(param, val);
    }

    //通用DNA控制
    public static class DnaControlSetTask extends AsyncTask<String, Void, BLStdControlResult> {

        private String did;

        public DnaControlSetTask(String did) {
            this.did = did;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected BLStdControlResult doInBackground(String... params) {
            String param = params[0];

            BLStdData.Value value = new BLStdData.Value();
            value.setVal(params[1]);

            ArrayList<BLStdData.Value> dnaVals = new ArrayList<>();
            dnaVals.add(value);

            BLStdControlParam stdControlParam = new BLStdControlParam();
            stdControlParam.setAct("set");
            stdControlParam.getParams().add(param);
            stdControlParam.getVals().add(dnaVals);
            return BLLet.Controller.dnaControl(did, null, stdControlParam);
        }

        @Override
        protected void onPostExecute(BLStdControlResult result) {
            super.onPostExecute(result);
            if(result != null && result.getStatus() == BLControllerErrCode.SUCCESS){
                BLStdData stdData = result.getData();
                L.i("发送dna通用指令成功");
            }
        }
    }


    //透传控制,获取空气检测仪数据
    private static byte[] passthroughControl;
    public static String dnaPassthrough(String did){
        passthroughControl = BLNetWorkDataParser.getInstace().a1RefreshBytes();
        if (did!=null) {
            BLPassthroughResult blPassthroughResult = BLLet.Controller.dnaPassthrough(did, null, passthroughControl);
            L.i("msg" + blPassthroughResult.getMsg() + "data::" + blPassthroughResult.getData());
            A1Info a1Info = BLNetWorkDataParser.getInstace().a1RefreshBytesParse(blPassthroughResult.getData());
            L.i("humidity:" + a1Info.humidity + "---light:" + a1Info.light + "---temperature:" + a1Info.temperature + "---voice:" + a1Info.voice + "---air_condition:" + a1Info.air_condition);

            JSONObject object = new JSONObject();
            try {
                object.put("temperature", a1Info.temperature);
                object.put("humidity", a1Info.humidity);
                object.put("light", a1Info.light);
                object.put("air", a1Info.air_condition);
                object.put("noisy", a1Info.voice);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            L.i("空气检测仪数据:" + object.toString());
            return object.toString();
        }
        return "";
    }

    //获取安防S1的数据
    //List<S1SensorInfo> s1Info = new ArrayList<>();
    public void getSecurityData(){
        bldnaDevice = (BLDNADevice) blDNADevicesMap.get("s1");
        if (bldnaDevice!=null){
            byte[] s1SensorPritect = BLNetWorkDataParser.getInstace().s1QuerySensorPritectMap();
            BLPassthroughResult blPassthroughs1SensorPritect = BLLet.Controller.dnaPassthrough(bldnaDevice.getDid(),null,s1SensorPritect);
            L.i("msg！！！！！！："+blPassthroughs1SensorPritect.getMsg()+"---"+blPassthroughs1SensorPritect.getData());



            byte[] s1bytes = BLNetWorkDataParser.getInstace().s1GetSensorAlarmState();
            BLPassthroughResult blPassthroughResult = BLLet.Controller.dnaPassthrough(bldnaDevice.getDid(),null,s1bytes);
            L.i("msg！！！！！！："+blPassthroughResult.getMsg()+"---"+blPassthroughResult.getData());

            S1AlarmState s1Alarm = BLNetWorkDataParser.getInstace().s1ParseGetSensorAlarmState(blPassthroughResult.getData());
            S1SensorAlarmState s1 = null;
            for (int i=0;i<s1Alarm.getCount();i++){
                s1 = s1Alarm.getStatusList().get(i);
                L.i(i+" :"+s1.getStatus());
            }
        }else {
            L.i("没有找到该设备");
        }

    }

    //通用控制红外，射频设备
    public void commandRedCodeDevice(String strRedCode, String deviceDid){
        new NewSendIrTask(deviceDid).execute(strRedCode);
    }


    //新的发送红外
    public static class NewSendIrTask extends AsyncTask<String, Void, BLStdControlResult> {
        private String deviceDid;
        private SendIrResponse sendIrResponse;

        public void setOnSendIrResponse(SendIrResponse sendIrResponse){
            this.sendIrResponse = sendIrResponse;
        }

        public NewSendIrTask(String deviceDid) {
            this.deviceDid = deviceDid;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected BLStdControlResult doInBackground(String... params) {

            //设置要发送的红外指令
            BLStdData.Value value = new BLStdData.Value();
            value.setVal(params[0]);

            ArrayList<BLStdData.Value> irVals = new ArrayList<>();
            irVals.add(value);

            /**发送学习到的命令**/
            BLStdControlParam intoStudyParam = new BLStdControlParam();
            intoStudyParam.setAct("set");
            intoStudyParam.getParams().add("irda");
            intoStudyParam.getVals().add(irVals);
            return BLLet.Controller.dnaControl(deviceDid, null, intoStudyParam);
        }

        @Override
        protected void onPostExecute(BLStdControlResult stdControlResult) {
            super.onPostExecute(stdControlResult);
            if(stdControlResult != null && stdControlResult.getStatus() == BLControllerErrCode.SUCCESS){
                sendIrResponse.onSendSuccess();
            }else {
                sendIrResponse.onSendFailed();
                L.i("红外吗发射错误："+stdControlResult.getMsg());
            }
        }
    }


    //学习红外
    private static String irCodeStr;
    public static class StudyIrTask extends AsyncTask<Void, Void, BLStdControlResult> {
        private boolean mQueryIr;
        private int mQueryTime = 0;
        private String deviceDid;
        StudyIrResponse studyIrResponse;

        public void setOnStudyIrResponse(StudyIrResponse studyIrResponse){
            this.studyIrResponse = studyIrResponse;
        }

        public StudyIrTask(String deviceDid) {
            this.deviceDid = deviceDid;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected BLStdControlResult doInBackground(Void... params) {
            /**发送 RM 进入学习命令**/
            BLStdControlParam intoStudyParam = new BLStdControlParam();
            intoStudyParam.setAct("set");
            intoStudyParam.getParams().add("irdastudy");
            if (deviceDid!=null){
                BLStdControlResult intoStudyResult = BLLet.Controller.dnaControl(deviceDid, null, intoStudyParam);
                //判断是否进入学习成功
                if(intoStudyResult != null && intoStudyResult.getStatus() == BLAccountErrCode.SUCCESS){
                    mQueryIr = true;

                    while (mQueryIr){
                        //隔间500ms 查询一次，是否学习成功
                        SystemClock.sleep(500);

                        mQueryTime += 500;

                        //超过60s超时，则不再继续查询
                        if(mQueryTime > 60 * 1000){
                            mQueryIr = false;
                            L.i("学习超过60s，停止学习");
                        }

                        /**进入学习成功之后，等待RM学习，查询RM是否学习到红外**/
                        BLStdControlParam queryIrStudyParam = new BLStdControlParam();
                        queryIrStudyParam.setAct("get");
                        queryIrStudyParam.getParams().add("irda");
                        BLStdControlResult queryRtudyResult = BLLet.Controller.dnaControl(deviceDid, null, queryIrStudyParam);
                        if(queryRtudyResult != null && queryRtudyResult.getStatus() == BLAccountErrCode.SUCCESS){
                            mQueryIr = false;
                            return queryRtudyResult;
                        }
                    }
                }
            }else {
                // Toast.makeText(MainActivity.this, "没有找到设备", Toast.LENGTH_SHORT).show();
            }


            return null;
        }

        @Override
        protected void onPostExecute(BLStdControlResult stdControlResult) {
            super.onPostExecute(stdControlResult);
            // progressDialog.dismiss();
            if(stdControlResult != null && stdControlResult.getStatus() == BLAccountErrCode.SUCCESS){

                try{
                    //学习到的红外码
                    irCodeStr = (String) stdControlResult.getData().getVals().get(0).get(0).getVal();
                    L.i("irCodeStr-----"+irCodeStr);
                    //将学习到的红外吗返回
                    studyIrResponse.onStudySuccess(irCodeStr);

                }catch (Exception e){
                    studyIrResponse.onStudyFailed();
                }
            }
        }
    }
}
