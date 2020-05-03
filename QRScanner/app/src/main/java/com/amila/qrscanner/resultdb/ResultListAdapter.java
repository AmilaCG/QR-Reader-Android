package com.amila.qrscanner.resultdb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amila.qrscanner.R;

import java.util.List;

public class ResultListAdapter extends RecyclerView.Adapter<ResultListAdapter.ResultViewHolder> {

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView resultItemView;

        private ResultViewHolder(View itemView) {
            super(itemView);
            resultItemView = itemView.findViewById(R.id.textView);
        }
    }

    private final LayoutInflater mInflater;
    private List<Result> mResults;

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
        } else {
            holder.resultItemView.setText("No results");
        }
    }

    public void setResults(List<Result> results) {
        mResults = results;
        notifyDataSetChanged();
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
