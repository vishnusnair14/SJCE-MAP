package com.vishnu.sjcemap.service;

import java.util.Objects;

public class GeoFence {

    public static boolean isInsideGeoFenceArea(double lat, double lon, String spot) {
        //TODO : Add the appropriate LatLon coordinates here

        double topLeftLat, topLeftLon, bottomRightLat, bottomRightLon;

        if (Objects.equals(spot, "SJCE")) {
            // SJCE-MYSORE BACK EXIT GATE COORDINATES:
            topLeftLat = 12.318289380014258;
            topLeftLon = 76.61125779310221;

            // SJCE-MYSORE MAIN ENTRY GATE COORDINATES:
            bottomRightLat = 12.311264587819064;
            bottomRightLon = 76.61526699712476;
        } else if (Objects.equals(spot, "GKLM")) {
            // TEST BOUNDARIES (GKLM)
            topLeftLat = 12.337006178380026;
            topLeftLon = 76.62650340130956;

            // TEST BOUNDARIES (GKLM)
            bottomRightLat = 12.336567919050339;
            bottomRightLon = 76.62761303374035;
        } else if (Objects.equals(spot, "NJKD")) {
            // TEST BOUNDARIES (NJKD)
            topLeftLat = 12.116076417826017;
            topLeftLon = 76.74433115584156;

            // TEST BOUNDARIES (NJKD)
            bottomRightLat = 12.108990858007683;
            bottomRightLon = 76.75952822236135;
        } else {
            topLeftLat = 12.318289380014258;
            topLeftLon = 76.61125779310221;

            // SJCE-MYSORE MAIN ENTRY GATE COORDINATES:`
            bottomRightLat = 12.311264587819064;
            bottomRightLon = 76.61526699712476;
        }

        return (lat >= bottomRightLat && lat <= topLeftLat && lon >= topLeftLon && lon <= bottomRightLon);
    }

    public static boolean isInsideGJB(double lat, double lon) {
        // golden-jubilee-block boundary
        double topLeftLat = 12.31700026871531;
        double topLeftLon = 76.61384665081256;
        double bottomRightLat = 12.315803282353533;
        double bottomRightLon = 76.61474608292343;

        return (lat >= bottomRightLat && lat <= topLeftLat && lon >= topLeftLon && lon <= bottomRightLon);
    }

    public static boolean isInsideCMS(double lat, double lon) {
        // CMS-block boundary
        double topLeftLat = 12.317812749625686;
        double topLeftLon = 76.61399819105716;
        double bottomRightLat = 12.31743221981478;
        double bottomRightLon = 76.61470260061012;

        return (lat >= bottomRightLat && lat <= topLeftLat && lon >= topLeftLon && lon <= bottomRightLon);
    }

}
