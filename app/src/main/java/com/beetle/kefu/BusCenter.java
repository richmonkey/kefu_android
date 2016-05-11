package com.beetle.kefu;

import com.squareup.otto.Bus;

/**
 * Created by houxh on 16/5/3.
 */
public class BusCenter {
    private static final Bus BUS = new Bus();
    public static Bus getBus() {
        return BUS;
    }

    public static class Logout {

    }

    public static class XMDeviceToken {
        public String token;
    }
}
