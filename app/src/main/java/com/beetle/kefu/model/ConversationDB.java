package com.beetle.kefu.model;

import com.google.code.p.leveldb.LevelDB;

/**
 * Created by houxh on 16/5/3.
 */
public class ConversationDB {
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

    public static void setTop(long appid, long uid, boolean top) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("top_%d_%d", appid, uid);
            db.setLong(key, top?1:0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getTop(long appid, long uid) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("top_%d_%d", appid, uid);
            int t = (int)db.getLong(key);
            return t == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
