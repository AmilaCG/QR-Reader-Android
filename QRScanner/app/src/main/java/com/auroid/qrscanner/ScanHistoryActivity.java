package com.auroid.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.auroid.qrscanner.resultdb.Result;
import com.auroid.qrscanner.resultdb.ResultListAdapter;
import com.auroid.qrscanner.resultdb.ResultViewModel;
import com.google.android.material.snackbar.Snackbar;

public class ScanHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ScanHistoryActivity";

    private ResultViewModel mResultViewModel;

    private Result mRecentlyDeletedItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        Button btnClear = findViewById(R.id.clear_db);
        TextView tvGuide = findViewById(R.id.text_guide);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);

        final ResultListAdapter adapter = new ResultListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mResultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        mResultViewModel.getAllResults().observe(this, results -> {
            adapter.submitList(results);

            if (results.size() == 0) {
                btnClear.setEnabled(false);
                tvGuide.setText(getString(R.string.empty_scan_history));
            } else {
                btnClear.setEnabled(true);
                tvGuide.setText(getString(R.string.delete_guide));
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Result selectedItem = adapter.getResultAt(viewHolder.getAdapterPosition());

                mResultViewModel.delete(selectedItem);

                mRecentlyDeletedItem = selectedItem;
                showUndoSnackbar();
            }
        }).attachToRecyclerView(recyclerView);
    }

    public void clearAll(View view) {
        DialogInterface.OnClickListener listener = (dialog, id) -> {
            if (id == DialogInterface.BUTTON_POSITIVE) {
                mResultViewModel.clearAll();
                Toast.makeText(this, getString(R.string.confirm_clear_scan_history),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR Code Reader")
                .setMessage(R.string.clear_history_confirmation)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", listener)
                .show();
    }

    public void back(View view) {
        finish();
    }

    private void showUndoSnackbar() {
        View view = findViewById(R.id.scan_history_layout);
        Snackbar snackbar = Snackbar.make(view, R.string.confirm_result_delete,
                Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", v -> mResultViewModel.insert(mRecentlyDeletedItem));
        snackbar.show();
    }
}
