package com.beetle.kefu.model;

import com.google.code.p.leveldb.LevelDB;

/**
 * Created by houxh on 16/5/3.
 */
public class NewCount {
    public static int getNewCount(long appid, long uid) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("news_%d_%d", appid, uid);
            return (int)db.getLong(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setNewCount(long appid, long uid, int count) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("news_%d_%d", appid, uid);
            db.setLong(key, count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
