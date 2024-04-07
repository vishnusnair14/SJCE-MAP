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
import com.vishnu.sjce_map.ui.home.DepartmentFragment;

import java.text.MessageFormat;
import java.util.List;


public class AllDepartmentViewAdapter extends RecyclerView.Adapter<AllDepartmentViewAdapter.ViewHolder> {
    private List<AllDepartmentsViewModel> itemList;
    private final String LOG_TAG = "AllDepartmentViewAdapter";
    private final String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg" +
            "?alt=media&token=d1309045-5ebd-4aa9-a1ef-64424ffdc4ae";
    private final Context context;
    private DepartmentFragment departmentFragment;
    MainActivity mainActivity;
    SharedDataView sharedDataView;

    public AllDepartmentViewAdapter(List<AllDepartmentsViewModel> itemList, Context context, DepartmentFragment departmentFragment) {
        this.itemList = itemList;
        this.context = context;
        this.departmentFragment = departmentFragment;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<AllDepartmentsViewModel> filteredList) {
        itemList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_all_department_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllDepartmentsViewModel AllDepartmentsViewModel = itemList.get(position);

        holder.spotNameTV.setText(AllDepartmentsViewModel.getSpot_name());
        holder.spotCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E", AllDepartmentsViewModel.getSpot_lat(), AllDepartmentsViewModel.getSpot_lon()));
        try {
            if (AllDepartmentsViewModel.getSpot_image_url().isEmpty()) {
                Picasso.get().load(NO_IMG_FOUND_URL).into(holder.spotImageView);
            } else {
                Picasso.get().load(AllDepartmentsViewModel.getSpot_image_url()).into(holder.spotImageView);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e + " ");
        }

        holder.spotcardView.setOnClickListener(v -> {
            DepartmentFragment.updateDataToSharedView(itemList.get(position).getSpot_name_reference(), "DepartmentsLocationData");
            NavHostFragment.findNavController(departmentFragment).navigate(R.id.action_departmentFragment_to_mapFragment);
            Toast.makeText(context, holder.spotNameTV.getText(), Toast.LENGTH_SHORT).show();

        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView spotImageView;
        TextView spotNameTV, spotCoordinatesTV;
        CardView spotcardView;

        public ViewHolder(View itemView) {
            super(itemView);
            spotImageView = itemView.findViewById(R.id.spotImageAllDepartmentInfoView_imageView);
            spotNameTV = itemView.findViewById(R.id.spotNameAllDepartmentInfoView_textView);
            spotCoordinatesTV = itemView.findViewById(R.id.spotCoordinatesAllDepartmentInfoView_textView);
            spotcardView = itemView.findViewById(R.id.allDepartments_cardView);
        }
    }
}
