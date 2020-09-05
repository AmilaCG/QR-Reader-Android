package com.auroid.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.auroid.qrscanner.resultdb.Result;
import com.auroid.qrscanner.resultdb.ResultListAdapter;
import com.auroid.qrscanner.resultdb.ResultViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class ScanHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ScanHistoryActivity";

    private ResultViewModel mResultViewModel;
    private Result mRecentlyDeletedItem;

    private boolean mIsHistoryEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView tvGuide = findViewById(R.id.text_guide);

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
            invalidateOptionsMenu();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_history, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem clearItem = menu.findItem(R.id.action_clear_all);
        clearItem.getIcon().setAlpha(mIsHistoryEmpty ? 64 : 255);
        clearItem.setEnabled(!mIsHistoryEmpty);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_all) {
            clearAll();
            return true;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        this.finish();
        return true;
    }

    public void clearAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.clear_history_confirmation)
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Do nothing
                })
                .setPositiveButton("OK", (dialog, which) -> {
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
