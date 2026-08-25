package com.example.proekt;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class RouteManager {

    private final GoogleMap map;
    private Polyline routePolyline;

    public RouteManager(GoogleMap map) {
        this.map = map;
    }

    public void drawRoute(List<LatLng> points) {
        clearRoute();

        if (points == null || points.size() < 2) {
            return;
        }

        routePolyline = map.addPolyline(
                new PolylineOptions()
                        .addAll(points)
                        .color(Color.BLUE)
                        .width(10f)
                        .zIndex(2f)
        );
    }

    public void clearRoute() {
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }
    }
}
