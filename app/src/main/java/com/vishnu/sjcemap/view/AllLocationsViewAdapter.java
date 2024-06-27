package com.vishnu.sjcemap.view;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.vishnu.sjcemap.ui.home.AllLocationsFragment;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class AllLocationsViewAdapter extends BaseAdapter {
    private List<AllLocationsViewModel> itemList;
    private final String LOG_TAG = "SpotViewAdapter";
    Context context;
    private final AllLocationsFragment allLocationsFragment;
    String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg?alt=media&token=ec64235b-374c-458a-aaf9-7dc67c110513";

    public AllLocationsViewAdapter(List<AllLocationsViewModel> itemList, Context context, AllLocationsFragment allLocationsFragment) {
        this.itemList = itemList;
        this.context = context;
        this.allLocationsFragment = allLocationsFragment;
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

        AllLocationsViewModel allLocationsViewModel = itemList.get(position);
        holder.spotNameTV.setText(allLocationsViewModel.getSpot_name());
        holder.spotCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E", allLocationsViewModel.getSpot_lat(), allLocationsViewModel.getSpot_lon()));
        try {
            if (allLocationsViewModel.getSpot_image_url().isEmpty() || Objects.equals(allLocationsViewModel.getSpot_image_url(), " ")) {
                Picasso.get().load(NO_IMG_FOUND_URL).into(holder.spotImageView);
            } else {
                Picasso.get().load(allLocationsViewModel.getSpot_image_url()).into(holder.spotImageView);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e + " ");
        }

        holder.spotcardView.setOnClickListener(v -> {
            AllLocationsFragment.updatePlace(itemList.get(position).getSpot_name_reference(), "SavedPlaceData");
            NavHostFragment.findNavController(allLocationsFragment).navigate(R.id.action_nav_mainspots_to_mapFragment);
        });

        return convertView;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<AllLocationsViewModel> filteredList) {
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
