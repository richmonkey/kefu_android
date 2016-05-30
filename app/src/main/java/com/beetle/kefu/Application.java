package com.beetle.kefu;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.google.code.p.leveldb.LevelDB;
import com.squareup.otto.Bus;

import java.io.File;
import java.util.List;

/**
 * Created by houxh on 16/5/2.
 */
public class Application extends android.app.Application {
    private static final String TAG = "kefu";

    @Override
    public void onCreate() {
        super.onCreate();

        if (!isAppProcess()) {
            Log.i(TAG, "service application create");
            return;
        }
        Log.i(TAG, "app application create");

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        Log.i(TAG, "dir:" + dir);
        ldb.open(dir);

        FileCache fc = FileCache.getInstance();
        fc.setDir(this.getDir("cache", MODE_PRIVATE));

        IMService im = IMService.getInstance();

        im.setHost("121.41.30.52");//imnode.91lace.com
        IMHttpAPI.setAPIURL("http://121.41.30.52:20000");//api.im.91lace.com
        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        im.setDeviceID(androidID);

        long APPID = 17;
        im.setAppID(APPID);
        im.setCustomerMessageHandler(CustomerSupportMessageHandler.getInstance());
        im.registerConnectivityChangeReceiver(getApplicationContext());
    }

    private boolean isAppProcess() {
        Context context = getApplicationContext();
        int pid = android.os.Process.myPid();
        Log.i(TAG, "pid:" + pid + "package name:" + context.getPackageName());
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.i(TAG, "package name:" + appProcess.processName + " importance:" + appProcess.importance + " pid:" + appProcess.pid);
            if (pid == appProcess.pid) {
                if (appProcess.processName.equals(context.getPackageName())) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }


}
