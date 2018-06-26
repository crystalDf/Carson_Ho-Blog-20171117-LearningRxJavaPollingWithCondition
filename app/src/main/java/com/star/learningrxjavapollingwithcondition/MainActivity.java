package com.star.learningrxjavapollingwithcondition;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxJava";

    private static final String BASE_URL = "https://api.github.com";
    public static final String PATH = "/repos/{owner}/{repo}/contributors";

    private static final String OWNER = "square";
    private static final String REPO = "retrofit";

    private int mPollingTimes = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a very simple REST adapter which points the GitHub API.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        // Create an instance of our GitHub API interface.
        GitHub github = retrofit.create(GitHub.class);

        // Create an observable instance for looking up Retrofit contributors.
        Observable<List<Contributor>> observable = github.contributors(OWNER, REPO);

        observable
                .repeatWhen(objectObservable -> objectObservable.flatMap(
                        (Function<Object, ObservableSource<?>>) o ->
                                Observable.just(1).delay(3, TimeUnit.SECONDS)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Contributor>>() {

                    private Disposable mDisposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "开始采用subscribe连接");

                        mDisposable = d;
                    }

                    @Override
                    public void onNext(List<Contributor> contributors) {

                        Log.d(TAG, "第 " + mPollingTimes + " 次轮询");

                        for (Contributor contributor : contributors) {
                            Log.d(TAG, contributor.login + " (" + contributor.contributions + ")");
                        }

                        mPollingTimes++;

                        if (mPollingTimes > 3) {
                            mDisposable.dispose();

                            Log.d(TAG, "已经切断了连接：" + mDisposable.isDisposed());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "对Error事件作出响应");
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "对Complete事件作出响应");
                    }
                });
    }
}
