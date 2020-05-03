package com.amila.qrscanner.resultdb;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Result.class}, version = 1, exportSchema = false)
public abstract class ResultRoomDatabase extends RoomDatabase {

    public abstract ResultDao resultDao();

    private static volatile ResultRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static ResultRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ResultRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ResultRoomDatabase.class, "result_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
