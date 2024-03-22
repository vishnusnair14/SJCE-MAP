package com.vishnu.sjce_map.view;

public class AllDepartmentsViewModel {

    private String spot_name;
    private String spot_lat;
    private String spot_lon;
    private String spot_name_reference;
    private String spot_image_url;

    public AllDepartmentsViewModel(String spot_name, String spot_lat, String spot_lon, String spot_name_reference, String spot_image_url) {
        this.spot_name = spot_name;
        this.spot_lat = spot_lat;
        this.spot_lon = spot_lon;
        this.spot_name_reference = spot_name_reference;
        this.spot_image_url = spot_image_url;
    }

    public String getSpot_name() {
        return spot_name;
    }

    public String getSpot_image_url() {
        return spot_image_url;
    }

    public void setSpot_image_url(String spot_image_url) {
        this.spot_image_url = spot_image_url;
    }

    public void setSpot_name(String spot_name) {
        this.spot_name = spot_name;
    }

    public String getSpot_lat() {
        return spot_lat;
    }

    public void setSpot_lat(String spot_lat) {
        this.spot_lat = spot_lat;
    }

    public String getSpot_lon() {
        return spot_lon;
    }

    public void setSpot_lon(String spot_lon) {
        this.spot_lon = spot_lon;
    }

    public String getSpot_name_reference() {
        return spot_name_reference;
    }

    public void setSpot_name_reference(String spot_name_reference) {
        this.spot_name_reference = spot_name_reference;
    }
}
