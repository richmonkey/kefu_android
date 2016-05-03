package com.beetle.kefu;

import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.im.CustomerMessage;

/**
 * Created by houxh on 16/5/3.
 */
public class CustomerSupportMessageHandler implements com.beetle.im.CustomerMessageHandler {

    private static CustomerSupportMessageHandler instance = new CustomerSupportMessageHandler();

    public static CustomerSupportMessageHandler getInstance() {
        return instance;
    }

    @Override
    public boolean handleCustomerSupportMessage(CustomerMessage msg) {
        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        ICustomerMessage imsg = new ICustomerMessage();

        imsg.timestamp = msg.timestamp;
        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.sender = msg.customerID;
        imsg.receiver = msg.storeID;
        imsg.isSupport = true;

        imsg.setContent(msg.content);
        boolean r = db.insertMessage(imsg);
        msg.msgLocalID = imsg.msgLocalID;
        return r;
    }

    @Override
    public boolean handleMessage(CustomerMessage msg) {
        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        ICustomerMessage imsg = new ICustomerMessage();

        imsg.timestamp = msg.timestamp;
        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.sender = msg.customerID;
        imsg.receiver = msg.storeID;
        imsg.isSupport = false;

        imsg.setContent(msg.content);
        boolean r = db.insertMessage(imsg);
        msg.msgLocalID = imsg.msgLocalID;
        return r;
    }

    @Override
    public boolean handleMessageACK(CustomerMessage msg) {
        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        return db.acknowledgeMessage(msg.msgLocalID, msg.customerID, msg.customerAppID);
    }

    @Override
    public boolean handleMessageFailure(CustomerMessage msg) {
        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        return db.markMessageFailure(msg.msgLocalID, msg.customerID, msg.customerAppID);
    }
}
