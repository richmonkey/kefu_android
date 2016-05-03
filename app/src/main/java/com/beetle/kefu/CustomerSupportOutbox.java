package com.beetle.kefu;

import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.tools.Outbox;
import com.beetle.im.CustomerMessage;
import com.beetle.im.IMService;

/**
 * Created by houxh on 16/5/3.
 */
public class CustomerSupportOutbox extends Outbox {
    private static CustomerSupportOutbox instance = new CustomerSupportOutbox();
    public static CustomerSupportOutbox getInstance() {
        return instance;
    }

    @Override
    protected void markMessageFailure(IMessage msg) {
        ICustomerMessage cm = (ICustomerMessage)msg;
        CustomerSupportMessageDB.getInstance().markMessageFailure(cm.msgLocalID,
                cm.customerID, cm.customerAppID);
    }

    @Override
    protected void sendImageMessage(IMessage imsg, String url) {
        ICustomerMessage cm = (ICustomerMessage)imsg;

        CustomerMessage msg = new CustomerMessage();
        msg.msgLocalID = imsg.msgLocalID;
        msg.customerAppID = cm.customerAppID;
        msg.customerID = cm.customerID;
        msg.storeID = cm.storeID;
        msg.sellerID = cm.sellerID;
        msg.content = IMessage.newImage(url).getRaw();

        IMService im = IMService.getInstance();
        im.sendCustomerSupportMessage(msg);
    }

    @Override
    protected void sendAudioMessage(IMessage imsg, String url) {
        ICustomerMessage cm = (ICustomerMessage)imsg;
        IMessage.Audio audio = (IMessage.Audio)imsg.content;

        CustomerMessage msg = new CustomerMessage();
        msg.msgLocalID = imsg.msgLocalID;
        msg.customerAppID = cm.customerAppID;
        msg.customerID = cm.customerID;
        msg.storeID = cm.storeID;
        msg.sellerID = cm.sellerID;

        msg.content = IMessage.newAudio(url, audio.duration).getRaw();

        IMService im = IMService.getInstance();
        im.sendCustomerSupportMessage(msg);
    }

}
