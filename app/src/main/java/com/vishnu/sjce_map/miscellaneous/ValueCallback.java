package com.vishnu.sjce_map.miscellaneous;

import com.google.firebase.firestore.GeoPoint;

public interface ValueCallback {
    void onSuccess(String placeName, GeoPoint geoPoint);
    void onError(String errorMessage);
}
