package com.example.proekt;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        EditText nameEdit = new EditText(this);
        nameEdit.setHint("Введите ваше имя");

        EditText phoneEdit = new EditText(this);
        phoneEdit.setHint("Введите ваш номер телефона");

        Button saveButton = new Button(this);
        saveButton.setText("Сохранить");
        saveButton.setOnClickListener(v -> {
            String name = nameEdit.getText().toString();
            String phone = phoneEdit.getText().toString();
            Toast.makeText(this, "Сохранено: " + name + ", " + phone, Toast.LENGTH_SHORT).show();
            finish();
        });

        layout.addView(nameEdit);
        layout.addView(phoneEdit);
        layout.addView(saveButton);

        setContentView(layout);
    }

}
