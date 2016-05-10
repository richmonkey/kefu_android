package com.beetle.kefu.api;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by houxh on 16/5/10.
 */
public interface Robot {
    @GET("/robot/answer")
    Observable<List<Question>> getSimilarQuestions(@Query("question") String question);

    class Question {
        public long id;
        public String question;
        public String answer;
    }
}
