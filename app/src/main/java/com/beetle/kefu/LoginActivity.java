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

import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.kefu.api.APIService;
import com.beetle.kefu.api.Authorization;
import com.beetle.kefu.model.Profile;
import com.beetle.kefu.model.Token;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.io.File;
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

        final KProgressHUD hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setCancellable(true)
                .setAnimationSpeed(1)
                .setDimAmount(0.5f)
                .show();

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
                        t.save(LoginActivity.this);

                        Profile profile = Profile.getInstance();
                        profile.uid = token.uid;
                        profile.name = token.name;
                        profile.avatar = "";
                        profile.storeID = token.storeID;
                        profile.status = Profile.STATUS_ONLINE;
                        profile.loginTimestamp = now();
                        profile.keepalive = false;
                        profile.save(LoginActivity.this);

                        LoginActivity.this.insertWelcomeMessage();

                        hud.dismiss();
                        
                        Intent intent = new Intent(LoginActivity.this, MessageListActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        finish();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "throwable:" + throwable);
                        hud.dismiss();
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

    private void insertWelcomeMessage() {
        Profile profile = Profile.getInstance();
        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        File f = this.getDir("" + profile.uid, MODE_PRIVATE);
        File f2 = new File(f, "customer");
        f2.mkdir();
        db.setDir(f2);

        MessageIterator iter = CustomerSupportMessageDB.getInstance().newMessageIterator(profile.uid, Config.XIAOWEI_APPID);

        boolean exists = false;
        while (iter != null) {
            ICustomerMessage msg = (ICustomerMessage)iter.next();
            if (msg == null) {
                break;
            }
            exists = true;
            break;
        }

        if (!exists) {
            ICustomerMessage msg = new ICustomerMessage();
            msg.customerID = profile.uid;
            msg.customerAppID = Config.XIAOWEI_APPID;
            msg.storeID = Config.XIAOWEI_STORE_ID;
            msg.sellerID = 0;

            msg.timestamp = now();
            msg.sender = 0;
            msg.receiver = profile.uid;

            msg.isSupport = true;
            msg.isOutgoing = false;

            msg.setContent(IMessage.newText("欢迎你使用小微客服"));
            CustomerSupportMessageDB.getInstance().insertMessage(msg);
        }
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
