package ua.tft.yoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;
import java.util.Random;

import ua.tft.yoapp.models.Cat;

public class MainActivity extends AppCompatActivity {

    private final Random random = new Random();
    private int counter = 0;

    private TextView tvTimer;
    private CountDownTimer countDownTimer;
    private Cat myCat;

    // Змінні для поширення
    private Uri selectedImageUri = null;
    private EditText etShareMessage;

    private static final long START_TIME_IN_MILLIS = 300000;
    private static final int REQUEST_POST_NOTIFICATIONS = 100;

    // Щоб не запускати сервіс, коли просто відкриваємо PhotoEditor або галерею
    private boolean isOpeningInternalScreen = false;

    // Лаунчер для вибору зображення
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Toast.makeText(this, "Зображення обрано!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();

        if (savedInstanceState == null) {
            myCat = new Cat("Мурзик", 3, "Рудий");
            Toast.makeText(this, "Створено кота: " + myCat.getName(), Toast.LENGTH_SHORT).show();
        }

        ConstraintLayout layout = findViewById(R.id.main);
        tvTimer = findViewById(R.id.tvTimerDecor);
        etShareMessage = findViewById(R.id.etShareMessage);

        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startTimer();

        // Кнопка переходу в редактор
        Button btnPhotoEditor = findViewById(R.id.btnPhotoEditor);
        btnPhotoEditor.setOnClickListener(v -> {
            isOpeningInternalScreen = true;
            Intent intent = new Intent(MainActivity.this, PhotoEditorActivity.class);
            startActivity(intent);
        });

        // Кнопка вибору фото
        Button btnPickImageMain = findViewById(R.id.btnPickImageMain);
        btnPickImageMain.setOnClickListener(v -> {
            isOpeningInternalScreen = true;
            pickImageLauncher.launch("image/*");
        });

        // Кнопка поширення
        Button btnShareMain = findViewById(R.id.btnShareMain);
        btnShareMain.setOnClickListener(v -> shareContent());

        // Сітка
        GridLayout grid = findViewById(R.id.gridContainer);
        int buttonSizePx = dpToPx(38);
        for (int i = 0; i < 64; i++) {
            Button b = new Button(this);
            b.setLayoutParams(new ViewGroup.LayoutParams(buttonSizePx, buttonSizePx));
            b.setOnClickListener(this::onColorClick);
            grid.addView(b);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isOpeningInternalScreen = false;
        // Користувач у додатку — зупиняємо сервіс, щоб не слав сповіщення
        stopService(new Intent(this, DefuseNotificationService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Якщо користувач вийшов/закрив додаток (не поворот екрану і не перехід у редактор)
        if (!isChangingConfigurations() && !isOpeningInternalScreen) {
            startLeaveNotificationService();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS
            );
        }
    }

    private void startLeaveNotificationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            Intent serviceIntent = new Intent(this, DefuseNotificationService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        } catch (Exception ignored) {
            // Захист від обмежень фонового запуску на окремих версіях Android
        }
    }

    private void shareContent() {
        String textMessage = etShareMessage.getText().toString();

        if (textMessage.isEmpty() && selectedImageUri == null) {
            Toast.makeText(this, "Додайте текст або оберіть фото!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        // Якщо є фото, додаємо його до Intent
        if (selectedImageUri != null) {
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, selectedImageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent.setType("text/plain");
        }

        // Якщо є текст, додаємо його
        if (!textMessage.isEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, textMessage);
        }

        isOpeningInternalScreen = true;
        startActivity(Intent.createChooser(shareIntent, "Надіслати звіт"));
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(START_TIME_IN_MILLIS, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                int hundredths = (int) (millisUntilFinished % 1000) / 10;
                String timeFormatted = String.format(Locale.getDefault(), "%02d : %02d : %02d", minutes, seconds, hundredths);
                tvTimer.setText(timeFormatted);
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00 : 00 : 00");
            }
        }.start();
    }

    public void fun(View view) {
        counter++;
        Button b = (Button) view;
        b.setText(String.valueOf(counter));
    }

    public void onColorClick(View view) {
        Button b = (Button) view;
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(red, green, blue)));
    }

    private int dpToPx(int dp) {
        return Math.round((float) dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("CAT_STATE", myCat);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            myCat = (Cat) savedInstanceState.getSerializable("CAT_STATE");
        }
    }
}