package com.beetle.kefu;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
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
import com.beetle.im.SystemMessageObserver;
import com.beetle.kefu.api.APIService;
import com.beetle.kefu.api.Authorization;
import com.beetle.kefu.model.Profile;
import com.beetle.kefu.model.User;
import com.beetle.kefu.api.Customer;
import com.beetle.kefu.model.ConversationDB;
import com.beetle.kefu.model.Token;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


public class MessageListActivity extends MainActivity implements IMServiceObserver,
        CustomerMessageObserver,
        SystemMessageObserver,
        AdapterView.OnItemClickListener,
        NotificationCenter.NotificationCenterObserver {
    private static final String TAG = "beetle";

    private List<CustomerConversation> conversations;
    private SwipeMenuListView lv;
    protected long currentUID = 0;
    protected long storeID = 0;

    private ConversationAdapter adapter;

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
        public int getViewTypeCount() {
            // menu type count
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            CustomerConversation conversation = conversations.get(position);
            if (!conversation.top) {
                return 0;
            } else {
                return 1;
            }
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

    private int dp2px(int dp) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float)dp, r.getDisplayMetrics());
        return (int)px;
    }
    // 初始化组件
    private void initWidget() {
        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem openItem = new SwipeMenuItem(
                        getApplicationContext());
                openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                openItem.setWidth(dp2px(90));
                if (menu.getViewType() == 0) {
                    openItem.setTitle("置顶");
                } else {
                    openItem.setTitle("取消置顶");
                }
                openItem.setTitleSize(18);
                openItem.setTitleColor(Color.WHITE);
                menu.addMenuItem(openItem);

                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                deleteItem.setWidth(dp2px(90));
                deleteItem.setTitle("Delete");
                deleteItem.setTitleSize(18);
                deleteItem.setTitleColor(Color.WHITE);

                menu.addMenuItem(deleteItem);
            }
        };

        lv = (SwipeMenuListView) findViewById(R.id.list);
        adapter = new ConversationAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
        lv.setMenuCreator(creator);
        lv.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        Log.i(TAG, "on top:" + position);
                        MessageListActivity.this.setConversationTop(position);
                        break;
                    case 1:
                        Log.i(TAG, "on delete:" + position);
                        MessageListActivity.this.deleteConversation(position);
                        break;
                }
                return false;
            }
        });
    }

    //置顶或者取消置顶
    private void setConversationTop(int position) {
        if (position < conversations.size() && position >= 0) {
            CustomerConversation conv = conversations.get(position);
            boolean top;
            if (conv.top) {
                top = false;
                ConversationDB.setTop(conv.customerAppID, conv.customerID, false);
            } else {
                top = true;
                ConversationDB.setTop(conv.customerAppID, conv.customerID, true);
            }

            if (top) {
                if (position > 0) {
                    conversations.remove(position);
                    conversations.add(0, conv);
                }
                conv.top = top;
            } else {
                //第一个非置顶的会话
                int index = -1;
                for (int i = 0; i < conversations.size(); i++) {
                    CustomerConversation cc = conversations.get(i);
                    if (!cc.top) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    index = conversations.size() - 1;
                } else {
                    index = index - 1;
                }
                if (position < index) {
                    conversations.remove(position);
                    conversations.add(index, conv);
                }
                conv.top = top;
            }
            adapter.notifyDataSetChanged();
        }
    }
    private void deleteConversation(int position) {
        if (position < conversations.size() && position >= 0) {
            CustomerConversation conv = conversations.get(position);
            CustomerSupportMessageDB.getInstance().clearCoversation(conv.customerID, conv.customerAppID);
            ConversationDB.setTop(conv.customerAppID, conv.customerID, false);
            ConversationDB.setNewCount(conv.customerAppID, conv.customerID, 0);
            conversations.remove(position);
            adapter.notifyDataSetChanged();
        }
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
        Profile profile = Profile.getInstance();

        currentUID = profile.uid;
        storeID = profile.storeID;

        IMService im =  IMService.getInstance();
        im.addObserver(this);
        im.addCustomerServiceObserver(this);
        im.addSystemObserver(this);

        loadConversations();
        initWidget();

        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.addObserver(this, CustomerSupportMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, CustomerSupportMessageActivity.CLEAR_MESSAGES);
        nc.addObserver(this, CustomerSupportMessageActivity.CLEAR_NEW_MESSAGES);

        nc.addObserver(this, XWMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, XWMessageActivity.CLEAR_NEW_MESSAGES);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMService im =  IMService.getInstance();
        im.removeObserver(this);
        im.removeCustomerServiceObserver(this);
        im.removeSystemObserver(this);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.removeObserver(this);

        Log.i(TAG, "message list activity destroyed");
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
            if (cc.isXiaoWei) {
                conv.setName(getResources().getString(R.string.xiaowei));
                conv.setAvatar("");
            } else {
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
                    //超过一天,从服务器更新用户名
                    if (now() - u.timestamp > 24 * 3600) {
                        final Conversation fconv = conv;
                        asyncGetUser(cc.customerAppID, cc.customerID, new GetUserCallback() {
                            @Override
                            public void onUser(User u) {
                                fconv.setName(u.name);
                                fconv.setAvatar(u.avatarURL);
                            }
                        });
                    }
                }
                conv.setAvatar(u.avatarURL);
            }
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
            conv.isXiaoWei = (conv.customerID == this.currentUID && conv.customerAppID == Config.XIAOWEI_APPID);
            conv.top = ConversationDB.getTop(conv.customerAppID, conv.customerID);
            int unread = ConversationDB.getNewCount(conv.customerAppID, conv.customerID);
            conv.setUnreadCount(unread);
            updateConversationName(conv);
            updateConversationDetail(conv);
            conversations.add(conv);
        }

        Comparator<Conversation> cmp = new Comparator<Conversation>() {
            public int compare(Conversation c1, Conversation c2) {

                CustomerConversation cc1 = (CustomerConversation)c1;
                CustomerConversation cc2 = (CustomerConversation)c2;

                long top1 = cc1.top ? 1 : 0;
                long top2 = cc2.top ? 1 : 0;

                long t1 = top1 << 32 | c1.message.timestamp;
                long t2 = top2 << 32 | c2.message.timestamp;

                if (t1 > t2) {
                    return -1;
                } else if (t1 == t2) {
                    return 0;
                } else {
                    return 1;
                }

            }
        };
        Collections.sort(conversations, cmp);
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
            CustomerConversation cc = (CustomerConversation)conv;
            if (cc.isXiaoWei) {
                onXiaoWeiClick(conv);
            } else {
                onCustomerServiceClick(conv);
            }
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


    private void onNewCustomerMessage(ICustomerMessage msg) {
        int pos = findConversationPosition(msg.customerAppID, msg.customerID);
        CustomerConversation conversation = null;
        if (pos == -1) {
            conversation = new CustomerConversation();
            conversation.type = Conversation.CONVERSATION_CUSTOMER_SERVICE;
            conversation.cid = msg.customerID;
            conversation.customerAppID = msg.customerAppID;
            conversation.customerID = msg.customerID;
            conversation.isXiaoWei = (conversation.customerID == this.currentUID &&
                                        conversation.customerAppID == Config.XIAOWEI_APPID);
        } else {
            conversation = conversations.get(pos);
        }

        if (!msg.isOutgoing) {
            int unread = conversation.getUnreadCount() + 1;
            conversation.setUnreadCount(unread);
            ConversationDB.setNewCount(conversation.customerAppID, conversation.customerID, unread);
        }

        conversation.message = msg;
        updateConversationName(conversation);
        updateConversationDetail(conversation);

        if (conversation.top) {
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
        } else {
            //第一个非置顶的会话
            int index = -1;
            for (int i = 0; i < conversations.size(); i++) {
                CustomerConversation cc = conversations.get(i);
                if (!cc.top) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                index = conversations.size();
            }

            if (pos == -1) {
                conversations.add(index, conversation);
                adapter.notifyDataSetChanged();
            } else if (pos > 0 && index < pos) {
                //向上移动
                conversations.remove(pos);
                conversations.add(index, conversation);
                adapter.notifyDataSetChanged();
            } else {
                //pos == 0
            }
        }
    }

    private void clearCustomerNewState(long uid, long appid) {

        int pos = findConversationPosition(appid, uid);
        if (pos != -1) {
            CustomerConversation cc = conversations.get(pos);
            cc.setUnreadCount(0);
            ConversationDB.setNewCount(appid, uid, 0);
        }
    }

    @Override
    public void onNotification(Notification notification) {
        if (notification.name.equals(CustomerSupportMessageActivity.SEND_MESSAGE_NAME)) {
            ICustomerMessage msg = (ICustomerMessage) notification.obj;
            onNewCustomerMessage(msg);
        } else if (notification.name.equals(CustomerSupportMessageActivity.CLEAR_MESSAGES)) {
            HashMap<String, Long> obj = (HashMap<String, Long> )notification.obj;
            long appid = obj.get("appid");
            long uid = obj.get("uid");
            int pos = findConversationPosition(appid, uid);
            if (pos != -1) {
                conversations.remove(pos);
                adapter.notifyDataSetChanged();
            }
        } else if (notification.name.equals(CustomerSupportMessageActivity.CLEAR_NEW_MESSAGES)) {
            HashMap<String, Long> obj = (HashMap<String, Long>) notification.obj;
            long appid = obj.get("appid");
            long uid = obj.get("uid");
            clearCustomerNewState(uid, appid);
        } else if (notification.name.equals(XWMessageActivity.SEND_MESSAGE_NAME)) {
            ICustomerMessage msg = (ICustomerMessage) notification.obj;
            onNewCustomerMessage(msg);
        } else if (notification.name.equals(XWMessageActivity.CLEAR_NEW_MESSAGES)) {
            HashMap<String, Long> obj = (HashMap<String, Long>) notification.obj;
            long appid = obj.get("appid");
            long uid = obj.get("uid");
            clearCustomerNewState(uid, appid);
        }
    }

    protected User getUser(long appid, long uid) {
        User u = User.load(appid, uid);
        if (u == null) {
            u = new User();
            u.appID = appid;
            u.uid = uid;
            u.name = null;
            u.avatarURL = "";
            u.identifier = String.format("匿名(%d)", uid);
        } else if (TextUtils.isEmpty(u.name)){
            u.identifier = String.format("匿名(%d)", uid);
        }
        return u;
    }


    protected void asyncGetUser(final long appid, final long uid, final GetUserCallback cb) {
        Customer api = APIService.getCustomerService();

        api.getCustomer(appid, uid).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Customer.User>() {
                    @Override
                    public void call(Customer.User user) {
                        Log.i(TAG, "user name:" + user.name);
                        User u = new User();
                        u.appID = appid;
                        u.uid = uid;
                        u.name = user.name;
                        u.timestamp = now();
                        if (!TextUtils.isEmpty(u.name)) {
                            User.save(u);
                            cb.onUser(u);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "get customer error:" + throwable);
                    }
                });
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

    protected void onXiaoWeiClick(Conversation conv) {
        ICustomerMessage msg = (ICustomerMessage)conv.message;

        Intent intent = new Intent(this, XWMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra("title", getResources().getString(R.string.xiaowei));
        intent.putExtra("store_id", Config.XIAOWEI_STORE_ID);
        intent.putExtra("seller_id", (long)(0));
        intent.putExtra("current_uid", this.currentUID);
        intent.putExtra("app_id", Config.XIAOWEI_APPID);

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
        imsg.isOutgoing = (msg.sellerID == this.currentUID);

        imsg.setContent(msg.content);

        onNewCustomerMessage(imsg);
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
        imsg.isOutgoing = (imsg.customerAppID == Config.XIAOWEI_APPID && imsg.customerID == this.currentUID);

        imsg.setContent(msg.content);

        onNewCustomerMessage(imsg);
    }

    @Override
    public void onCustomerMessageACK(CustomerMessage msg) {
        Log.i(TAG, "on customer message ack");
    }

    @Override
    public void onCustomerMessageFailure(CustomerMessage msg) {
        Log.i(TAG, "on customer message failure");
    }

    @Override
    public void onSystemMessage(String sm) {
        Gson gson = new GsonBuilder().create();
        JsonObject element = gson.fromJson(sm, JsonObject.class);

        if (!element.has("login")) {
            return;
        }

        JsonObject obj = element.getAsJsonObject("login");
        int platform = 0;
        int timestamp = 0;
        String deviceID = "";
        String deviceName = "";
        if (obj.has("platform")) {
            platform = obj.getAsJsonPrimitive("platform").getAsInt();
        }
        if (obj.has("timestamp")) {
            timestamp = obj.getAsJsonPrimitive("timestamp").getAsInt();
        }

        if (obj.has("device_id")) {
            deviceID = obj.getAsJsonPrimitive("device_id").getAsString();
        }

        if (obj.has("device_name")) {
            deviceName = obj.getAsJsonPrimitive("device_name").getAsString();
        }

        Token t = Token.getInstance();
        Profile profile = Profile.getInstance();
        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (platform == Authorization.PLATFORM_ANDROID && deviceID.equals(androidID)) {
            return;
        }

        if (profile.loginTimestamp > timestamp) {
            return;
        }

        Log.i(TAG, "login another place");
        logout();
    }

    void logout() {
        Token t = Token.getInstance();
        t.accessToken = "";
        t.refreshToken = "";
        t.expireTimestamp = 0;
        t.save(this);

        Profile profile = Profile.getInstance();
        profile.uid = 0;
        profile.storeID = 0;
        profile.status = "";
        profile.name = "";
        profile.avatar = "";
        profile.loginTimestamp = 0;
        profile.keepalive = false;
        profile.save(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("hint", true);
        startActivity(intent);

        Bus bus = BusCenter.getBus();
        bus.post(new BusCenter.Logout());

        finish();
    }
}
