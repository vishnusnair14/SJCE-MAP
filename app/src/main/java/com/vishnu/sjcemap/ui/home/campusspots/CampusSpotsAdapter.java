package com.vishnu.sjcemap.ui.home.campusspots;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.navigation.fragment.NavHostFragment;

import com.squareup.picasso.Picasso;
import com.vishnu.sjcemap.R;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class CampusSpotsAdapter extends BaseAdapter {
    private List<CampusSpotsModel> itemList;
    private final String LOG_TAG = "SpotViewAdapter";
    Context context;
    Bundle bundle;
    private final CampusSpotsFragment campusSpotsFragment;
    String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg?alt=media&token=ec64235b-374c-458a-aaf9-7dc67c110513";

    public CampusSpotsAdapter(List<CampusSpotsModel> itemList, Context context, CampusSpotsFragment campusSpotsFragment) {
        this.itemList = itemList;
        this.context = context;
        this.campusSpotsFragment = campusSpotsFragment;
        bundle = new Bundle();
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.srv_spot_info, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CampusSpotsModel model = itemList.get(position);
        holder.spotNameTV.setText(model.getSpot_name());
        holder.spotCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E", model.getSpot_lat(), model.getSpot_lon()));

        try {
            if (model.getSpot_image_url().isEmpty() || Objects.equals(model.getSpot_image_url(), " ")) {
                Picasso.get().load(NO_IMG_FOUND_URL).into(holder.spotImageView);
            } else {
                Picasso.get().load(model.getSpot_image_url()).into(holder.spotImageView);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e + " ");
        }

        holder.spotcardView.setOnClickListener(v -> {
            bundle.clear();

            bundle.putString("doc_path", "allMainCampusLocations");
            bundle.putString("doc_id", model.getDoc_id());
            bundle.putBoolean("load_from_db", false);

            bundle.putString("spot_name", model.getSpot_name());
            bundle.putString("spot_image_url", model.getSpot_image_url());
            bundle.putString("about_department", model.getAbout_department());
            bundle.putString("spot_360_view_gmap_url", model.getSpot_360_view_gmap_url());
            bundle.putString("spot_lat", model.getSpot_lat());
            bundle.putString("spot_lon", model.getSpot_lon());
            bundle.putString("spot_google_image_url_1", model.getSpot_google_image_url_1());
            bundle.putString("spot_google_image_url_2", model.getSpot_google_image_url_2());
            bundle.putString("spot_google_image_url_3", model.getSpot_google_image_url_3());
            bundle.putString("spot_google_image_url_4", model.getSpot_google_image_url_4());
            NavHostFragment.findNavController(campusSpotsFragment).navigate(R.id.action_nav_mainspots_to_mapFragment, bundle);
        });

        return convertView;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<CampusSpotsModel> filteredList) {
        itemList = filteredList;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView spotImageView;
        TextView spotNameTV, spotCoordinatesTV;
        CardView spotcardView;

        ViewHolder(View itemView) {
            spotImageView = itemView.findViewById(R.id.spotImageSpotInfoView_imageView);
            spotNameTV = itemView.findViewById(R.id.spotNameSpotInfoView_textView);
            spotCoordinatesTV = itemView.findViewById(R.id.spotCoordinatesSpotInfoView_textView);
            spotcardView = itemView.findViewById(R.id.spotInfo_cardView);
        }
    }
}
