package com.beetle.kefu.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.beetle.kefu.MessageListActivity;
import com.beetle.kefu.R;


/**
 * Created by houxh on 16/7/19.
 */
public class AssistService extends Service {
    private static final String TAG = "face";


    private final int PID = android.os.Process.myPid();


    public class LocalBinder extends Binder {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "AssistService: onBind()");
        return new LocalBinder();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "start foreground service");
        this.startForeground(PID, getNotification());
    }
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        this.stopForeground(true);
        Log.d(TAG, "AssistService: onDestroy()");
    }


    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MessageListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        String appName = getResources().getString(R.string.app_name);
        String running = getResources().getString(R.string.running);
        Notification notification = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle(appName)
                .setContentText(running)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .getNotification();
        return notification;
    }

}

