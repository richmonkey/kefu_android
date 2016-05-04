package com.beetle.kefu.model;
import com.google.code.p.leveldb.LevelDB;

public class User {
    public long appID;
    public long uid;
    public String name;
    public String avatarURL;
    public int timestamp;
    //name为nil时，界面显示identifier字段,未被保存
    public String identifier;

    public static void save(User user) {
        LevelDB db = LevelDB.getDefaultDB();
        try {
            String prefix = String.format("users_%d_%d", user.appID, user.uid);
            String key = String.format("%s_name", prefix);

            String name = user.name != null ? user.name : "";
            db.set(key, name);

            key = String.format("%s_avatar", prefix);
            String avatar = user.avatarURL != null ? user.avatarURL : "";
            db.set(key, avatar);

            key = String.format("%s_timestamp", prefix);
            db.setLong(key, user.timestamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static User load(long appID, long uid) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            User u = new User();
            u.appID = appID;
            u.uid = uid;

            String prefix = String.format("users_%d_%d", appID, uid);
            String key = String.format("%s_name", prefix);
            u.name = db.get(key);

            key = String.format("%s_avatar", prefix);
            u.avatarURL = db.get(key);

            key = String.format("%s_timestamp", prefix);
            u.timestamp = (int)db.getLong(key);
            return u;
        } catch (Exception e) {
            return null;
        }
    }
}