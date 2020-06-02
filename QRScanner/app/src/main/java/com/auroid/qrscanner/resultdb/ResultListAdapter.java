package com.auroid.qrscanner.resultdb;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.auroid.qrscanner.ActionHandler;
import com.auroid.qrscanner.R;
import com.auroid.qrscanner.serializable.BarcodeWrapper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ResultListAdapter extends ListAdapter<Result, ResultListAdapter.ResultViewHolder> {

    private final SimpleDateFormat mFormatter =
            new SimpleDateFormat("EEE, d MMM yyyy, h:mm a", Locale.getDefault());

    private Gson mGson;

    private Context mContext;

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
        mContext = parent.getContext();
        View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.recyclerview_item, parent, false);
        return new ResultViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        Result current = getItem(position);
        BarcodeWrapper barcodeWrapper = mGson.fromJson(current.getResult(), BarcodeWrapper.class);
        int valueFormat = barcodeWrapper.valueFormat;

        holder.resultItemView.setText(barcodeWrapper.displayValue);
        holder.timeItemView.setText(mFormatter.format(current.getTime()));
        setIcon(barcodeWrapper.valueFormat, holder);

        holder.optionItemView.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(mContext, holder.optionItemView);

            inflateMenu(valueFormat, popupMenu, barcodeWrapper);
            ActionHandler actionHandler = new ActionHandler(mContext, barcodeWrapper);

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_item_more:
                        displayInfo(valueFormat, actionHandler, barcodeWrapper);
                        return true;

                    case R.id.menu_item_action:
                        runAction(valueFormat, actionHandler);
                        return true;

                    case R.id.menu_item_search:
                        actionHandler.webSearch();
                        return true;

                    case R.id.menu_item_copy:
                        actionHandler.copyToClipboard();
                        return true;

                    default:
                        return false;
                }
            });
            popupMenu.show();
        });
    }

    private void displayInfo(int resultType, ActionHandler actionHandler, BarcodeWrapper bcWrapper) {
        switch (resultType) {
            case FirebaseVisionBarcode.TYPE_URL:
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("URL / Text")
                        .setMessage(bcWrapper.rawValue)
                        .setIcon(R.drawable.ic_public_white_24dp)
                        .show();
                break;

            case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                LayoutInflater inflater =
                        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (inflater == null) return;
                View dialogView = inflater.inflate(R.layout.material_dialog_text, null);
                TextView textView = dialogView.findViewById(R.id.tv_material);
                textView.setText(actionHandler.getFormattedContactDetails());
                textView.setMovementMethod(LinkMovementMethod.getInstance());

                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("Contact")
                        .setView(dialogView)
                        .setIcon(R.drawable.ic_person_white_24dp)
                        .show();
                break;

            case FirebaseVisionBarcode.TYPE_CALENDAR_EVENT:
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("Calender Event")
                        .setMessage(actionHandler.getFormattedEventDetails())
                        .setIcon(R.drawable.ic_calender_white_24dp)
                        .show();
                break;
        }
    }

    private void runAction(int resultType, ActionHandler actionHandler) {
        switch (resultType) {
            case FirebaseVisionBarcode.TYPE_URL:
                actionHandler.openBrowser();
                break;

            case FirebaseVisionBarcode.TYPE_PHONE:
                actionHandler.openDialer();
                break;

            case FirebaseVisionBarcode.TYPE_GEO:
                actionHandler.openMaps();
                break;

            case FirebaseVisionBarcode.TYPE_CALENDAR_EVENT:
                actionHandler.addToCalender();
                break;

            case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                actionHandler.addToContacts();
                break;

            default:
                break;
        }
    }

    private void inflateMenu(int resultType, PopupMenu popupMenu, BarcodeWrapper bcWrapper) {
        switch(resultType) {
            case FirebaseVisionBarcode.TYPE_URL:
                if (bcWrapper.rawValue.equals(bcWrapper.displayValue)) {
                    popupMenu.inflate(R.menu.menu_result_web);
                } else {
                    popupMenu.inflate(R.menu.menu_result_web_ex);
                }
                break;

            case FirebaseVisionBarcode.TYPE_PHONE:
                popupMenu.inflate(R.menu.menu_result_phone);
                break;

            case FirebaseVisionBarcode.TYPE_GEO:
                popupMenu.inflate(R.menu.menu_result_geo);
                break;

            case FirebaseVisionBarcode.TYPE_CALENDAR_EVENT:
                popupMenu.inflate(R.menu.menu_result_event);
                break;

            case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                popupMenu.inflate(R.menu.menu_result_contact);
                break;

            default:
                popupMenu.inflate(R.menu.menu_result_default);
                break;
        }
    }

    private void setIcon(int resultType, ResultViewHolder holder) {
        switch(resultType) {
            case FirebaseVisionBarcode.TYPE_URL:
                holder.iconItemView.setImageResource(R.drawable.ic_public_white_24dp);
                break;

            case FirebaseVisionBarcode.TYPE_PHONE:
                holder.iconItemView.setImageResource(R.drawable.ic_phone_white_24dp);
                break;

            case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                holder.iconItemView.setImageResource(R.drawable.ic_person_white_24dp);
                break;

            case FirebaseVisionBarcode.TYPE_GEO:
                holder.iconItemView.setImageResource(R.drawable.ic_location_on_white_24dp);
                break;

            case FirebaseVisionBarcode.TYPE_CALENDAR_EVENT:
                holder.iconItemView.setImageResource(R.drawable.ic_calender_white_24dp);
                break;

            case FirebaseVisionBarcode.TYPE_WIFI:
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
        private final TextView optionItemView;

        private ResultViewHolder(View itemView) {
            super(itemView);
            resultItemView = itemView.findViewById(R.id.textViewResult);
            timeItemView = itemView.findViewById(R.id.textViewTime);
            iconItemView = itemView.findViewById(R.id.imgview_history);
            optionItemView = itemView.findViewById(R.id.textThreeDot);
        }
    }
}
