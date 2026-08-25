package com.example.proekt;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        AppCompatDelegate.setDefaultNightMode(
                prefs.getBoolean("dark_mode", false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(
                android.R.attr.windowBackground,
                typedValue,
                true
        );

        findViewById(android.R.id.content).setBackgroundColor(
                ContextCompat.getColor(this, typedValue.resourceId)
        );

        findViewById(R.id.languageButton).setOnClickListener(v -> {
        });

        findViewById(R.id.themeButton).setOnClickListener(v -> {
            boolean darkMode = !prefs.getBoolean("dark_mode", false);

            prefs.edit()
                    .putBoolean("dark_mode", darkMode)
                    .apply();

            AppCompatDelegate.setDefaultNightMode(
                    darkMode
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        findViewById(R.id.aboutButton).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class))
        );

        findViewById(R.id.profileButton).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        findViewById(R.id.helpButton).setOnClickListener(v -> {
            Intent emailIntent = new Intent(
                    Intent.ACTION_SENDTO,
                    Uri.fromParts("mailto", "your_email@example.com", null)
            );

            emailIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Help Request"
            );

            startActivity(
                    Intent.createChooser(
                            emailIntent,
                            "Send email..."
                    )
            );
        });

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }
}


