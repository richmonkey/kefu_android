package com.beetle.kefu;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.beetle.bauhinia.db.Conversation;
import com.beetle.bauhinia.db.IMessage;

import com.squareup.picasso.Picasso;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ConversationView extends FrameLayout implements PropertyChangeListener {
    protected Context context;
    protected LayoutInflater inflater;
    private Conversation conversation = null;

    public ConversationView(Context context) {
        super(context);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.conversation_message, this);
    }

    public void setConversation(Conversation c) {
        if (this.conversation != null) {
            this.conversation.removePropertyChangeListener(this);
        }
        this.conversation = c;
        this.conversation.addPropertyChangeListener(this);


        CustomerConversation cc = (CustomerConversation)c;
        if (cc.top) {
            setBackgroundColor(getResources().getColor(R.color.top));
        } else {
            setBackgroundColor(Color.WHITE);
        }

        TextView tv = (TextView) this.findViewById(R.id.name);
        tv.setText(c.getName());

        tv = (TextView)this.findViewById(R.id.content);
        tv.setText(c.getDetail());

        String ts = TimeUtil.formatTimeBase(c.message.timestamp);
        tv = (TextView)findViewById(R.id.timestamp);
        tv.setText(ts);

        int placeholder;
        if (c.type == Conversation.CONVERSATION_PEER) {
            placeholder = R.drawable.avatar_contact;
        } else if (c.type == Conversation.CONVERSATION_GROUP){
            placeholder = R.drawable.avatar_group;
        } else if (c.type == Conversation.CONVERSATION_CUSTOMER_SERVICE){
            placeholder = R.drawable.avatar_contact;
        } else {
            placeholder = R.drawable.avatar_contact;
        }

        String avatar = null;
        if (!TextUtils.isEmpty(c.getAvatar())) {
            avatar = c.getAvatar();
        }

        ImageView imageView = (ImageView) this.findViewById(R.id.header);
        Picasso.with(context)
                .load(avatar)
                .placeholder(placeholder)
                .into(imageView);

        setUnreadCount();
    }


    private void setUnreadCount() {
        TextView tv = (TextView) this.findViewById(R.id.unReadCount);
        if (conversation.getUnreadCount() > 0) {
            tv.setVisibility(VISIBLE);
            tv.setText(String.valueOf(conversation.getUnreadCount()));
        } else {
            tv.setVisibility(GONE);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event){
        if (event.getPropertyName().equals("detail")) {
            TextView tv = (TextView)this.findViewById(R.id.content);
            tv.setText(this.conversation.getDetail());
        } else if (event.getPropertyName().equals("name")) {
            TextView tv = (TextView) this.findViewById(R.id.name);
            tv.setText(this.conversation.getName());
        } else if (event.getPropertyName().equals("avatar")) {
            int placeholder;
            if (conversation.type == Conversation.CONVERSATION_PEER) {
                placeholder = R.drawable.avatar_contact;
            } else if (conversation.type == Conversation.CONVERSATION_GROUP){
                placeholder = R.drawable.avatar_group;
            } else if (conversation.type == Conversation.CONVERSATION_CUSTOMER_SERVICE){
                CustomerConversation cc = (CustomerConversation)conversation;
                if (cc.isXiaoWei) {
                    placeholder = R.drawable.xiaowei;
                } else {
                    placeholder = R.drawable.avatar_contact;
                }
            } else {
                placeholder = R.drawable.avatar_contact;
            }

            String avatar = null;
            if (!TextUtils.isEmpty(this.conversation.getAvatar())) {
                avatar = this.conversation.getAvatar();
            }

            ImageView imageView = (ImageView) this.findViewById(R.id.header);
            Picasso.with(context)
                    .load(avatar)
                    .placeholder(placeholder)
                    .into(imageView);
        } else if (event.getPropertyName().equals("unreadCount")) {
            setUnreadCount();
        }
    }
}