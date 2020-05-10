package com.auroid.qrscanner.resultdb;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Result result);

    @Delete
    void delete(Result result);

    @Query("DELETE FROM result_table")
    void deleteAll();

    @Query("SELECT * FROM result_table ORDER BY mTime DESC")
    LiveData<List<Result>> getSortedResults();
}

