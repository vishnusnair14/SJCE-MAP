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

}
