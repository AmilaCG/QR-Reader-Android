package com.auroid.qrscanner.resultdb;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "result_table")
public class Result {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "result")
    private String mResult;

    public Result(@NonNull String result) {
        this.mResult = result;
    }

    public String getResult() {
        return this.mResult;
    }
}
