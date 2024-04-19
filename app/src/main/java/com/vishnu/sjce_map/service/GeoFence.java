package com.vishnu.sjce_map.service;

public class GeoFence {

    public static boolean isInsideGeoFenceArea(double lat, double lon) {
        //TODO : Add the appropriate LatLon coordinates here

        // SJCE-MYSORE BACK EXIT GATE COORDINATES:
        double topLeftLat = 12.318289380014258;
        double topLeftLon = 76.61125779310221;

        // SJCE-MYSORE MAIN ENTRY GATE COORDINATES:
        double bottomRightLat = 12.311264587819064;
        double bottomRightLon = 76.61526699712476;

        // TEST BOUNDARIES (KKY)
        double topLeftLat1 = 10.754605223078789;
        double topLeftLon1 = 76.79775902053301;

        // TEST BOUNDARIES (KKY)
        double bottomRightLat1 = 10.75449683752582;
        double bottomRightLon1 = 76.79792999933173;

        return (lat >= bottomRightLat1 && lat <= topLeftLat1 && lon >= topLeftLon1 && lon <= bottomRightLon1);
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
