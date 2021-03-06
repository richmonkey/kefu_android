package com.beetle.kefu;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.bauhinia.db.SyncKeyHandler;
import com.beetle.im.IMService;
import com.beetle.im.Timer;
import com.beetle.kefu.api.APIService;
import com.beetle.kefu.api.Authorization;
import com.beetle.kefu.model.Profile;
import com.beetle.kefu.model.Token;
import com.google.code.p.leveldb.LevelDB;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.os.SystemClock.uptimeMillis;

/**
 * Created by houxh on 16/5/3.
 */
public class MainActivity extends BaseActivity {

    private final static String TAG = "kefu";

    private Timer refreshTokenTimer;

    private Object listener = new Object() {
        @Subscribe
        public void onLogout(BusCenter.Logout e) {
            Log.i(TAG, "MainActivity logout...");
            MainActivity.this.finish();
        }

        @Subscribe
        public void onXMDeviceToken(BusCenter.XMDeviceToken token) {
            PostDeviceToken t = new PostDeviceToken();
            t.xmDeviceToken = token.token;
            IMHttpAPI.Singleton().bindDeviceToken(t)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Object>() {
                        @Override
                        public void call(Object o) {
                            Log.i(TAG, "bind token success");
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.i(TAG, "bind token error:" + throwable);
                        }
                    });
            Profile.getInstance().xmDeviceToken = token.token;
        }
    };;

    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Token token = Token.getInstance();
        Profile profile = Profile.getInstance();

        LevelDB ldb = LevelDB.getDefaultDB();
        String dbPath = getFilesDir().getAbsoluteFile() + File.separator + "db_" + profile.uid;
        Log.i(TAG, "leveldb dir:" + dbPath);
        ldb.open(dbPath);

        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        File f = this.getDir("" + profile.uid, MODE_PRIVATE);
        File f2 = new File(f, "customer");
        f2.mkdir();
        db.setDir(f2);

        Log.i(TAG, "customer message path:" + f2.getAbsolutePath());

        IMHttpAPI.setToken(token.accessToken);
        IMService im =  IMService.getInstance();
        im.setUID(profile.uid);
        im.setToken(token.accessToken);

        SyncKeyHandler handler = new SyncKeyHandler(this.getApplicationContext(), "sync_key");
        handler.load();

        IMService.getInstance().setSyncKey(handler.getSyncKey());
        Log.i(TAG, "sync key:" + handler.getSyncKey());
        IMService.getInstance().setSyncKeyHandler(handler);

        im.start();

        int now = getNow();
        if (now >= Token.getInstance().expireTimestamp - 60) {
            refreshToken();
        } else {
            int t = Token.getInstance().expireTimestamp - 60 - now;
            refreshTokenDelay(t);
        }

        initXiaomiPush();

        Bus bus = BusCenter.getBus();
        bus.register(listener);

        try
        {
            File dir = getCacheDir();
            File iconFile = new File(dir, "xiaowei.png");
            if (iconFile.length() == 0) {
                InputStream inputStream = getResources().openRawResource(R.drawable.xiaowei);
                OutputStream out = new FileOutputStream(iconFile);
                byte buf[] = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                inputStream.close();
            }
        }
        catch (IOException e) {

        }
    }

    private void initXiaomiPush() {
        // 注册push服务，注册成功后会向XiaomiPushReceiver发送广播
        // 可以从onCommandResult方法中MiPushCommandMessage对象参数中获取注册信息
        String appId = "2882303761517469469";
        String appKey = "5761746957469";
        MiPushClient.registerPush(this, appId, appKey);
    }

    public static int getNow() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    private void refreshToken() {
        Authorization.RefreshToken rt = new Authorization.RefreshToken();
        rt.refreshToken = Token.getInstance().refreshToken;

        Authorization api = APIService.getAuthoriation();
        api.refreshAccessToken(rt).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Authorization.AccessToken>() {
                    @Override
                    public void call(Authorization.AccessToken token) {
                        Log.i(TAG, "refresh token success");
                        Log.i(TAG, "access token:" + token.accessToken);
                        Token t = Token.getInstance();
                        t.accessToken = token.accessToken;
                        t.refreshToken = token.refreshToken;
                        t.expireTimestamp = token.expires + getNow();
                        t.save(MainActivity.this);

                        int ts = t.expireTimestamp - 60 - getNow();
                        if (ts <= 0) {
                            android.util.Log.w(TAG, "expire timestamp:" + t.expireTimestamp);
                            return;
                        }
                        refreshTokenDelay(ts);

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "refresh token error:" + throwable);
                    }
                });
    }

    private void refreshTokenDelay(int t) {
        if (refreshTokenTimer != null) {
            refreshTokenTimer.suspend();
        }

        refreshTokenTimer = new Timer() {
            @Override
            protected void fire() {
                refreshToken();
            }
        };
        refreshTokenTimer.setTimer(uptimeMillis() + t*1000);
        refreshTokenTimer.resume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "main activity on stop");

        if (!isAppOnForeground()) {
            if (refreshTokenTimer != null) {
                refreshTokenTimer.suspend();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int now = getNow();
        if (now >= Token.getInstance().expireTimestamp - 60) {
            refreshToken();
        } else {
            int t = Token.getInstance().expireTimestamp - 60 - now;
            refreshTokenDelay(t);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMService im = IMService.getInstance();
        im.stop();

        LevelDB.getDefaultDB().close();

        BusCenter.getBus().unregister(listener);
    }


}
