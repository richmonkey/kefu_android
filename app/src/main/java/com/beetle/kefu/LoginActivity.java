package com.beetle.kefu;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.beetle.bauhinia.db.ICustomerMessage;
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
    }

    void login(String username, String password) {
        Authorization.User u = new Authorization.User();
        u.username = username;
        u.password = password;

        Authorization api = APIService.getAuthoriation();
        api.getAccessToken(u) .observeOn(AndroidSchedulers.mainThread())
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
                        Response resp = error.getResponse();
                        Log.i(TAG, "reason:" + resp.getReason());
                        Log.i(TAG, "resp:" + resp);
                        Toast.makeText(getApplicationContext(), "登录失败", Toast.LENGTH_SHORT).show();
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

}
