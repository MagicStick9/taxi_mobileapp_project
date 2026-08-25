package com.example.proekt;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.button.MaterialButton;

import java.util.*;

import okhttp3.Interceptor;
import okhttp3.Request;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String BASE_URL = "http://10.0.2.2:3000";

    private static final String AUTH_PREFS = "auth";
    private static final String JWT_KEY = "jwt";
    private static final String NAME_KEY = "name";
    private static final String EMAIL_KEY = "email";

    private GoogleMap map;
    private Marker startMarker, endMarker;
    private LatLng startLatLng, endLatLng;

    private MapController mapController;
    private RoutingService routingService;
    private RouteManager routeManager;
    private RetrofitInterface retrofitInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Восстанавливаем тему до создания Activity.
        applySavedTheme();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupLocation();
        setupMap();
        setupRetrofit();
        setupButtons();

        // Восстанавливаем аккаунт после пересоздания Activity.
        restoreSession();
    }

    private void applySavedTheme() {
        boolean darkMode = getSharedPreferences(
                "app_settings", MODE_PRIVATE
        ).getBoolean("dark_mode", false);

        AppCompatDelegate.setDefaultNightMode(
                darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

     // Восстанавление пользователя (настройки, пересоздание activity, повторный запуск приложения)
    private void restoreSession() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);

        String token = prefs.getString(JWT_KEY, null);
        String name = prefs.getString(NAME_KEY, null);
        String email = prefs.getString(EMAIL_KEY, null);

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        // Если данные пользователя сохранены локально, сразу восстанавливаем профиль без повторного входа.
        if (name != null && !name.isEmpty()) {
            showProfile(name, email != null ? email : "", false);
        }
    }

    private void setupLocation() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            enableLocation();
        }
    }

    private void setupMap() {
        SupportMapFragment fragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (fragment == null) return;

        fragment.getMapAsync(googleMap -> {
            map = googleMap;
            routeManager = new RouteManager(map);
            routingService = new RoutingService();

            setupMapStyle();

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(55.751244, 37.618423), 14
            ));

            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setCompassEnabled(true);

            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }

            setupMapController();
            setupBookingButton();
        });
    }

    private void setupMapStyle() {
        if (AppCompatDelegate.getDefaultNightMode()
                != AppCompatDelegate.MODE_NIGHT_YES) {
            return;
        }

        try {
            map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.night_map_style
                    )
            );
        } catch (Resources.NotFoundException ignored) {
        }
    }

    private void setupMapController() {
        mapController = new MapController(
                map,
                this,
                (newStartMarker, newEndMarker, newStartLatLng, newEndLatLng) -> {
                    startMarker = newStartMarker;
                    endMarker = newEndMarker;
                    startLatLng = newStartLatLng;
                    endLatLng = newEndLatLng;

                    if (startLatLng == null || endLatLng == null) return;

                    routingService.findRoute(
                            startLatLng,
                            endLatLng,
                            new RoutingService.RouteCallback() {
                                @Override
                                public void onRouteFound(List<LatLng> points) {
                                    runOnUiThread(() -> {
                                        if (routeManager != null) {
                                            routeManager.drawRoute(points);
                                        }
                                    });
                                }

                                @Override
                                public void onRouteError(String message) {
                                    runOnUiThread(() ->
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    message,
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                    );
                                }
                            }
                    );
                }
        );
    }

    private void setupBookingButton() {
        MaterialButton button = findViewById(R.id.BookingSheetButton);

        button.setOnClickListener(v -> {
            if (startLatLng == null || endLatLng == null) {
                Toast.makeText(
                        this,
                        "Выберите начальную и конечную точку на карте",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            BookingBottomSheet.newInstance(
                    startLatLng.latitude + ", " + startLatLng.longitude,
                    endLatLng.latitude + ", " + endLatLng.longitude,
                    startLatLng,
                    endLatLng
            ).show(getSupportFragmentManager(), "BookingBottomSheet");
        });
    }

    private void setupRetrofit() {
        Interceptor interceptor = chain -> {
            String token = getSharedPreferences(
                    AUTH_PREFS, MODE_PRIVATE
            ).getString(JWT_KEY, null);

            Request request = chain.request();

            if (token != null && !token.trim().isEmpty()) {
                request = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
            }

            return chain.proceed(request);
        };

        okhttp3.OkHttpClient client =
                new okhttp3.OkHttpClient.Builder()
                        .addInterceptor(interceptor)
                        .build();

        retrofitInterface = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RetrofitInterface.class);
    }

    private void setupButtons() {
        findViewById(R.id.login).setOnClickListener(
                v -> handleLoginDialog()
        );

        findViewById(R.id.signup).setOnClickListener(
                v -> handleSignupDialog()
        );

        findViewById(R.id.test).setOnClickListener(
                v -> showTestUser()
        );
    }

    private void showTestUser() {
        LinearLayout profile = findViewById(R.id.profileLayout);
        TextView welcome = findViewById(R.id.welcomeText);
        Button login = findViewById(R.id.login);
        Button signup = findViewById(R.id.signup);
        View test = findViewById(R.id.test);

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(
                android.R.attr.textColorPrimary, value, true
        );

        welcome.setText("Куда поедете, Тестовый пользователь?");
        welcome.setTextColor(ContextCompat.getColor(this, value.resourceId));

        profile.setVisibility(View.VISIBLE);
        login.setVisibility(View.GONE);
        signup.setVisibility(View.GONE);
        test.setVisibility(View.GONE);

        findViewById(R.id.preferencesButton).setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class))
        );

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            profile.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            signup.setVisibility(View.VISIBLE);
            test.setVisibility(View.VISIBLE);

            Toast.makeText(
                    this,
                    "Разлогированы",
                    Toast.LENGTH_SHORT
            ).show();
        });

        Toast.makeText(
                this,
                "Тестовый пользователь",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                    this,
                    "Разрешение на геолокацию получено",
                    Toast.LENGTH_SHORT
            ).show();

            enableLocation();
        } else {
            Toast.makeText(
                    this,
                    "Разрешение на геолокацию отклонено",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void enableLocation() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && map != null) {
            map.setMyLocationEnabled(true);
        }
    }

    private void handleLoginDialog() {
        View view = getLayoutInflater()
                .inflate(R.layout.login_dialog, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.show();

        Button loginBtn = view.findViewById(R.id.login);
        Button signupBtn = view.findViewById(R.id.notreg);
        EditText email = view.findViewById(R.id.emailEdit);
        EditText password = view.findViewById(R.id.passwordEdit);

        loginBtn.setOnClickListener(v -> {
            String e = email.getText().toString()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            String p = password.getText().toString();

            if (!isValidLogin(e, p)) {
                showValidationRules();
                return;
            }

            loginBtn.setEnabled(false);

            HashMap<String, String> data = new HashMap<>();
            data.put("email", e);
            data.put("password", p);

            retrofitInterface.executeLogin(data)
                    .enqueue(new Callback<LoginResult>() {
                        @Override
                        public void onResponse(
                                Call<LoginResult> call,
                                Response<LoginResult> response
                        ) {
                            loginBtn.setEnabled(true);

                            if (!response.isSuccessful()
                                    || response.body() == null) {

                                if (response.code() == 400
                                        || response.code() == 401
                                        || response.code() == 404) {
                                    showValidationRules();
                                } else if (response.code() >= 500) {
                                    showError(
                                            "Сервер временно недоступен.\n" +
                                                    "Попробуйте позже."
                                    );
                                } else {
                                    showError(
                                            "Не удалось выполнить вход."
                                    );
                                }

                                return;
                            }

                            LoginResult result = response.body();
                            String token = result.getToken();

                            if (token == null || token.trim().isEmpty()) {
                                showError("Сервер не вернул JWT.");
                                return;
                            }

                            // Сохраняем полноценную сессию.
                            saveSession(
                                    token,
                                    result.getName(),
                                    result.getEmail()
                            );

                            showProfile(
                                    result.getName(),
                                    result.getEmail(),
                                    true
                            );

                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(
                                Call<LoginResult> call,
                                Throwable t
                        ) {
                            loginBtn.setEnabled(true);

                            showError(
                                    "Не удалось подключиться к серверу.\n" +
                                            "Технические шоколадки."
                            );
                        }
                    });
        });

        signupBtn.setOnClickListener(v -> {
            dialog.dismiss();
            handleSignupDialog();
        });
    }

    // Сохраняет данные текущего пользователя.
    private void saveSession(String token, String name, String email) {
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                .edit()
                .putString(JWT_KEY, token)
                .putString(NAME_KEY, name)
                .putString(EMAIL_KEY, email)
                .apply();
    }

    /**
     * Показывает авторизованный профиль.
     * showWelcome = true только сразу после успешного входа.
     * При восстановлении после смены темы диалог повторно не показывается.
     */
    private void showProfile(
            String name,
            String email,
            boolean showWelcome
    ) {
        LinearLayout profile = findViewById(R.id.profileLayout);
        TextView welcome = findViewById(R.id.welcomeText);
        Button login = findViewById(R.id.login);
        Button signup = findViewById(R.id.signup);
        View test = findViewById(R.id.test);

        welcome.setText("Куда поедете, " + name + " ?");

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(
                android.R.attr.textColorPrimary, value, true
        );

        welcome.setTextColor(
                ContextCompat.getColor(this, value.resourceId)
        );

        profile.setVisibility(View.VISIBLE);
        login.setVisibility(View.GONE);
        signup.setVisibility(View.GONE);
        test.setVisibility(View.GONE);

        findViewById(R.id.preferencesButton).setOnClickListener(
                v -> startActivity(
                        new Intent(this, SettingsActivity.class)
                )
        );

        findViewById(R.id.logoutButton).setOnClickListener(
                v -> logout()
        );

        if (!showWelcome) return;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(email)
                .setCancelable(false)
                .create();

        dialog.show();

        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (startLatLng != null && endLatLng != null) {
                showBookingSheet();
            }
        }, 2000);
    }

    //Logout
    private void logout() {
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                .edit()
                .remove(JWT_KEY)
                .remove(NAME_KEY)
                .remove(EMAIL_KEY)
                .apply();

        LinearLayout profile = findViewById(R.id.profileLayout);
        Button login = findViewById(R.id.login);
        Button signup = findViewById(R.id.signup);
        View test = findViewById(R.id.test);

        profile.setVisibility(View.GONE);
        login.setVisibility(View.VISIBLE);
        signup.setVisibility(View.VISIBLE);
        test.setVisibility(View.VISIBLE);

        Toast.makeText(
                this,
                "Разлогированы",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void handleSignupDialog() {
        View view = getLayoutInflater()
                .inflate(R.layout.signup_dialog, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.show();

        Button loginBtn = view.findViewById(R.id.alreadyreg);
        Button signupBtn = view.findViewById(R.id.signup);
        EditText name = view.findViewById(R.id.nameEdit);
        EditText email = view.findViewById(R.id.emailEdit);
        EditText password = view.findViewById(R.id.passwordEdit);

        signupBtn.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String e = email.getText().toString()
                    .trim()
                    .toLowerCase(Locale.ROOT);
            String p = password.getText().toString();

            if (!isValidSignup(n, e, p)) {
                showValidationRules();
                return;
            }

            signupBtn.setEnabled(false);

            HashMap<String, String> data = new HashMap<>();
            data.put("name", n);
            data.put("email", e);
            data.put("password", p);

            retrofitInterface.executeSignup(data)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(
                                Call<Void> call,
                                Response<Void> response
                        ) {
                            signupBtn.setEnabled(true);

                            if (response.code() == 200
                                    || response.code() == 201) {

                                Toast.makeText(
                                        MainActivity.this,
                                        "Регистрация успешно завершена",
                                        Toast.LENGTH_LONG
                                ).show();

                                dialog.dismiss();
                                handleLoginDialog();

                            } else if (response.code() == 400
                                    || response.code() == 409) {

                                showError(
                                        "Пользователь с таким email уже зарегистрирован."
                                );

                            } else if (response.code() == 422) {
                                showValidationRules();

                            } else if (response.code() >= 500) {
                                showError(
                                        "Сервер временно недоступен.\n" +
                                                "Попробуйте позже."
                                );

                            } else {
                                showError(
                                        "Не удалось зарегистрироваться."
                                );
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<Void> call,
                                Throwable t
                        ) {
                            signupBtn.setEnabled(true);

                            showError(
                                    "Не удалось подключиться к серверу.\n" +
                                            "Регистратион нэ пинго-коннекто."
                            );
                        }
                    });
        });

        loginBtn.setOnClickListener(v -> {
            dialog.dismiss();
            handleLoginDialog();
        });
    }

    private boolean isValidLogin(String email, String password) {
        return !email.isEmpty()
                && isValidEmail(email)
                && !password.isEmpty()
                && isValidPassword(password);
    }

    private boolean isValidSignup(
            String name,
            String email,
            String password
    ) {
        return !name.isEmpty()
                && name.matches(
                "^[А-Яа-яЁёA-Za-z][А-Яа-яЁёA-Za-z -]{1,49}$"
        )
                && isValidEmail(email)
                && isValidPassword(password);
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches();
    }

    private boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 8
                && password.matches(".*[A-Za-zА-Яа-яЁё].*")
                && password.matches(".*\\d.*");
    }

    private void showValidationRules() {
        showError(
                "Правила заполнения:\n\n" +
                        "Имя: 2–50 символов, буквы, пробел и дефис.\n" +
                        "Email: корректный адрес, например user@example.com.\n" +
                        "Пароль: минимум 8 символов, минимум одна буква и одна цифра.\n\n" +
                        "Для входа используйте зарегистрированные email и пароль."
        );
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .show();
    }

    private void showBookingSheet() {
        if (startLatLng == null || endLatLng == null) {
            Toast.makeText(
                    this,
                    "Выберите начальную и конечную точку на карте",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String startAddress = getMarkerAddress(startMarker);
        String endAddress = getMarkerAddress(endMarker);

        BookingBottomSheet.newInstance(
                startAddress,
                endAddress,
                startLatLng,
                endLatLng
        ).show(getSupportFragmentManager(), "BookingBottomSheet");
    }

    private String getMarkerAddress(Marker marker) {
        if (marker == null) return "";

        if (marker.getTitle() != null
                && !marker.getTitle().isEmpty()) {
            return marker.getTitle();
        }

        return marker.getSnippet() != null
                ? marker.getSnippet()
                : "";
    }
}
