package com.beetle.kefu.api;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import rx.Observable;

public interface Authorization {
    @POST("/auth/token")
    Observable<AccessToken> getAccessToken(@Body User user);

    @POST("/auth/refresh_token")
    Observable<AccessToken> refreshAccessToken(@Body RefreshToken rt);


    public class User {
        public String username;
        public String password;
    }

    public class RefreshToken {
        @SerializedName("refresh_token")
        public String refreshToken;
    }

    public class AccessToken {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("refresh_token")
        public String refreshToken;
        @SerializedName("store_id")
        public long storeID;

        public long uid;
        public String name;

        @SerializedName("expires_in")
        public int expires;
    }

}


