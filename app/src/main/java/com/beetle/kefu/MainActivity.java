package com.beetle.kefu;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.im.IMService;
import com.beetle.im.Timer;
import com.beetle.kefu.api.APIService;
import com.beetle.kefu.api.Authorization;
import com.beetle.kefu.model.Token;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.Date;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.os.SystemClock.uptimeMillis;

/**
 * Created by houxh on 16/5/3.
 */
public class MainActivity extends BaseActivity {

    private final static String TAG = "kefu";

    private Timer refreshTokenTimer;

    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Token token = Token.getInstance();

        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        File f = this.getDir("" + token.uid, MODE_PRIVATE);
        File f2 = new File(f, "customer");
        f2.mkdir();
        db.setDir(f2);

        Log.i(TAG, "customer message path:" + f2.getAbsolutePath());

        IMHttpAPI.setToken(token.accessToken);
        IMService im =  IMService.getInstance();
        im.setUID(token.uid);
        im.setToken(token.accessToken);
        im.start();

        int now = getNow();
        if (now >= Token.getInstance().expireTimestamp - 60) {
            refreshToken();
        } else {
            int t = Token.getInstance().expireTimestamp - 60 - now;
            refreshTokenDelay(t);
        }
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
                        if (!TextUtils.isEmpty(token.name)) {
                            t.name = token.name;
                        }
                        t.save();


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

    }

}
