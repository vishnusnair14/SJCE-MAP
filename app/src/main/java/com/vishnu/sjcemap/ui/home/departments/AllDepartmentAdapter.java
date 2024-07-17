package com.vishnu.sjcemap.ui.home.departments;

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

public class AllDepartmentAdapter extends BaseAdapter {
    private List<AllDepartmentModel> deptList;
    private final LayoutInflater inflater;
    Bundle bundle;
    private final String LOG_TAG = "AllDepartmentViewAdapter";
    private final String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg?alt=media&token=ec64235b-374c-458a-aaf9-7dc67c110513";
    private final Context context;
    private final DepartmentFragment departmentFragment;

    public AllDepartmentAdapter(List<AllDepartmentModel> deptList, Context context,
                                DepartmentFragment departmentFragment) {
        this.deptList = deptList;
        this.context = context;
        this.departmentFragment = departmentFragment;
        this.inflater = LayoutInflater.from(context);
        bundle = new Bundle();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<AllDepartmentModel> filteredList) {
        deptList = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return deptList.size();
    }

    @Override
    public Object getItem(int position) {
        return deptList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        AllDepartmentModel model = deptList.get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.srv_all_department_info, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.spotNameTV.setText(model.getSpot_name());
        holder.spotCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E", model.getSpot_lat(), model.getSpot_lon()));
        try {
            String imageUrl = model.getSpot_image_url().isEmpty() ? NO_IMG_FOUND_URL : model.getSpot_image_url();
            Picasso.get().load(imageUrl).into(holder.spotImageView);
        } catch (Exception e) {
            Picasso.get().load(NO_IMG_FOUND_URL).into(holder.spotImageView);
            Log.e(LOG_TAG, "Image load error", e);
        }

        holder.spotcardView.setOnClickListener(v -> {
            bundle.clear();

            bundle.putString("doc_path", "allDepartments");
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

            NavHostFragment.findNavController(departmentFragment).navigate(R.id.action_departmentFragment_to_mapFragment, bundle);
        });

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
