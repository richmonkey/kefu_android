package com.beetle.kefu.api;

import android.text.TextUtils;

import com.beetle.kefu.model.Token;
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

    private static Customer newCustomer() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .setConverter(new GsonConverter(new Gson()))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        Token t = Token.getInstance();
                        if (!TextUtils.isEmpty(t.accessToken)) {
                            request.addHeader("Authorization", "Bearer " + t.accessToken);
                        }
                    }
                })
                .build();

        return adapter.create(Customer.class);
    }

    private static Robot newRobot() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .setConverter(new GsonConverter(new Gson()))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        Token t = Token.getInstance();
                        if (!TextUtils.isEmpty(t.accessToken)) {
                            request.addHeader("Authorization", "Bearer " + t.accessToken);
                        }
                    }
                })
                .build();

        return adapter.create(Robot.class);
    }



    static final Object monitor = new Object();
    static Authorization authorization;
    static Customer customer;
    static Robot robot;

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

    public static Customer getCustomerService() {
        if (customer == null) {
            synchronized (monitor) {
                if (customer == null) {
                    customer = newCustomer();
                }
            }
        }
        return customer;
    }

    public static Robot getRobotService() {
        if (robot == null) {
            synchronized (monitor) {
                if (robot == null) {
                    robot = newRobot();
                }
            }
        }
        return robot;
    }
}
