package com.beetle.kefu.api;

import com.google.gson.Gson;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by houxh on 16/5/2.
 */
public class APIService {

    public static final String API_URL = "http://api.kefu.gobelieve.io";

    private static Authorization newAuthorization() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .setConverter(new GsonConverter(new Gson()))
                .build();

        return adapter.create(Authorization.class);
    }

    static final Object monitor = new Object();
    static Authorization authorization;

    public static Authorization getAuthoriation() {
        if (authorization == null) {
            synchronized (monitor) {
                if (authorization == null) {
                    authorization = newAuthorization();
                }
            }
        }
        return authorization;
    }
}
