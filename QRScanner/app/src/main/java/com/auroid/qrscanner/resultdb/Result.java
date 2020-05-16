package com.auroid.qrscanner.resultdb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "result_table")
public class Result {

    @PrimaryKey(autoGenerate = true)
    private int mId;

    private String mResult;

    private Date mTime;

    public Result(String result, Date time) {
        this.mResult = result;
        this.mTime = time;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public int getId() {
        return mId;
    }

    public String getResult() {
        return mResult;
    }

    public Date getTime() {
        return mTime;
    }
}
