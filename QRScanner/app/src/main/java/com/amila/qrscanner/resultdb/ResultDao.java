package com.amila.qrscanner.resultdb;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Result result);

    @Query("DELETE FROM result_table")
    void deleteAll();

    @Query("SELECT * FROM result_table")
    LiveData<List<Result>> getSortedResults();
}

