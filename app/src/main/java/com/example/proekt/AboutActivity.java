package com.example.proekt;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView textView = new TextView(this);
        textView.setText(
                "INFO\n\n" +
                "📱 О приложении:\n" +
                        "Это приложение такси создано, чтобы помочь вам заказать поездку быстро и легко.\n\n" +

                        "👨‍💻 Разработчик:\n" +
                        "MagicStick9\n\n" +

                        "📚 ТЗ\n" +
                        "Сделано по ТЗ\n\n" +

                        "🎓 Тема проекта:\n" +
                        "Мобильное приложение «Такси»\n\n" +

                        "📝 Комментарии:\n" +
                        "Проект ориентирован на создание приложения для вызова такси в рамках портфолио " +
                        "без коммерческой реализации или поддержки в будущем.\n\n" +

                        "🛠️ — Версия:\n" +
                        "Альфа-версия 1.05\n"
        );
        textView.setTextSize(16);
        layout.addView(textView);
        scrollView.addView(layout);
        setContentView(scrollView);
    }
}

