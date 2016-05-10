package com.beetle.kefu.model;

import com.google.code.p.leveldb.LevelDB;

public class Token {
    private static Token instance;
    public static Token getInstance() {
        if (instance == null) {
            instance = new Token();
            instance.load();
        }
        return instance;
    }

    public String accessToken;
    public String refreshToken;
    public int expireTimestamp;
    public long uid;
    public long storeID;
    public String name;
    public int loginTimestamp;

    public void save() {
        LevelDB db = LevelDB.getDefaultDB();
        try {
            db.set("token_access_token", accessToken);
            db.set("token_refresh_token", refreshToken);
            db.setLong("token_expire", expireTimestamp);
            db.setLong("token_uid", uid);
            db.setLong("token_store_id", storeID);
            db.set("token_name", name);
            db.setLong("token_login", loginTimestamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        LevelDB db = LevelDB.getDefaultDB();
        try {
            accessToken = db.get("token_access_token");
            refreshToken = db.get("token_refresh_token");
            expireTimestamp = (int)db.getLong("token_expire");
            uid = db.getLong("token_uid");
            name = db.get("token_name");
            storeID = db.getLong("token_store_id");
            loginTimestamp = (int)db.getLong("token_login");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}