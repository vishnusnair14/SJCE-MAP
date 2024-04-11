package com.vishnu.sjce_map.miscellaneous;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedDataView extends ViewModel {
    private final MutableLiveData<Double> destLat = new MutableLiveData<>();
    private final MutableLiveData<Double> destLon = new MutableLiveData<>();
    private final MutableLiveData<Double> clientLat = new MutableLiveData<>();
    private final MutableLiveData<Double> clientLon = new MutableLiveData<>();
    private final MutableLiveData<String> place = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLocProviderEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> docPath = new MutableLiveData<>();

    public LiveData<String> getDocPath() {
        return docPath;
    }

    public LiveData<Double> getClientLat() {
        return clientLat;
    }

    public LiveData<Boolean> getIsLocProviderEnabled() {
        return isLocProviderEnabled;
    }

    public LiveData<Double> getClientLon() {
        return clientLon;
    }

    public LiveData<String> getPlace() {
        return place;
    }

    public LiveData<Double> getDestLat() {
        return destLat;
    }

    public LiveData<Double> getDestLon() {
        return destLon;
    }

    public void setDestLat(double lat) {
        destLon.setValue(lat);
    }

    public void setPlace(String pl) {
        place.setValue(pl);
    }

    public void setIsLocProviderEnabled(boolean en) {
        isLocProviderEnabled.setValue(en);
    }

    public void setDocPath(String path) {
        docPath.setValue(path);
    }

    public void setDestLon(double lon) {
        destLat.setValue(lon);
    }

    public void setClientLon(double lon) {
        clientLon.setValue(lon);
    }

    public void setClientLat(double lat) {
        clientLat.setValue(lat);
    }
}
