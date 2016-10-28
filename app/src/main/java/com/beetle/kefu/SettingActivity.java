package com.beetle.kefu;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.beetle.kefu.model.Profile;
import com.beetle.kefu.model.Token;
import com.squareup.otto.Bus;

public class SettingActivity extends BaseActivity {
    private static final String TAG = "kefu";
    protected ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Profile profile = Profile.getInstance();
        TextView numberTextView = (TextView)findViewById(R.id.number);
        TextView nameTextView = (TextView)findViewById(R.id.name);
        numberTextView.setText(String.format("客服工号    %d", profile.uid));
        if (!TextUtils.isEmpty(profile.name)) {
            nameTextView.setText(String.format("客服姓名    %s", profile.name));
        }
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


    public void onLogout(View view) {
        Log.i(TAG, "logout");

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
        profile.save(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        Bus bus = BusCenter.getBus();
        bus.post(new BusCenter.Logout());

        finish();
    }
}
