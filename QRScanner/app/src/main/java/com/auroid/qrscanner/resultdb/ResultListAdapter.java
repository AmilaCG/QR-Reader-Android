package com.auroid.qrscanner.resultdb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.auroid.qrscanner.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ResultListAdapter extends RecyclerView.Adapter<ResultListAdapter.ResultViewHolder> {

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView resultItemView;
        private final TextView timeItemView;

        private ResultViewHolder(View itemView) {
            super(itemView);
            resultItemView = itemView.findViewById(R.id.textViewResult);
            timeItemView = itemView.findViewById(R.id.textViewTime);
        }
    }

    private final LayoutInflater mInflater;
    private List<Result> mResults;

    private final SimpleDateFormat mFormatter =
            new SimpleDateFormat("EEE, d MMM yyyy, h:mm a", Locale.getDefault());

    public ResultListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new ResultViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        if (mResults != null) {
            Result current = mResults.get(position);
            holder.resultItemView.setText(current.getResult());
            holder.timeItemView.setText(mFormatter.format(current.getTime()));
        } else {
            holder.resultItemView.setText("No results");
        }
    }

    public void setResults(List<Result> results) {
        this.mResults = results;
        notifyDataSetChanged();
    }

    public Result getResultAt(int position) {
        return mResults.get(position);
    }

    @Override
    public int getItemCount() {
        if (mResults != null) {
            return mResults.size();
        } else {
            return 0;
        }
    }
}
