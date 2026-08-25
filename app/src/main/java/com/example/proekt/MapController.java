package com.example.proekt;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MapController {

    public interface PointListener {

        void onPointsChanged(
                Marker startMarker,
                Marker endMarker,
                LatLng startLatLng,
                LatLng endLatLng
        );
    }

    private final GoogleMap map;
    private final Context context;
    private final MapMarkerManager markerManager;
    private final RouteManager routeManager;
    private final PointListener pointListener;

    public MapController(
            GoogleMap map,
            Context context,
            PointListener pointListener
    ) {

        this.map = map;
        this.context = context;
        this.pointListener = pointListener;

        this.markerManager =
                new MapMarkerManager(
                        map,
                        context
                );

        this.routeManager =
                new RouteManager(
                        map
                );

        map.setOnMapClickListener(
                this::handleMapClick
        );
    }

    private void handleMapClick(LatLng latLng) {

        if (!markerManager.hasStartPoint()) {

            markerManager.setStartPoint(latLng);

            notifyPointListener();

            return;
        }

        if (!markerManager.hasEndPoint()) {

            markerManager.setEndPoint(latLng);

            notifyPointListener();

            return;
        }

        routeManager.clearRoute();

        markerManager.reset();

        markerManager.setStartPoint(latLng);

        notifyPointListener();
    }

    private void notifyPointListener() {

        if (pointListener != null) {

            pointListener.onPointsChanged(
                    markerManager.getStartMarker(),
                    markerManager.getEndMarker(),
                    markerManager.getStartLatLng(),
                    markerManager.getEndLatLng()
            );
        }
    }

    public LatLng getStartLatLng() {
        return markerManager.getStartLatLng();
    }

    public LatLng getEndLatLng() {
        return markerManager.getEndLatLng();
    }

    public Marker getStartMarker() {
        return markerManager.getStartMarker();
    }

    public Marker getEndMarker() {
        return markerManager.getEndMarker();
    }

    public boolean hasStartPoint() {
        return markerManager.hasStartPoint();
    }

    public boolean hasEndPoint() {
        return markerManager.hasEndPoint();
    }

    public void drawRoute(
            java.util.List<LatLng> points
    ) {
        routeManager.drawRoute(points);
    }

    public void clearRoute() {
        routeManager.clearRoute();
    }

    public void reset() {

        routeManager.clearRoute();
        markerManager.reset();

        notifyPointListener();
    }

    public void destroy() {

        routeManager.clearRoute();
        markerManager.shutdown();
    }
}