package com.beetle.kefu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.beetle.bauhinia.db.Conversation;
import com.beetle.kefu.api.APIService;
import com.beetle.kefu.api.Authorization;
import com.beetle.kefu.api.Robot;
import com.beetle.kefu.model.Token;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class RobotActivity extends BaseActivity implements
        AdapterView.OnItemClickListener {

    private static final String TAG = "kefu";


    static class Question {
        public long id;
        public String question;
        public String answer;
    }

    String question;
    ArrayList<Question> questions;

    protected ActionBar actionBar;

    private BaseAdapter adapter;
    class QuestionAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return questions.size();
        }
        @Override
        public Object getItem(int position) {
            return questions.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(RobotActivity.this);
                view = inflater.inflate(R.layout.robot_question, null);
            } else {
                view = convertView;
            }

            Question q = questions.get(position);

            TextView t = (TextView)view.findViewById(R.id.question);
            t.setText(q.question);
            t = (TextView)view.findViewById(R.id.answer);
            t.setText(q.answer);;
            return view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot);

        Intent intent = getIntent();
        String question = intent.getStringExtra("question");
        if (TextUtils.isEmpty(question)) {
            Log.i(TAG, "question is empty");
            return;
        }
        Log.i(TAG, "question:" + question);

        this.question = question;
        questions = new ArrayList<>();

        ListView lv = (ListView) findViewById(R.id.list);
        adapter = new QuestionAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        Robot robot = APIService.getRobotService();
        robot.getSimilarQuestions(question).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Robot.Question>>() {
                    @Override
                    public void call(List<Robot.Question> questions) {

                        RobotActivity.this.questions = new ArrayList<Question>();
                        for (int i = 0; i < questions.size(); i++) {
                            Robot.Question q = questions.get(i);
                            Log.i(TAG, "answer:" + q.answer);
                            Question question = new Question();
                            question.id = q.id;
                            question.question = q.question;
                            question.answer = q.answer;
                            RobotActivity.this.questions.add(question);

                        }
                        adapter.notifyDataSetChanged();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "throwable:" + throwable);
                    }
                });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        final Question q = questions.get(position);
        Log.i(TAG, "answer:" + q.answer);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("确认发送");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent();
                intent.putExtra("answer", q.answer);
                RobotActivity.this.setResult(RESULT_OK, intent);
                RobotActivity.this.finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return false;
    }

}
