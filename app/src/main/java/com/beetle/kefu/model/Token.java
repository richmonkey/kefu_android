package com.beetle.kefu.model;

import android.content.Context;
import android.content.SharedPreferences;

public class Token {
    private static Token instance;
    public static Token getInstance() {
        if (instance == null) {
            instance = new Token();
        }
        return instance;
    }

    public String accessToken;
    public String refreshToken;
    public int expireTimestamp;


    public void save(Context context) {
        SharedPreferences pref = context.getSharedPreferences("token", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putInt("expire", this.expireTimestamp);
        editor.putString("access_token", (this.accessToken != null ? this.accessToken : ""));
        editor.putString("refresh_token", this.refreshToken != null ? this.refreshToken : "");

        editor.commit();
    }

    public void load(Context context) {
        SharedPreferences customer = context.getSharedPreferences("token", Context.MODE_PRIVATE);

        this.accessToken = customer.getString("access_token", "");
        this.refreshToken = customer.getString("refresh_token", "");
        this.expireTimestamp = customer.getInt("expire", 0);
    }

}