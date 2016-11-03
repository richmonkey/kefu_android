package com.beetle.kefu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.beetle.kefu.model.Profile;
import com.beetle.kefu.model.Token;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.kyleduo.switchbutton.SwitchButton;
import com.squareup.otto.Bus;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingActivity extends BaseActivity {
    private static final String TAG = "kefu";
    protected ActionBar actionBar;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mainHandler = new Handler(getMainLooper());

        Profile profile = Profile.getInstance();

        if (profile.isOnline()) {
            findViewById(R.id.online).setVisibility(View.VISIBLE);
            findViewById(R.id.offline).setVisibility(View.GONE);
        } else {
            findViewById(R.id.online).setVisibility(View.GONE);
            findViewById(R.id.offline).setVisibility(View.VISIBLE);
        }

        TextView numberTextView = (TextView)findViewById(R.id.number);
        TextView nameTextView = (TextView)findViewById(R.id.name);
        numberTextView.setText(String.format("%d", profile.uid));
        if (!TextUtils.isEmpty(profile.name)) {
            nameTextView.setText(String.format("%s", profile.name));
        }

        SwitchButton switchButton = (SwitchButton)findViewById(R.id.run);
        switchButton.setCheckedImmediately(profile.keepalive);
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Profile profile = Profile.getInstance();
                profile.keepalive = isChecked;
                profile.save(SettingActivity.this);
                Log.i(TAG, "app keepalive: " + (isChecked ? "on" : "off"));
            }
        });
    }

    Request newUnregisterRequest(String xmDeviceToken) {
        String url = Config.API_URL + "/auth/unregister";
        JSONObject body = new JSONObject();
        if (xmDeviceToken == null) {
            xmDeviceToken = "";
        }
        try {
            body.put("xm_device_token", xmDeviceToken);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        Token token = Token.getInstance();
        String auth = String.format("Bearer %s", token.accessToken);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, body.toString());
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .post(requestBody)
                .build();

        return request;
    }

    public void onHelpClick(View v) {

        Intent intent = new Intent(this, XWMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra("title", getResources().getString(R.string.xiaowei));
        intent.putExtra("store_id", Config.XIAOWEI_STORE_ID);
        intent.putExtra("seller_id", Config.XIAOWEI_SELLER_ID);
        intent.putExtra("current_uid", Profile.getInstance().uid);
        intent.putExtra("app_id", Config.XIAOWEI_APPID);

        startActivity(intent);
    }

    public void onAboutClick(View v) {
        Log.i(TAG, "on about click");
    }

    public void onOnlineClick(View v) {
        setUserStatus(true);
    }
    public void onOfflineClick(View v) {
        setUserStatus(false);
    }

    Request newSetUserStatusRequest(boolean online) {
        Profile profile = Profile.getInstance();
        String url = Config.API_URL + "/users/" + profile.uid;

        JSONObject body = new JSONObject();
        try {
            body.put("status", online ? Profile.STATUS_ONLINE : Profile.STATUS_OFFLINE);
        } catch (JSONException e) {
            e.printStackTrace();
            //impossible
            return null;
        }
        Token token = Token.getInstance();

        String auth = String.format("Bearer %s", token.accessToken);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, body.toString());
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .patch(requestBody)
                .build();

        return request;
    }

    void setUserStatus(final boolean online) {
        Profile profile = Profile.getInstance();
        if (profile.isOnline() == online) {
            return;
        }


        final String errorText = online ? "上线失败" : "隐身失败";
        final String status = online ? Profile.STATUS_ONLINE : Profile.STATUS_OFFLINE;
        final KProgressHUD hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setCancellable(true)
                .setAnimationSpeed(1)
                .setDimAmount(0.5f)
                .show();

        OkHttpClient client = new OkHttpClient();
        Request request = newSetUserStatusRequest(online);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        hud.dismiss();
                        Toast.makeText(SettingActivity.this, errorText, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                int statusCode = response.code();
                if (statusCode != 200) {
                    Log.i(TAG, "response code:" + statusCode + " " + resp);
                    throw new IOException("status code:" + statusCode);
                }

                try {
                    Log.i(TAG, "set user status:" + status + " response:" + resp);
                    JSONObject obj = new JSONObject(resp);

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            Profile profile = Profile.getInstance();
                            profile.status = online ? Profile.STATUS_ONLINE : Profile.STATUS_OFFLINE;
                            profile.save(SettingActivity.this);

                            if (profile.isOnline()) {
                                findViewById(R.id.online).setVisibility(View.VISIBLE);
                                findViewById(R.id.offline).setVisibility(View.GONE);
                            } else {
                                findViewById(R.id.online).setVisibility(View.GONE);
                                findViewById(R.id.offline).setVisibility(View.VISIBLE);
                            }

                            hud.dismiss();
                        }
                    };
                    SettingActivity.this.mainHandler.post(r);

                } catch (JSONException e) {
                    e.printStackTrace();
                    throw new IOException("can't json decode");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return false;
    }


    private void logout() {
        Token t = Token.getInstance();
        t.accessToken = "";
        t.refreshToken = "";
        t.expireTimestamp = 0;
        t.save(this);

        Profile profile = Profile.getInstance();
        profile.uid = 0;
        profile.storeID = 0;
        profile.status = "";
        profile.name = "";
        profile.avatar = "";
        profile.loginTimestamp = 0;
        profile.keepalive = false;
        profile.save(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        Bus bus = BusCenter.getBus();
        bus.post(new BusCenter.Logout());

        finish();
    }

    public void unregister() {
        final KProgressHUD hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setCancellable(true)
                .setAnimationSpeed(1)
                .setDimAmount(0.5f)
                .show();

        OkHttpClient client = new OkHttpClient();

        Profile profile = Profile.getInstance();
        Request request = newUnregisterRequest(profile.xmDeviceToken);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        hud.dismiss();
                        Toast.makeText(SettingActivity.this, "注销失败，请检查网络是否连接", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                int statusCode = response.code();
                if (statusCode != 200) {
                    Log.i(TAG, "response code:" + statusCode + " " + resp);
                    throw new IOException("status code:" + statusCode);
                }

                Log.i(TAG, "unregister response:" + resp);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        hud.dismiss();
                        SettingActivity.this.logout();
                    }
                };
                SettingActivity.this.mainHandler.post(r);
            }
        });
    }

    public void onLogout(View view) {
        Log.i(TAG, "logout");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("确认退出吗？");
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SettingActivity.this.unregister();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
