package com.auroid.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.auroid.qrscanner.resultdb.Result;
import com.auroid.qrscanner.resultdb.ResultListAdapter;
import com.auroid.qrscanner.resultdb.ResultViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class ScanHistoryActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ScanHistoryActivity";

    private ResultViewModel mResultViewModel;
    private Result mRecentlyDeletedItem;

    private boolean mIsHistoryEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        TextView tvGuide = findViewById(R.id.text_guide);

        findViewById(R.id.back_button).setOnClickListener(this);
        ((TextView) findViewById(R.id.top_action_title))
                .setText(R.string.activity_label_scan_history);
        ImageView topActionButton = findViewById(R.id.top_action_button);
        topActionButton.setOnClickListener(this);
        topActionButton.setImageResource(R.drawable.ic_baseline_delete_24);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);

        final ResultListAdapter adapter = new ResultListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mResultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        mResultViewModel.getAllResults().observe(this, results -> {
            adapter.submitList(results);

            if (results.size() == 0) {
                mIsHistoryEmpty = true;
                tvGuide.setText(getString(R.string.empty_scan_history));
            } else {
                mIsHistoryEmpty = false;
                tvGuide.setText(getString(R.string.delete_guide));
            }
            setupDeleteButton(topActionButton);
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back_button) {
            onBackPressed();
        } else if (id == R.id.top_action_button) {
            clearAll();
        }
    }

    private void setupDeleteButton(ImageView deleteButton) {
        deleteButton.setImageAlpha(mIsHistoryEmpty ? 64 : 255);
        deleteButton.setEnabled(!mIsHistoryEmpty);
    }

    public void clearAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.clear_history_confirmation)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    // Do nothing
                })
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    mResultViewModel.clearAll();
                    Toast.makeText(this, R.string.confirm_clear_scan_history,
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    private void showUndoSnackbar() {
        View view = findViewById(R.id.scan_history_layout);
        Snackbar snackbar = Snackbar.make(view, R.string.confirm_result_delete,
                Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", v -> mResultViewModel.insert(mRecentlyDeletedItem));
        snackbar.show();
    }
}
