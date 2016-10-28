package com.beetle.kefu.model;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by houxh on 2016/10/28.
 */

public class Profile {
    private static Profile instance;
    public static Profile getInstance() {
        if (instance == null) {
            instance = new Profile();
        }
        return instance;
    }

    public String status;
    public long uid;
    public String name;
    public String avatar;
    public long storeID;
    public int loginTimestamp;//单位：秒


    public void save(Context context) {
        SharedPreferences pref = context.getSharedPreferences("profile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putLong("uid", this.uid);
        editor.putLong("store_id", this.storeID);
        editor.putString("name", (this.name != null ? this.name : ""));
        editor.putString("avatar", this.avatar != null ? this.avatar : "");
        editor.putString("status", this.status != null ? this.status : "");
        editor.putInt("timestamp", this.loginTimestamp);

        editor.commit();
    }

    public void load(Context context) {
        SharedPreferences customer = context.getSharedPreferences("profile", Context.MODE_PRIVATE);

        this.storeID = customer.getLong("store_id", 0);
        this.uid = customer.getLong("uid", 0);
        this.name = customer.getString("name", "");
        this.avatar = customer.getString("avatar", "");
        this.loginTimestamp = customer.getInt("timestamp", 0);
        this.status = customer.getString("status", "");
    }
}
