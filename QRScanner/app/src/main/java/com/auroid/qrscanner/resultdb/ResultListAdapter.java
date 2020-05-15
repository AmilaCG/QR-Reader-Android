package com.auroid.qrscanner.resultdb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.auroid.qrscanner.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ResultListAdapter extends ListAdapter<Result, ResultListAdapter.ResultViewHolder> {

    private final SimpleDateFormat mFormatter =
            new SimpleDateFormat("EEE, d MMM yyyy, h:mm a", Locale.getDefault());

    private static final DiffUtil.ItemCallback<Result> DIFF_CALLBACK = new DiffUtil.ItemCallback<Result>() {
        @Override
        public boolean areItemsTheSame(@NonNull Result oldItem, @NonNull Result newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Result oldItem, @NonNull Result newItem) {
            return oldItem.getResult().equals(newItem.getResult()) &&
                    oldItem.getTime().equals(newItem.getTime());
        }
    };

    public ResultListAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item, parent, false);
        return new ResultViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        Result current = getItem(position);
        holder.resultItemView.setText(current.getResult());
        holder.timeItemView.setText(mFormatter.format(current.getTime()));
    }

    public Result getResultAt(int position) {
        return getItem(position);
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView resultItemView;
        private final TextView timeItemView;

        private ResultViewHolder(View itemView) {
            super(itemView);
            resultItemView = itemView.findViewById(R.id.textViewResult);
            timeItemView = itemView.findViewById(R.id.textViewTime);
        }
    }
}
