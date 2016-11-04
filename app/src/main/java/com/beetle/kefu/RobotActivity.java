package com.beetle.kefu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;


import com.beetle.kefu.api.APIService;
import com.beetle.kefu.api.Robot;

import java.util.ArrayList;
import java.util.List;


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
        if (question == null) {
            question = "";
        }

        Log.i(TAG, "question:" + question);
        questions = new ArrayList<>();

        ListView lv = (ListView) findViewById(R.id.list);
        adapter = new QuestionAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        final SearchView searchView = (SearchView)findViewById(R.id.search_view);
        searchView.setBackgroundColor(Color.GRAY);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String queryText) {
                Log.d(TAG, "onQueryTextChange = " + queryText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String queryText) {
                Log.d(TAG, "onQueryTextSubmit = " + queryText);

                InputMethodManager imm = (InputMethodManager) getSystemService(RobotActivity.this.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                }
                searchView.clearFocus();


                if (!TextUtils.isEmpty(queryText)) {
                    RobotActivity.this.findQuestion(queryText);
                }
                return true;
            }
        });

        if (!TextUtils.isEmpty(question)) {
            searchView.setQuery(question, false);
            findQuestion(question);
        }
    }

    @Override
    public void onBackPressed() {
        final SearchView searchView = (SearchView)findViewById(R.id.search_view);

        InputMethodManager imm = (InputMethodManager) getSystemService(RobotActivity.this.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }
        searchView.clearFocus();

        super.onBackPressed();
    }

    private void findQuestion(String question) {
        Robot robot = APIService.getRobotService();
        robot.getSimilarQuestions(question).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Robot.Question>>() {
                    @Override
                    public void call(List<Robot.Question> questions) {
                        if (questions == null) {
                            Toast.makeText(getApplicationContext(), "没有找到相似问题", Toast.LENGTH_SHORT).show();
                            RobotActivity.this.questions = new ArrayList<Question>();
                            adapter.notifyDataSetChanged();
                            return;
                        }
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

    @Override
    protected void onPause() {
        super.onPause();

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
