package fitme.ai;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by 69441 on 2017/7/26.
 */

public class Tools {

    private static final String TAG = "SoundAI";

    public static void copyAssets2Sdcard(Context context) {
        if (new File("/sdcard/sai_config/sai_api.q").exists()) {
            Log.d(TAG, "copyAssets2Sdcard: config file exist");
            return;
        }
        File sai_config = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sai_config");
        if (!sai_config.exists() || !sai_config.isDirectory())
            sai_config.mkdirs();
        copyAsset(context, "saires.q", sai_config.getAbsolutePath() + "/saires.q");
        copyAsset(context, "saires2.q", sai_config.getAbsolutePath() + "/saires2.q");
        copyAsset(context, "sai_config.txt", sai_config.getAbsolutePath() + "/sai_config.txt");
        copyAsset(context, "wopt_6mic_sai.bin", sai_config.getAbsolutePath() + "/wopt_6mic_sai.bin");

    }

    private static void copyAsset(Context context, String oldPath, String newPath){
        InputStream is;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(newPath);
            is = context.getAssets().open(oldPath);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = is.read(buffer)) != -1){
                fos.write(buffer,0,length);
            }
            fos.flush();
            fos.close();
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
