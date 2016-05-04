package com.beetle.kefu.api;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import rx.Observable;

/**
 * Created by houxh on 16/5/4.
 */
public interface Customer {
    @GET("/customers/{appid}/{uid}")
    Observable<User> getCustomer(@Path("appid") long appid, @Path("uid") long uid);

    class User {
        public String appid;
        public String uid;
        public String name;
    }
}
