package com.example.proekt;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class RoutingService {

    public interface RouteCallback {
        void onRouteFound(List<LatLng> points);
        void onRouteError(String message);
    }

    interface OSRMApi {

        @GET("route/v1/driving/{coordinates}")
        Call<OSRMResponse> getRoute(
                @Path(value = "coordinates", encoded = true) String coordinates,
                @Query("overview") String overview,
                @Query("geometries") String geometries,
                @Query("steps") String steps
        );
    }

    public static class OSRMResponse {
        public String code;
        public List<OSRMRoute> routes;
    }

    public static class OSRMRoute {
        public OSRMGeometry geometry;
    }

    public static class OSRMGeometry {
        public String type;
        public List<List<Double>> coordinates;
    }

    private final OSRMApi api;

    public RoutingService() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://router.project-osrm.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(OSRMApi.class);
    }

    public void findRoute(
            LatLng start,
            LatLng end,
            RouteCallback callback
    ) {

        if (start == null || end == null) {

            callback.onRouteError(
                    "Начальная или конечная точка не выбрана"
            );

            return;
        }

        String coordinates =
                start.longitude + "," + start.latitude
                        + ";"
                        + end.longitude + "," + end.latitude;

        api.getRoute(
                coordinates,
                "full",
                "geojson",
                "false"
        ).enqueue(
                new Callback<OSRMResponse>() {

                    @Override
                    public void onResponse(
                            Call<OSRMResponse> call,
                            Response<OSRMResponse> response
                    ) {

                        if (!response.isSuccessful()) {

                            callback.onRouteError(
                                    "Ошибка маршрутизации: HTTP "
                                            + response.code()
                            );

                            return;
                        }

                        OSRMResponse body =
                                response.body();

                        if (body == null) {

                            callback.onRouteError(
                                    "Пустой ответ сервиса маршрутизации"
                            );

                            return;
                        }

                        if (!"Ok".equalsIgnoreCase(body.code)) {

                            callback.onRouteError(
                                    "OSRM: " + body.code
                            );

                            return;
                        }

                        if (body.routes == null
                                || body.routes.isEmpty()) {

                            callback.onRouteError(
                                    "Маршрут не найден"
                            );

                            return;
                        }

                        OSRMRoute route =
                                body.routes.get(0);

                        if (route.geometry == null
                                || route.geometry.coordinates == null
                                || route.geometry.coordinates.size() < 2) {

                            callback.onRouteError(
                                    "Маршрут не содержит координат"
                            );

                            return;
                        }

                        List<LatLng> points =
                                new ArrayList<>();

                        for (
                                List<Double> coordinate
                                : route.geometry.coordinates
                        ) {

                            if (coordinate == null
                                    || coordinate.size() < 2) {
                                continue;
                            }

                            double longitude =
                                    coordinate.get(0);

                            double latitude =
                                    coordinate.get(1);

                            points.add(
                                    new LatLng(
                                            latitude,
                                            longitude
                                    )
                            );
                        }

                        if (points.size() < 2) {

                            callback.onRouteError(
                                    "Недостаточно точек маршрута"
                            );

                            return;
                        }

                        callback.onRouteFound(points);
                    }

                    @Override
                    public void onFailure(
                            Call<OSRMResponse> call,
                            Throwable t
                    ) {

                        callback.onRouteError(
                                "Ошибка соединения: "
                                        + t.getMessage()
                        );
                    }
                }
        );
    }
}
