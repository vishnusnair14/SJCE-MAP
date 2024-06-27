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

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.vishnu.sjcemap.R;
import com.vishnu.sjcemap.ui.home.DepartmentFragment;

import java.text.MessageFormat;
import java.util.List;


public class AllDepartmentViewAdapter extends BaseAdapter {
    private List<AllDepartmentsViewModel> itemList;
    private final LayoutInflater inflater;
    private final String LOG_TAG = "AllDepartmentViewAdapter";
    private final String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg?alt=media&token=ec64235b-374c-458a-aaf9-7dc67c110513";
    private final Context context;
    private DepartmentFragment departmentFragment;

    public AllDepartmentViewAdapter(List<AllDepartmentsViewModel> itemList, Context context, DepartmentFragment departmentFragment) {
        this.itemList = itemList;
        this.context = context;
        this.departmentFragment = departmentFragment;
        this.inflater = LayoutInflater.from(context);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<AllDepartmentsViewModel> filteredList) {
        itemList = filteredList;
        notifyDataSetChanged();
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

        AllDepartmentsViewModel item = itemList.get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.srv_all_department_info, parent, false);
            holder = new ViewHolder(convertView);

            holder.spotNameTV.setText(item.getSpot_name());
            holder.spotCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E", item.getSpot_lat(), item.getSpot_lon()));
            try {
                if (item.getSpot_image_url().isEmpty()) {
                    Picasso.get().load(NO_IMG_FOUND_URL).into(holder.spotImageView);
                } else {
                    Picasso.get().load(item.getSpot_image_url()).into(holder.spotImageView);
                }
            } catch (Exception e) {
                Picasso.get().load(NO_IMG_FOUND_URL).into(holder.spotImageView);
                Log.e(LOG_TAG, e + " ");
            }

            holder.spotcardView.setOnClickListener(v -> {
                DepartmentFragment.updateDataToSharedView(itemList.get(position).getSpot_name_reference(), "DepartmentsLocationData");
                NavHostFragment.findNavController(departmentFragment).navigate(R.id.action_departmentFragment_to_mapFragment);
            });

        }
        return convertView;
    }

    static class ViewHolder {
        ImageView spotImageView;
        TextView spotNameTV, spotCoordinatesTV;
        CardView spotcardView;

        public ViewHolder(View itemView) {
            spotImageView = itemView.findViewById(R.id.spotImageAllDepartmentInfoView_imageView);
            spotNameTV = itemView.findViewById(R.id.spotNameAllDepartmentInfoView_textView);
            spotCoordinatesTV = itemView.findViewById(R.id.spotCoordinatesAllDepartmentInfoView_textView);
            spotcardView = itemView.findViewById(R.id.allDepartments_cardView);
        }
    }
}
