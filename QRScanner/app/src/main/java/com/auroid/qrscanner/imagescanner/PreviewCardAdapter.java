package com.auroid.qrscanner.imagescanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.auroid.qrscanner.R;
import com.google.mlkit.vision.barcode.Barcode;

import java.util.List;

public class PreviewCardAdapter extends RecyclerView.Adapter<PreviewCardAdapter.CardViewHolder> {

    /** Listens to user's interaction with the preview card item. */
    public interface CardItemListener {
        void onPreviewCardClicked(DetectedBarcode detectedBarcode);
    }

    private final List<DetectedBarcode> searchedObjectList;
    private final CardItemListener cardItemListener;

    public PreviewCardAdapter(
            List<DetectedBarcode> searchedObjectList, CardItemListener cardItemListener) {
        this.searchedObjectList = searchedObjectList;
        this.cardItemListener = cardItemListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.barcodes_preview_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        DetectedBarcode detectedBarcode = searchedObjectList.get(position);
        holder.bindProducts(detectedBarcode);
        holder.itemView.setOnClickListener(v -> cardItemListener.onPreviewCardClicked(detectedBarcode));
    }

    @Override
    public int getItemCount() {
        return searchedObjectList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView titleView;
        private final TextView subtitleView;

        private CardViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.card_image);
            titleView = itemView.findViewById(R.id.card_title);
            subtitleView = itemView.findViewById(R.id.card_subtitle);
        }

        private void bindProducts(DetectedBarcode detectedBarcode) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(null);

            setIcon(detectedBarcode.getBarcode().getValueType(), imageView);
            setTitle(detectedBarcode.getBarcode().getFormat(), titleView);
            subtitleView.setText(detectedBarcode.getBarcode().getDisplayValue());
        }

        private void setTitle(int resultFormat, TextView titleView) {
            switch(resultFormat) {
                case Barcode.FORMAT_QR_CODE:
                    titleView.setText(R.string.preview_card_title_qr);
                    break;

                case Barcode.FORMAT_EAN_13:
                    titleView.setText(R.string.preview_card_title_ean13);
                    break;

                case Barcode.FORMAT_EAN_8:
                    titleView.setText(R.string.preview_card_title_ean8);
                    break;

                case Barcode.FORMAT_UPC_A:
                    titleView.setText(R.string.preview_card_title_upca);
                    break;

                case Barcode.FORMAT_UPC_E:
                    titleView.setText(R.string.preview_card_title_upce);
                    break;

                case Barcode.FORMAT_DATA_MATRIX:
                    titleView.setText(R.string.preview_card_title_datamatrix);
                    break;

                case Barcode.FORMAT_CODE_128:
                    titleView.setText(R.string.preview_card_title_code128);
                    break;
            }
        }

        private void setIcon(int resultType, ImageView imageView) {
            switch(resultType) {
                case Barcode.TYPE_URL:
                    imageView.setImageResource(R.drawable.ic_public_white_24dp);
                    break;

                case Barcode.TYPE_PHONE:
                    imageView.setImageResource(R.drawable.ic_phone_white_24dp);
                    break;

                case Barcode.TYPE_CONTACT_INFO:
                    imageView.setImageResource(R.drawable.ic_person_white_24dp);
                    break;

                case Barcode.TYPE_GEO:
                    imageView.setImageResource(R.drawable.ic_location_on_white_24dp);
                    break;

                case Barcode.TYPE_CALENDAR_EVENT:
                    imageView.setImageResource(R.drawable.ic_calender_white_24dp);
                    break;

                case Barcode.TYPE_WIFI:
                    imageView.setImageResource(R.drawable.ic_wifi_white_24dp);
                    break;

                default:
                    imageView.setImageResource(R.drawable.ic_text_white_24dp);
                    break;
            }
        }
    }
}
