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
import com.vishnu.sjce_map.ui.home.HomeFragment;

import java.text.MessageFormat;
import java.util.List;


public class SavedPlaceViewAdapter extends RecyclerView.Adapter<SavedPlaceViewAdapter.ViewHolder> {
    private List<SavedPlaceViewModel> itemList;
    private final String LOG_TAG = "SpotViewAdapter";
    private final Context context;
    private HomeFragment homeFragment;
    MainActivity mainActivity;
    SharedDataView sharedDataView;

    public SavedPlaceViewAdapter(List<SavedPlaceViewModel> itemList, Context context, HomeFragment homeFragment) {
        this.itemList = itemList;
        this.context = context;
        this.homeFragment = homeFragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_spot_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedPlaceViewModel savedPlaceViewModel = itemList.get(position);

        holder.spotNameTV.setText(savedPlaceViewModel.getSpot_name());
        holder.spotCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E", savedPlaceViewModel.getSpot_lat(), savedPlaceViewModel.getSpot_lon()));
        try {
            if (savedPlaceViewModel.getSpot_image_url().isEmpty()) {
                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg" +
                        "?alt=media&token=d1309045-5ebd-4aa9-a1ef-64424ffdc4ae").into(holder.spotImageView);
            } else {
                Picasso.get().load(savedPlaceViewModel.getSpot_image_url()).into(holder.spotImageView);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e + " ");
        }

        holder.spotcardView.setOnClickListener(v -> {
            if (itemList.get(position).getSpot_name_reference().equals("sjce_department_blocks")) {
                NavHostFragment.findNavController(homeFragment).navigate(R.id.action_nav_home_to_departmentFragment);
            } else {
                HomeFragment.updatePlace(itemList.get(position).getSpot_name_reference(), "SavedPlaceData");
                NavHostFragment.findNavController(homeFragment).navigate(R.id.action_nav_home_to_mapFragment);
                Toast.makeText(context, holder.spotNameTV.getText(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<SavedPlaceViewModel> filteredList) {
        itemList = filteredList;
        notifyDataSetChanged();
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
