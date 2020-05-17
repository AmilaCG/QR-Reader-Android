package com.auroid.qrscanner.resultdb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.auroid.qrscanner.R;
import com.auroid.qrscanner.serializable.BarcodeWrapper;

import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ResultListAdapter extends ListAdapter<Result, ResultListAdapter.ResultViewHolder> {

    private final SimpleDateFormat mFormatter =
            new SimpleDateFormat("EEE, d MMM yyyy, h:mm a", Locale.getDefault());

    private Gson mGson;

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
        mGson = new Gson();
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
        BarcodeWrapper barcodeWrapper = mGson.fromJson(current.getResult(), BarcodeWrapper.class);
        holder.resultItemView.setText(barcodeWrapper.displayValue);
        holder.timeItemView.setText(mFormatter.format(current.getTime()));
        setIcon(barcodeWrapper.valueFormat, holder);
    }

    private void setIcon(int resultType, ResultViewHolder holder) {
        switch(resultType) {
            case Barcode.URL:
                holder.iconItemView.setImageResource(R.drawable.ic_public_white_24dp);
                break;

            case Barcode.PHONE:
                holder.iconItemView.setImageResource(R.drawable.ic_phone_white_24dp);
                break;

            case Barcode.GEO:
                holder.iconItemView.setImageResource(R.drawable.ic_location_on_white_24dp);
                break;

            case Barcode.CALENDAR_EVENT:
                holder.iconItemView.setImageResource(R.drawable.ic_calender_white_24dp);
                break;

            case Barcode.WIFI:
                holder.iconItemView.setImageResource(R.drawable.ic_wifi_white_24dp);
                break;

            default:
                holder.iconItemView.setImageResource(R.drawable.ic_text_white_24dp);
                break;
        }
    }

    public Result getResultAt(int position) {
        return getItem(position);
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView resultItemView;
        private final TextView timeItemView;
        private final ImageView iconItemView;

        private ResultViewHolder(View itemView) {
            super(itemView);
            resultItemView = itemView.findViewById(R.id.textViewResult);
            timeItemView = itemView.findViewById(R.id.textViewTime);
            iconItemView = itemView.findViewById(R.id.imgview_history);
        }
    }
}
