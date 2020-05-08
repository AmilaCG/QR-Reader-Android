package com.auroid.qrscanner.resultdb;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ResultRepository {

    private ResultDao mResultDao;
    private LiveData<List<Result>> mAllResults;

    ResultRepository(Application application) {
        ResultRoomDatabase db = ResultRoomDatabase.getDatabase(application);
        mResultDao = db.resultDao();
        mAllResults = mResultDao.getSortedResults();
    }

    LiveData<List<Result>> getAllResults() {
        return mAllResults;
    }

    void insert(Result result) {
        ResultRoomDatabase.databaseWriteExecutor.execute(() -> {
            mResultDao.insert(result);
        });
    }

    void clearAll() {
        ResultRoomDatabase.databaseWriteExecutor.execute(() -> {
            mResultDao.deleteAll();
        });
    }
}
