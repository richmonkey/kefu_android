package com.beetle.kefu;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.support.v7.widget.Toolbar;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.db.Conversation;
import com.beetle.bauhinia.db.ConversationIterator;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.IMessage.GroupNotification;
import com.beetle.bauhinia.tools.NotificationCenter;
import com.beetle.im.CustomerMessage;
import com.beetle.im.CustomerMessageObserver;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.kefu.model.Token;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MessageListActivity extends MainActivity implements IMServiceObserver,
        CustomerMessageObserver,
        AdapterView.OnItemClickListener,
        NotificationCenter.NotificationCenterObserver {
    private static final String TAG = "beetle";

    private List<CustomerConversation> conversations;
    private ListView lv;
    protected long currentUID = 0;
    protected long storeID = 0;

    private BaseAdapter adapter;
    class ConversationAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return conversations.size();
        }
        @Override
        public Object getItem(int position) {
            return conversations.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ConversationView view = null;
            if (convertView == null) {
                view = new ConversationView(MessageListActivity.this);
            } else {
                view = (ConversationView)convertView;
            }
            Conversation c = conversations.get(position);
            view.setConversation(c);;
            return view;
        }
    }

    // 初始化组件
    private void initWidget() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.support_toolbar);
        setSupportActionBar(toolbar);

        lv = (ListView) findViewById(R.id.list);
        adapter = new ConversationAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_setting) {
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "main activity create...");

        setContentView(R.layout.activity_conversation);

        Token token = Token.getInstance();
        currentUID = token.uid;
        storeID = token.storeID;

        IMService im =  IMService.getInstance();
        im.addObserver(this);
        im.addCustomerServiceObserver(this);

        loadConversations();
        initWidget();

        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.addObserver(this, CustomerSupportMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, CustomerSupportMessageActivity.CLEAR_MESSAGES);

        Bus bus = BusCenter.getBus();
        bus.register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMService im =  IMService.getInstance();
        im.removeObserver(this);
        im.removeCustomerServiceObserver(this);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.removeObserver(this);
        BusCenter.getBus().unregister(this);
        Log.i(TAG, "message list activity destroyed");
    }


    @Subscribe
    public void onLogout(BusCenter.Logout e) {
        Log.i(TAG, "message list activity logout...");
        finish();
    }


    public  String messageContentToString(IMessage.MessageContent content) {
        if (content instanceof IMessage.Text) {
            return ((IMessage.Text) content).text;
        } else if (content instanceof IMessage.Image) {
            return "一张图片";
        } else if (content instanceof IMessage.Audio) {
            return "一段语音";
        } else if (content instanceof IMessage.GroupNotification) {
            return ((GroupNotification) content).description;
        } else if (content instanceof IMessage.Location) {
            return "一个地理位置";
        } else {
            return content.getRaw();
        }
    }

    void updateConversationDetail(Conversation conv) {
        String detail = messageContentToString(conv.message.content);
        conv.setDetail(detail);
    }

    void updateConversationName(Conversation conv) {
        if (conv.type == Conversation.CONVERSATION_CUSTOMER_SERVICE) {
            CustomerConversation cc = (CustomerConversation)conv;
            User u = getUser(cc.customerAppID, cc.customerID);
            if (TextUtils.isEmpty(u.name)) {
                conv.setName(u.identifier);
                final Conversation fconv = conv;
                asyncGetUser(cc.customerAppID, cc.customerID, new GetUserCallback() {
                    @Override
                    public void onUser(User u) {
                        fconv.setName(u.name);
                        fconv.setAvatar(u.avatarURL);
                    }
                });
            } else {
                conv.setName(u.name);
            }
            conv.setAvatar(u.avatarURL);
        }
    }

    void loadConversations() {
        conversations = new ArrayList<CustomerConversation>();

        ConversationIterator iter = CustomerSupportMessageDB.getInstance().newConversationIterator();
        while (true) {
            CustomerConversation conv = (CustomerConversation)iter.next();
            if (conv == null) {
                break;
            }
            if (conv.message == null) {
                continue;
            }
            updateConversationName(conv);
            updateConversationDetail(conv);
            conversations.add(conv);
        }

        Comparator<Conversation> cmp = new Comparator<Conversation>() {
            public int compare(Conversation c1, Conversation c2) {
                if (c1.message.timestamp > c2.message.timestamp) {
                    return -1;
                } else if (c1.message.timestamp == c2.message.timestamp) {
                    return 0;
                } else {
                    return 1;
                }

            }
        };
        Collections.sort(conversations, cmp);
    }

    public static class User {
        public long uid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }



    public interface GetUserCallback {
        void onUser(User u);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Conversation conv = conversations.get(position);
        Log.i(TAG, "conv:" + conv.getName());

        if (conv.type == Conversation.CONVERSATION_CUSTOMER_SERVICE) {
            onCustomerServiceClick(conv);
        }
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {

    }

    public int findConversationPosition(long appid, long uid) {
        for (int i = 0; i < conversations.size(); i++) {
            CustomerConversation conv = (CustomerConversation)conversations.get(i);
            if (conv.customerID == uid && conv.customerAppID == appid) {
                return i;
            }
        }
        return -1;
    }


    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    @Override
    public void onNotification(Notification notification) {
        if (notification.name.equals(CustomerSupportMessageActivity.SEND_MESSAGE_NAME)) {
            ICustomerMessage msg = (ICustomerMessage) notification.obj;

            int pos = findConversationPosition(msg.customerAppID, msg.customerID);
            CustomerConversation conversation = null;
            if (pos == -1) {
                conversation = new CustomerConversation();
                conversation.type = Conversation.CONVERSATION_CUSTOMER_SERVICE;
                conversation.cid = msg.customerID;
                conversation.customerAppID = msg.customerAppID;
                conversation.customerID = msg.customerID;
            } else {
                conversation = conversations.get(pos);
            }

            conversation.message = msg;
            updateConversationDetail(conversation);

            if (pos == -1) {
                conversations.add(0, conversation);
                adapter.notifyDataSetChanged();
            } else if (pos > 0){
                conversations.remove(pos);
                conversations.add(0, conversation);
                adapter.notifyDataSetChanged();
            } else {
                //pos == 0
            }
        } else if (notification.name.equals(CustomerSupportMessageActivity.CLEAR_MESSAGES)) {
            HashMap<String, Long> obj = (HashMap<String, Long> )notification.obj;
            long appid = obj.get("appid");
            long uid = obj.get("uid");
            int pos = findConversationPosition(appid, uid);
            if (pos != -1) {
                conversations.remove(pos);
                adapter.notifyDataSetChanged();
            }
        }
    }


    public boolean canBack() {
        return false;
    }

    protected User getUser(long appid, long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("匿名(%d)", uid);
        return u;
    }


    protected void asyncGetUser(long appid, long uid, GetUserCallback cb) {
        final long fuid = uid;
        final GetUserCallback fcb = cb;
        new AsyncTask<Void, Integer, User>() {
            @Override
            protected User doInBackground(Void... urls) {
                User u = new User();
                u.uid = fuid;
                u.name = String.format("匿名(%d)", fuid);
                u.avatarURL = "";
                u.identifier = String.format("匿名(%d)", fuid);
                return u;
            }
            @Override
            protected void onPostExecute(User result) {
                fcb.onUser(result);
            }
        }.execute();
    }

    protected void onCustomerServiceClick(Conversation conv) {
        ICustomerMessage msg = (ICustomerMessage)conv.message;

        Intent intent = new Intent(this, CustomerSupportMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra("customer_id", msg.customerID);
        intent.putExtra("customer_appid", msg.customerAppID);
        intent.putExtra("customer_name", conv.getName());
        intent.putExtra("store_id", storeID);
        intent.putExtra("current_uid", this.currentUID);

        startActivity(intent);
    }

    @Override
    public void onCustomerSupportMessage(CustomerMessage msg) {
        Log.i(TAG, "on customer support message");
        ICustomerMessage imsg = new ICustomerMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.isSupport = true;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;

        imsg.setContent(msg.content);

        int pos = findConversationPosition(msg.customerAppID, msg.customerID);
        CustomerConversation conversation = null;
        if (pos == -1) {
            conversation = new CustomerConversation();
            conversation.type = Conversation.CONVERSATION_CUSTOMER_SERVICE;
            conversation.cid = msg.customerID;
            conversation.customerAppID = msg.customerAppID;
            conversation.customerID = msg.customerID;
        } else {
            conversation = conversations.get(pos);
        }

        conversation.message = imsg;
        updateConversationName(conversation);
        updateConversationDetail(conversation);

        if (pos == -1) {
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else if (pos > 0) {
            conversations.remove(pos);
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else {
            //pos == 0
        }
    }
    @Override
    public void onCustomerMessage(CustomerMessage msg) {
        Log.i(TAG, "on customer message");
        ICustomerMessage imsg = new ICustomerMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.isSupport = false;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;

        imsg.setContent(msg.content);

        int pos = findConversationPosition(msg.customerAppID, msg.customerID);
        CustomerConversation conversation = null;
        if (pos == -1) {
            conversation = new CustomerConversation();
            conversation.type = Conversation.CONVERSATION_CUSTOMER_SERVICE;
            conversation.cid = msg.customerID;
            conversation.customerAppID = msg.customerAppID;
            conversation.customerID = msg.customerID;
        } else {
            conversation = conversations.get(pos);
        }

        conversation.message = imsg;
        updateConversationName(conversation);
        updateConversationDetail(conversation);

        if (pos == -1) {
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else if (pos > 0) {
            conversations.remove(pos);
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else {
            //pos == 0
        }
    }

    @Override
    public void onCustomerMessageACK(CustomerMessage msg) {
        Log.i(TAG, "on customer message ack");
    }

    @Override
    public void onCustomerMessageFailure(CustomerMessage msg) {
        Log.i(TAG, "on customer message failure");
    }
}
