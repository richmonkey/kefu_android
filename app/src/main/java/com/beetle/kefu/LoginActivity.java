package com.beetle.kefu;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.beetle.kefu.api.APIService;
import com.beetle.kefu.api.Authorization;
import com.beetle.kefu.model.Token;

import java.util.Date;

import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class LoginActivity extends ActionBarActivity {
    public static final String TAG = "kefu";

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Intent intent = getIntent();
        boolean hint = intent.getBooleanExtra("hint", false);
        if (hint) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("你的账号在其它设备上登录");
            builder.setNegativeButton("确定", null);
            builder.show();
        }
    }

    void login(String username, String password) {
        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Authorization.User u = new Authorization.User();
        u.username = username;
        u.password = password;
        u.deviceName = String.format("%s-%s", android.os.Build.BRAND, android.os.Build.MODEL);
        u.deviceID = androidID;
        u.platform = Authorization.PLATFORM_ANDROID;

        Authorization api = APIService.getAuthoriation();
        api.getAccessToken(u).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Authorization.AccessToken>() {
                    @Override
                    public void call(Authorization.AccessToken token) {
                        Log.i(TAG, "access token:" + token.accessToken);
                        Token t = Token.getInstance();
                        t.accessToken = token.accessToken;
                        t.refreshToken = token.refreshToken;
                        t.expireTimestamp = token.expires + now();
                        t.storeID = token.storeID;
                        t.uid = token.uid;
                        t.name = token.name;
                        t.loginTimestamp = now();
                        t.save();

                        Intent intent = new Intent(LoginActivity.this, MessageListActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        finish();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "throwable:" + throwable);
                        RetrofitError error = (RetrofitError)throwable;
                        if (error.getResponse() != null) {
                            Authorization.Error e = (Authorization.Error) error.getBodyAs(Authorization.Error.class);
                            Log.i(TAG, "error:" + e.error);
                            Toast.makeText(getApplicationContext(), e.error, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "登录失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void onLogin(View view) {
        Log.i(TAG, "on login");

        EditText name = (EditText) findViewById(R.id.username);
        EditText password = (EditText) findViewById(R.id.password);

        if (TextUtils.isEmpty(name.getText())) {
            Toast.makeText(getApplicationContext(), "用户名为空", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(password.getText())) {
            Toast.makeText(getApplicationContext(), "密码为空", Toast.LENGTH_SHORT).show();
        }

        login(name.getText().toString(), password.getText().toString());
    }

    public void onRegister(View view) {
        String url = "http://www.xiaowei.io";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
}
