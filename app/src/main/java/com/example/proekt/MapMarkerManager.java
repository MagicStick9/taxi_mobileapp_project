package com.example.proekt;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapMarkerManager {

    private final GoogleMap map;
    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private Marker startMarker;
    private Marker endMarker;

    private LatLng startLatLng;
    private LatLng endLatLng;

    public MapMarkerManager(
            GoogleMap map,
            Context context
    ) {
        this.map = map;
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setStartPoint(LatLng latLng) {

        removeStartMarker();

        startLatLng = latLng;

        startMarker = map.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .title("🟢 Начальная точка")
                        .snippet("Определяем улицу...")
                        .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN
                                )
                        )
                        .anchor(0.5f, 1.0f)
        );

        if (startMarker != null) {
            getAddress(latLng, startMarker);
        }
    }

    public void setEndPoint(LatLng latLng) {

        removeEndMarker();

        endLatLng = latLng;

        endMarker = map.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .title("🔴 Конечная точка")
                        .snippet("Определяем улицу...")
                        .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED
                                )
                        )
                        .anchor(0.5f, 1.0f)
        );

        if (endMarker != null) {
            getAddress(latLng, endMarker);
        }
    }

    private void getAddress(
            LatLng latLng,
            Marker marker
    ) {

        executor.execute(() -> {

            String address = resolveAddress(latLng);

            mainHandler.post(() -> {

                if (marker != null) {
                    marker.setSnippet(address);
                    marker.showInfoWindow();
                }
            });
        });
    }

    private String resolveAddress(LatLng latLng) {

        try {

            Geocoder geocoder = new Geocoder(context);

            List<Address> addresses =
                    geocoder.getFromLocation(
                            latLng.latitude,
                            latLng.longitude,
                            1
                    );

            if (addresses != null && !addresses.isEmpty()) {

                Address address = addresses.get(0);

                StringBuilder result = new StringBuilder();

                if (address.getThoroughfare() != null) {
                    result.append(
                            address.getThoroughfare()
                    );
                }

                if (address.getSubThoroughfare() != null) {

                    if (result.length() > 0) {
                        result.append(", ");
                    }

                    result.append(
                            address.getSubThoroughfare()
                    );
                }

                if (result.length() > 0) {
                    return result.toString();
                }

                String fullAddress =
                        address.getAddressLine(0);

                if (fullAddress != null) {
                    return fullAddress;
                }
            }

        } catch (IOException e) {
            return "Адрес не определён";
        }

        return "Адрес не определён";
    }

    public LatLng getStartLatLng() {
        return startLatLng;
    }

    public LatLng getEndLatLng() {
        return endLatLng;
    }

    public Marker getStartMarker() {
        return startMarker;
    }

    public Marker getEndMarker() {
        return endMarker;
    }

    public boolean hasStartPoint() {
        return startLatLng != null;
    }

    public boolean hasEndPoint() {
        return endLatLng != null;
    }

    public void reset() {

        removeStartMarker();
        removeEndMarker();

        startLatLng = null;
        endLatLng = null;
    }

    private void removeStartMarker() {

        if (startMarker != null) {
            startMarker.remove();
            startMarker = null;
        }
    }

    private void removeEndMarker() {

        if (endMarker != null) {
            endMarker.remove();
            endMarker = null;
        }
    }

    public void shutdown() {

        executor.shutdownNow();

        startMarker = null;
        endMarker = null;

        startLatLng = null;
        endLatLng = null;
    }
}