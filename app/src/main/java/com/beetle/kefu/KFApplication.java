package com.beetle.kefu;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.beetle.kefu.model.Profile;
import com.beetle.kefu.model.Token;
import com.beetle.kefu.service.ForegroundService;
import com.google.code.p.leveldb.LevelDB;

import java.io.File;
import java.util.List;

/**
 * Created by houxh on 16/5/2.
 */
public class KFApplication extends Application implements Application.ActivityLifecycleCallbacks  {
    private static final String TAG = "kefu";

    @Override
    public void onCreate() {
        super.onCreate();

        if (!isAppProcess()) {
            Log.i(TAG, "service application create");
            return;
        }
        Log.i(TAG, "app application create");

        Token token = Token.getInstance();
        token.load(this);

        Profile profile = Profile.getInstance();
        profile.load(this);

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        Log.i(TAG, "dir:" + dir);
        ldb.open(dir);

        FileCache fc = FileCache.getInstance();
        fc.setDir(this.getDir("cache", MODE_PRIVATE));

        IMService im = IMService.getInstance();
        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        im.setDeviceID(androidID);

        im.setAppID(Config.XIAOWEI_APPID);
        im.setCustomerMessageHandler(CustomerSupportMessageHandler.getInstance());
        im.registerConnectivityChangeReceiver(getApplicationContext());

        registerActivityLifecycleCallbacks(this);
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


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }
    @Override
    public void onActivityStarted(Activity activity) {

    }
    public void onActivityResumed(Activity activity) {
        IMService.getInstance().enterForeground();
    }

    public void onActivityPaused(Activity activity) {

    }
    public void onActivityStopped(Activity activity) {
        if (!isAppOnForeground()) {
            //keep app foreground state
            Profile profile = Profile.getInstance();
            if (profile.keepalive) {
                Log.i(TAG, "start foreground service");
                Intent service = new Intent(this, ForegroundService.class);
                startService(service);
            }
        }
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }
    public void onActivityDestroyed(Activity activity) {

    }


    /**
     * 程序是否在前台运行
     *
     * @return
     */
    public boolean isAppOnForeground() {
        // Returns a list of application processes that are running on the
        // device

        ActivityManager activityManager =
                (ActivityManager) getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        String packageName = getApplicationContext().getPackageName();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance
                    == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }

        return false;
    }

}
