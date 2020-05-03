package com.amila.qrscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import com.amila.qrscanner.resultdb.Result;
import com.amila.qrscanner.resultdb.ResultListAdapter;
import com.amila.qrscanner.resultdb.ResultViewModel;

import java.util.Collections;
import java.util.List;

public class ScanHistory extends AppCompatActivity {

    private ResultViewModel mResultViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        final ResultListAdapter adapter = new ResultListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mResultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        mResultViewModel.getAllResults().observe(this, new Observer<List<Result>>() {
            @Override
            public void onChanged(List<Result> results) {
                Collections.reverse(results);
                adapter.setResults(results);
            }
        });
    }

    public void clearDb(View view) {
        mResultViewModel.clearAll();
    }

    public void back(View view) {
        finish();
    }
}
