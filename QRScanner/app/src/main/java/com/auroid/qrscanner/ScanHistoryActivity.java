package com.auroid.qrscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.auroid.qrscanner.resultdb.Result;
import com.auroid.qrscanner.resultdb.ResultListAdapter;
import com.auroid.qrscanner.resultdb.ResultViewModel;

import java.util.Collections;
import java.util.List;

public class ScanHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ScanHistoryActivity";

    private ResultViewModel mResultViewModel;
    private List<Result> mResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        mResults = null;

        RecyclerView recyclerView = findViewById(R.id.recyclerview);

        RecyclerView.ItemDecoration dividerItemDecoration =
                new DividerItemDecorator(ContextCompat.getDrawable(this, R.drawable.line_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

        final ResultListAdapter adapter = new ResultListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mResultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        mResultViewModel.getAllResults().observe(this, new Observer<List<Result>>() {
            @Override
            public void onChanged(List<Result> results) {
                mResults = results;
                Collections.reverse(mResults);
                adapter.setResults(mResults);
            }
        });
    }

    public void clearDb(View view) {
        if (mResults == null) {
            Log.e(TAG, "clearDb: mResults is null");
            return;
        }

        if (mResults.size() == 0) {
            DialogInterface.OnClickListener listener = (dialog, id) -> finish();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("QR Code Reader")
                    .setMessage(R.string.empty_scan_history)
                    .setPositiveButton("OK", listener)
                    .show();
        } else {
            DialogInterface.OnClickListener listener = (dialog, id) -> {
                if (id == DialogInterface.BUTTON_POSITIVE) {
                    mResultViewModel.clearAll();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("QR Code Reader")
                    .setMessage(R.string.clear_history_confirmation)
                    .setPositiveButton("OK", listener)
                    .setNegativeButton("Cancel", listener)
                    .show();
        }
    }

    public void back(View view) {
        finish();
    }
}
