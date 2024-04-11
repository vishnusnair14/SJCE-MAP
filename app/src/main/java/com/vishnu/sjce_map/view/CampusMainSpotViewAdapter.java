package com.vishnu.sjce_map.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.vishnu.sjce_map.MainActivity;
import com.vishnu.sjce_map.R;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.ui.home.MainSpotFragment;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class CampusMainSpotViewAdapter extends RecyclerView.Adapter<CampusMainSpotViewAdapter.ViewHolder> {
    private List<CampusMainSpotViewModel> itemList;
    private final String LOG_TAG = "SpotViewAdapter";
    private final Context context;
    private final MainSpotFragment mainSpotFragment;

    public CampusMainSpotViewAdapter(List<CampusMainSpotViewModel> itemList, Context context, MainSpotFragment mainSpotFragment) {
        this.itemList = itemList;
        this.context = context;
        this.mainSpotFragment = mainSpotFragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_spot_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CampusMainSpotViewModel campusMainSpotViewModel = itemList.get(position);

        holder.spotNameTV.setText(campusMainSpotViewModel.getSpot_name());
        holder.spotCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E", campusMainSpotViewModel.getSpot_lat(), campusMainSpotViewModel.getSpot_lon()));
        try {
            if (campusMainSpotViewModel.getSpot_image_url().isEmpty() || Objects.equals(campusMainSpotViewModel.getSpot_image_url(), " ")) {
                String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
                        "SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg?alt=media&token=ec64235b-374c-458a-aaf9-7dc67c110513";
                Picasso.get().load(NO_IMG_FOUND_URL).into(holder.spotImageView);
            } else {
                Picasso.get().load(campusMainSpotViewModel.getSpot_image_url()).into(holder.spotImageView);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e + " ");
        }

        holder.spotcardView.setOnClickListener(v -> {
            MainSpotFragment.updatePlace(itemList.get(position).getSpot_name_reference(), "SavedPlaceData");
            NavHostFragment.findNavController(mainSpotFragment).navigate(R.id.action_nav_mainspots_to_mapFragment);
//                Toast.makeText(context, holder.spotNameTV.getText(), Toast.LENGTH_SHORT).show();

        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<CampusMainSpotViewModel> filteredList) {
        itemList = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return 1;
        return position % 3;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView spotImageView;
        TextView spotNameTV, spotCoordinatesTV;
        CardView spotcardView;

        public ViewHolder(View itemView) {
            super(itemView);
            spotImageView = itemView.findViewById(R.id.spotImageSpotInfoView_imageView);
            spotNameTV = itemView.findViewById(R.id.spotNameSpotInfoView_textView);
            spotCoordinatesTV = itemView.findViewById(R.id.spotCoordinatesSpotInfoView_textView);
            spotcardView = itemView.findViewById(R.id.spotInfo_cardView);
        }
    }
}
