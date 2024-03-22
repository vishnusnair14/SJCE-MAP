package com.vishnu.sjce_map.miscellaneous;

public interface MapDataCallback {
    void onDataReceived(String placeName, double latitude, double longitude);

    void onError(String errorMessage);
}
