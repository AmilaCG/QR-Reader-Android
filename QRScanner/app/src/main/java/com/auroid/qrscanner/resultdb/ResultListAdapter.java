package com.auroid.qrscanner.resultdb;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.auroid.qrscanner.BarcodeResultActivity;
import com.auroid.qrscanner.R;
import com.auroid.qrscanner.ResultContent;
import com.auroid.qrscanner.serializable.BarcodeWrapper;

import com.google.mlkit.vision.barcode.common.Barcode;
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

        holder.resultItemView.setText(ResultContent.summary(barcodeWrapper));
        holder.timeItemView.setText(mFormatter.format(current.getTime()));
        setIcon(barcodeWrapper.valueFormat, holder);

        View.OnClickListener openDetails = v -> openResult(v.getContext(), current);
        holder.itemView.setOnClickListener(openDetails);
        holder.resultItemView.setOnClickListener(openDetails);
    }

    private void openResult(Context context, Result result) {
        context.startActivity(new Intent(context, BarcodeResultActivity.class)
                .putExtra("RESULT", result.getResult()));
    }

    private void setIcon(int resultType, ResultViewHolder holder) {
        holder.iconItemView.setImageTintList(android.content.res.ColorStateList.valueOf(
                holder.itemView.getContext().getColor(R.color.colorTinyIcon)));
        switch(resultType) {
            case Barcode.TYPE_URL:
                holder.iconItemView.setImageResource(R.drawable.ic_public_white_24dp);
                break;

            case Barcode.TYPE_PHONE:
                holder.iconItemView.setImageResource(R.drawable.ic_phone_white_24dp);
                break;

            case Barcode.TYPE_CONTACT_INFO:
                holder.iconItemView.setImageResource(R.drawable.ic_person_white_24dp);
                break;

            case Barcode.TYPE_GEO:
                holder.iconItemView.setImageResource(R.drawable.ic_location_on_white_24dp);
                break;

            case Barcode.TYPE_CALENDAR_EVENT:
                holder.iconItemView.setImageResource(R.drawable.ic_calender_white_24dp);
                break;

            case Barcode.TYPE_WIFI:
                holder.iconItemView.setImageResource(R.drawable.ic_wifi_white_24dp);
                break;

            case Barcode.TYPE_EMAIL:
                holder.iconItemView.setImageResource(R.drawable.ic_email);
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
