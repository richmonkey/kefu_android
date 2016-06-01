package com.beetle.kefu;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.beetle.bauhinia.MessageActivity;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.tools.AudioDownloader;
import com.beetle.bauhinia.tools.AudioUtil;
import com.beetle.bauhinia.tools.CustomerOutbox;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;
import com.beetle.im.CustomerMessage;
import com.beetle.im.CustomerMessageObserver;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.beetle.bauhinia.constant.RequestCodes.PICK_LOCATION;
import static com.beetle.bauhinia.constant.RequestCodes.ROBOT;

/**
 * Created by houxh on 16/5/3.
 */
public class CustomerSupportMessageActivity extends MessageActivity
        implements CustomerMessageObserver, IMServiceObserver,
        AudioDownloader.AudioDownloaderObserver,
        CustomerSupportOutbox.OutboxObserver {
    public static final String SEND_MESSAGE_NAME = "send_cs_message";
    public static final String CLEAR_MESSAGES = "clear_cs_messages";
    public static final String CLEAR_NEW_MESSAGES = "clear_cs_new_messages";

    private final int PAGE_SIZE = 10;

    protected long currentUID;
    protected long storeID;

    protected String customerName;
    long customerID;
    long customerAppID;

    public static class User {
        public long uid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }

    protected User getUser(long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("%d", uid);
        return u;
    }

    public interface GetUserCallback {
        void onUser(User u);
    }

    protected void asyncGetUser(long uid, GetUserCallback cb) {

    }

    private void loadUserName(IMessage msg) {
        User u = getUser(msg.sender);

        msg.setSenderAvatar(u.avatarURL);
        if (TextUtils.isEmpty(u.name)) {
            msg.setSenderName(u.identifier);
            final IMessage fmsg = msg;
            asyncGetUser(msg.sender, new GetUserCallback() {
                @Override
                public void onUser(User u) {
                    fmsg.setSenderName(u.name);
                    fmsg.setSenderAvatar(u.avatarURL);
                }
            });
        } else {
            msg.setSenderName(u.name);
        }
    }


    public CustomerSupportMessageActivity() {
        super();
        sendNotificationName = SEND_MESSAGE_NAME;
        clearNotificationName = CLEAR_MESSAGES;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        currentUID = intent.getLongExtra("current_uid", 0);
        storeID = intent.getLongExtra("store_id", 0);

        customerID = intent.getLongExtra("customer_id", 0);
        customerAppID = intent.getLongExtra("customer_appid", 0);
        customerName = intent.getStringExtra("customer_name");


        Log.i(TAG, "uid:" + currentUID + " store id:" + storeID +
                " customer id:" + customerID + " customer appid:" +
                customerAppID + " name:" + customerName);


        this.isShowUserName = intent.getBooleanExtra("show_name", false);


        this.loadConversationData();
        if (!TextUtils.isEmpty(customerName)) {
            titleView.setText(customerName);
        }

        CustomerSupportOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addCustomerServiceObserver(this);
        AudioDownloader.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");

        NotificationCenter nc = NotificationCenter.defaultCenter();
        HashMap<String, Long>  o = new HashMap();
        o.put("appid", this.customerAppID);
        o.put("uid", this.customerID);
        Notification notification = new Notification(o, CLEAR_NEW_MESSAGES);
        nc.postNotification(notification);

        CustomerSupportOutbox.getInstance().removeObserver(this);
        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removeCustomerServiceObserver(this);
        AudioDownloader.getInstance().removeObserver(this);
    }

    protected void loadConversationData() {
        messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = CustomerSupportMessageDB.getInstance().newMessageIterator(customerID, customerAppID);
        while (iter != null) {
            ICustomerMessage msg = (ICustomerMessage)iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment) msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                loadUserName(msg);
                msg.isOutgoing = (msg.isSupport && msg.sellerID==currentUID);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }

        downloadMessageContent(messages, count);
        checkMessageFailureFlag(messages, count);
        resetMessageTimeBase();
    }

    void checkMessageFailureFlag(IMessage msg) {
        if (msg.sender == this.currentUID) {
            if (msg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
                msg.setUploading(CustomerOutbox.getInstance().isUploading(msg));
            } else if (msg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
                msg.setUploading(CustomerOutbox.getInstance().isUploading(msg));
            }
            if (!msg.isAck() &&
                    !msg.isFailure() &&
                    !msg.getUploading() &&
                    !IMService.getInstance().isCustomerMessageSending(msg.receiver, msg.msgLocalID)) {
                markMessageFailure(msg);
                msg.setFailure(true);
            }
        }
    }

    void checkMessageFailureFlag(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage m = messages.get(i);
            checkMessageFailureFlag(m);
        }
    }


    protected void loadEarlierData() {
        if (messages.size() == 0) {
            return;
        }

        IMessage firstMsg = null;
        for (int i  = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            if (msg.msgLocalID > 0) {
                firstMsg = msg;
                break;
            }
        }
        if (firstMsg == null) {
            return;
        }

        int count = 0;
        MessageIterator iter = CustomerSupportMessageDB.getInstance().newMessageIterator(customerID, customerAppID, firstMsg.msgLocalID);
        while (iter != null) {
            ICustomerMessage msg = (ICustomerMessage)iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
                loadUserName(msg);
                msg.isOutgoing = (msg.isSupport && msg.sellerID==currentUID);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        if (count > 0) {
            downloadMessageContent(messages, count);
            checkMessageFailureFlag(messages, count);
            resetMessageTimeBase();
            adapter.notifyDataSetChanged();
            listview.setSelection(count);
        }
    }

    @Override
    protected MessageIterator getMessageIterator() {
        return CustomerSupportMessageDB.getInstance().newMessageIterator(this.customerID, this.customerAppID);
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {
        if (state == IMService.ConnectState.STATE_CONNECTED) {
            enableSend();
        } else {
            disableSend();
        }
        setSubtitle();
    }

    @Override
    public void onCustomerSupportMessage(CustomerMessage msg) {
        Log.i(TAG, "recv msg:" + msg.content);
        final ICustomerMessage imsg = new ICustomerMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;

        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.isSupport = true;
        imsg.isOutgoing = (msg.sellerID == currentUID);
        imsg.sender = msg.storeID;
        imsg.receiver = msg.customerID;

        imsg.setContent(msg.content);

        loadUserName(imsg);
        downloadMessageContent(imsg);

        insertMessage(imsg);
    }

    @Override
    public void onCustomerMessage(CustomerMessage msg) {
        Log.i(TAG, "recv msg:" + msg.content);
        final ICustomerMessage imsg = new ICustomerMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;

        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.isSupport = false;
        imsg.isOutgoing = false;
        imsg.sender = msg.customerID;
        imsg.receiver = msg.storeID;

        imsg.setContent(msg.content);

        loadUserName(imsg);
        downloadMessageContent(imsg);

        insertMessage(imsg);
    }

    @Override
    public void onCustomerMessageACK(CustomerMessage msg) {
        if (msg.customerAppID != customerAppID || msg.customerID != customerID) {
            return;
        }

        Log.i(TAG, "customer service message ack");
        IMessage imsg = findMessage(msg.msgLocalID);
        if (imsg == null) {
            Log.i(TAG, "can't find msg:" + msg.msgLocalID);
            return;
        }
        imsg.setAck(true);
    }

    @Override
    public void onCustomerMessageFailure(CustomerMessage msg) {
        if (msg.customerAppID != customerAppID || msg.customerID != customerID) {
            return;
        }

        Log.i(TAG, "message failure");

        IMessage imsg = findMessage(msg.msgLocalID);
        if (imsg == null) {
            Log.i(TAG, "can't find msg:" + msg.msgLocalID);
            return;
        }
        imsg.setFailure(true);
    }

    @Override
    protected void sendMessage(IMessage imsg) {
        if (imsg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
            CustomerSupportOutbox ob = CustomerSupportOutbox.getInstance();
            IMessage.Audio audio = (IMessage.Audio)imsg.content;
            imsg.setUploading(true);
            ob.uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
        } else if (imsg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
            IMessage.Image image = (IMessage.Image)imsg.content;
            //prefix:"file:"
            String path = image.image.substring(5);
            imsg.setUploading(true);
            CustomerSupportOutbox.getInstance().uploadImage(imsg, path);
        } else {
            ICustomerMessage cm = (ICustomerMessage)imsg;
            CustomerMessage msg = new CustomerMessage();

            msg.msgLocalID = cm.msgLocalID;
            msg.customerAppID = cm.customerAppID;
            msg.customerID = cm.customerID;
            msg.storeID = cm.storeID;
            msg.sellerID = cm.sellerID;
            msg.content = cm.content.getRaw();

            IMService im = IMService.getInstance();
            im.sendCustomerSupportMessage(msg);
        }
    }

    @Override
    protected void saveMessageAttachment(IMessage msg, String address) {
        ICustomerMessage attachment = new ICustomerMessage();
        attachment.content = IMessage.newAttachment(msg.msgLocalID, address);
        attachment.sender = msg.sender;
        attachment.receiver = msg.receiver;
        saveMessage(attachment);
    }

    @Override
    protected void saveMessage(IMessage imsg) {
        CustomerSupportMessageDB.getInstance().insertMessage(imsg);
    }

    @Override
    protected void markMessageFailure(IMessage imsg) {
        CustomerSupportMessageDB.getInstance().markMessageFailure(imsg.msgLocalID, customerID, customerAppID);
    }

    @Override
    protected void eraseMessageFailure(IMessage imsg) {
        CustomerSupportMessageDB.getInstance().eraseMessageFailure(imsg.msgLocalID, customerID, customerAppID);
    }

    @Override
    protected void clearConversation() {
        super.clearConversation();
        CustomerSupportMessageDB db = CustomerSupportMessageDB.getInstance();
        db.clearCoversation(this.customerID, this.customerAppID);

        NotificationCenter nc = NotificationCenter.defaultCenter();

        HashMap<String, Long>  o = new HashMap();
        o.put("appid", this.customerAppID);
        o.put("uid", this.customerID);
        Notification notification = new Notification(o, clearNotificationName);
        nc.postNotification(notification);

    }

    @Override
    public void onAudioUploadSuccess(IMessage msg, String url) {
        ICustomerMessage cm = (ICustomerMessage)msg;
        if (cm.customerAppID != this.customerAppID ||
                cm.customerID != this.customerID) {
            return;
        }
        Log.i(TAG, "audio upload success:" + url);

        IMessage m = findMessage(msg.msgLocalID);
        if (m != null) {
            m.setUploading(false);
        }
    }

    @Override
    public void onAudioUploadFail(IMessage msg) {
        ICustomerMessage cm = (ICustomerMessage)msg;
        if (cm.customerAppID != this.customerAppID ||
                cm.customerID != this.customerID) {
            return;
        }
        Log.i(TAG, "audio upload fail");

        IMessage m = findMessage(msg.msgLocalID);
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }
    }

    @Override
    public void onImageUploadSuccess(IMessage msg, String url) {
        ICustomerMessage cm = (ICustomerMessage)msg;
        if (cm.customerAppID != this.customerAppID ||
                cm.customerID != this.customerID) {
            return;
        }
        Log.i(TAG, "image upload success:" + url);

        IMessage m = findMessage(msg.msgLocalID);
        if (m != null) {
            m.setUploading(false);
        }
    }

    @Override
    public void onImageUploadFail(IMessage msg) {
        ICustomerMessage cm = (ICustomerMessage)msg;
        if (cm.customerAppID != this.customerAppID ||
                cm.customerID != this.customerID) {
            return;
        }
        Log.i(TAG, "image upload fail");

        IMessage m = findMessage(msg.msgLocalID);
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }
    }

    @Override
    public void onAudioDownloadSuccess(IMessage msg) {
        Log.i(TAG, "audio download success");
    }
    @Override
    public void onAudioDownloadFail(IMessage msg) {
        Log.i(TAG, "audio download fail");
    }


    @Override
    protected void onRobotClick() {
        Log.i(TAG, "on robot click");

        if (messages.size() == 0) {
            return;
        }

        String question = "";
        for (int i = messages.size() - 1; i > 0; i--) {
            IMessage m = messages.get(i);
            IMessage.MessageType msgType = m.content.getType();
            if (!m.isOutgoing & msgType == IMessage.MessageType.MESSAGE_TEXT) {
                question = ((IMessage.Text) m.content).text;
                break;
            }
        }

        if (TextUtils.isEmpty(question)) {
            return;
        }

        Intent intent = new Intent(this, RobotActivity.class);
        intent.putExtra("question", question);
        startActivityForResult(intent, ROBOT);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == ROBOT) {
            String answer = data.getExtras().getString("answer");
            if (!TextUtils.isEmpty(answer)) {
                sendTextMessage(answer);
            }
        }
    }


    protected void sendTextMessage(String text) {
        if (text.length() == 0) {
            return;
        }

        ICustomerMessage msg = new ICustomerMessage();
        msg.customerID = customerID;
        msg.customerAppID = customerAppID;
        msg.storeID = storeID;
        msg.sellerID = currentUID;

        msg.timestamp = now();
        msg.sender = currentUID;
        msg.receiver = storeID;

        msg.isSupport = true;
        msg.isOutgoing = true;

        msg.setContent(IMessage.newText(text));

        saveMessage(msg);
        sendMessage(msg);
        insertMessage(msg);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(msg, sendNotificationName);
        nc.postNotification(notification);
    }

    protected void sendImageMessage(Bitmap bmp) {
        double w = bmp.getWidth();
        double h = bmp.getHeight();
        double newHeight = 640.0;
        double newWidth = newHeight*w/h;


        Bitmap bigBMP = Bitmap.createScaledBitmap(bmp, (int)newWidth, (int)newHeight, true);

        double sw = 256.0;
        double sh = 256.0*h/w;

        Bitmap thumbnail = Bitmap.createScaledBitmap(bmp, (int)sw, (int)sh, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bigBMP.compress(Bitmap.CompressFormat.JPEG, 100, os);
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, os2);

        String originURL = localImageURL();
        String thumbURL = localImageURL();
        try {
            FileCache.getInstance().storeByteArray(originURL, os);
            FileCache.getInstance().storeByteArray(thumbURL, os2);

            String path = FileCache.getInstance().getCachedFilePath(originURL);
            String thumbPath = FileCache.getInstance().getCachedFilePath(thumbURL);

            String tpath = path + "@256w_256h_0c";
            File f = new File(thumbPath);
            File t = new File(tpath);
            f.renameTo(t);

            ICustomerMessage msg = new ICustomerMessage();
            msg.customerID = customerID;
            msg.customerAppID = customerAppID;
            msg.storeID = storeID;
            msg.sellerID = currentUID;
            msg.timestamp = now();
            msg.sender = currentUID;
            msg.receiver = storeID;

            msg.isSupport = true;
            msg.isOutgoing = true;

            msg.setContent(IMessage.newImage("file:" + path));

            saveMessage(msg);
            insertMessage(msg);
            sendMessage(msg);

            NotificationCenter nc = NotificationCenter.defaultCenter();
            Notification notification = new Notification(msg, sendNotificationName);
            nc.postNotification(notification);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected void sendAudioMessage() {
        String tfile = audioRecorder.getPathName();

        try {
            long mduration = AudioUtil.getAudioDuration(tfile);

            if (mduration < 1000) {
                Toast.makeText(this, "录音时间太短了", Toast.LENGTH_SHORT).show();
                return;
            }
            long duration = mduration/1000;

            String url = localAudioURL();

            ICustomerMessage msg = new ICustomerMessage();
            msg.customerID = customerID;
            msg.customerAppID = customerAppID;
            msg.storeID = storeID;
            msg.sellerID = currentUID;
            msg.timestamp = now();
            msg.sender = currentUID;
            msg.receiver = storeID;

            msg.isSupport = true;
            msg.isOutgoing = true;

            msg.setContent(IMessage.newAudio(url, duration));

            IMessage.Audio audio = (IMessage.Audio)msg.content;
            FileInputStream is = new FileInputStream(new File(tfile));
            Log.i(TAG, "store audio url:" + audio.url);
            FileCache.getInstance().storeFile(audio.url, is);

            saveMessage(msg);
            Log.i(TAG, "msg local id:" + msg.msgLocalID);
            insertMessage(msg);
            sendMessage(msg);

            NotificationCenter nc = NotificationCenter.defaultCenter();
            Notification notification = new Notification(msg, sendNotificationName);
            nc.postNotification(notification);

        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    protected void sendLocationMessage(float longitude, float latitude, String address) {

        ICustomerMessage msg = new ICustomerMessage();
        msg.customerID = customerID;
        msg.customerAppID = customerAppID;
        msg.storeID = storeID;
        msg.sellerID = currentUID;
        msg.timestamp = now();
        msg.sender = currentUID;
        msg.receiver = storeID;

        msg.isSupport = true;
        msg.isOutgoing = true;
        IMessage.Location loc = IMessage.newLocation(latitude, longitude);
        msg.setContent(loc);

        saveMessage(msg);

        loc.address = address;
        if (TextUtils.isEmpty(loc.address)) {
            queryLocation(msg);
        } else {
            saveMessageAttachment(msg, loc.address);
        }

        insertMessage(msg);
        sendMessage(msg);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(msg, sendNotificationName);
        nc.postNotification(notification);
    }
}
