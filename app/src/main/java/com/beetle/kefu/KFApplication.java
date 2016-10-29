package com.beetle.kefu;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
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


    private int started = 0;
    private int stopped = 0;

    public void onActivityCreated(Activity activity, Bundle bundle) {
        Log.i("","onActivityCreated:" + activity.getLocalClassName());
    }

    public void onActivityDestroyed(Activity activity) {
        Log.i("","onActivityDestroyed:" + activity.getLocalClassName());
    }

    public void onActivityPaused(Activity activity) {
        Log.i("","onActivityPaused:" + activity.getLocalClassName());
    }

    public void onActivityResumed(Activity activity) {
        Log.i("","onActivityResumed:" + activity.getLocalClassName());
    }

    public void onActivitySaveInstanceState(Activity activity,
                                            Bundle outState) {
        Log.i("","onActivitySaveInstanceState:" + activity.getLocalClassName());
    }

    public void onActivityStarted(Activity activity) {
        Log.i("","onActivityStarted:" + activity.getLocalClassName());
        ++started;

        if (started - stopped == 1 ) {
            IMService.getInstance().enterForeground();
        }
    }

    public void onActivityStopped(Activity activity) {
        Log.i("","onActivityStopped:" + activity.getLocalClassName());
        ++stopped;
        if (stopped == started) {
            Log.i(TAG, "app enter background stop imservice");
            IMService.getInstance().enterBackground();
        }

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
