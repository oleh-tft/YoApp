package ua.tft.yoapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.permissionx.guolindev.PermissionX;

import java.io.OutputStream;

public class PhotoEditorActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private EditText etWatermark;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        currentBitmap = originalBitmap;
                        ivPhoto.setImageBitmap(currentBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Помилка завантаження", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_editor);

        ivPhoto = findViewById(R.id.ivPhoto);
        etWatermark = findViewById(R.id.etWatermark);

        // Інструменти
        Button btnRotate = findViewById(R.id.btnRotate);
        Button btnFlipH = findViewById(R.id.btnFlipH);
        Button btnFlipV = findViewById(R.id.btnFlipV);
        Button btnAddText = findViewById(R.id.btnAddText);

        // Фільтри
        Button btnFilterBW = findViewById(R.id.btnFilterBW);
        Button btnFilterSepia = findViewById(R.id.btnFilterSepia);
        Button btnFilterInvert = findViewById(R.id.btnFilterInvert);
        Button btnFilterHue = findViewById(R.id.btnFilterHue);
        Button btnFilterSaturate = findViewById(R.id.btnFilterSaturate);
        Button btnFilterBright = findViewById(R.id.btnFilterBright);
        Button btnFilterContrast = findViewById(R.id.btnFilterContrast);

        // Дії
        Button btnLoad = findViewById(R.id.btnLoad);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnShare = findViewById(R.id.btnShare);

        btnLoad.setOnClickListener(v -> checkPermissionsAndLoadImage());
        btnSave.setOnClickListener(v -> saveImageToGallery(false));
        btnShare.setOnClickListener(v -> saveImageToGallery(true));

        // Трансформації та текст
        btnRotate.setOnClickListener(v -> transformImage(1, 1, 90));
        btnFlipH.setOnClickListener(v -> transformImage(-1, 1, 0));
        btnFlipV.setOnClickListener(v -> transformImage(1, -1, 0));
        btnAddText.setOnClickListener(v -> addTextToImage(etWatermark.getText().toString()));

        // Фільтри
        btnFilterBW.setOnClickListener(v -> applyFilter("BW"));
        btnFilterSepia.setOnClickListener(v -> applyFilter("SEPIA"));
        btnFilterInvert.setOnClickListener(v -> applyFilter("INVERT"));
        btnFilterHue.setOnClickListener(v -> applyFilter("HUE"));
        btnFilterSaturate.setOnClickListener(v -> applyFilter("SATURATE"));
        btnFilterBright.setOnClickListener(v -> applyFilter("BRIGHTNESS"));
        btnFilterContrast.setOnClickListener(v -> applyFilter("CONTRAST"));
    }

    private void checkPermissionsAndLoadImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        PermissionX.init(this)
                .permissions(permission)
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickImageLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "Потрібен дозвіл!", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Накладання тексту (впікається у зображення)
    private void addTextToImage(String text) {
        if (originalBitmap == null || text.trim().isEmpty()) {
            Toast.makeText(this, "Введіть текст та завантажте фото!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Створюємо змінну копію (mutable) для малювання
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        // Динамічний розмір тексту відносно ширини картинки
        paint.setTextSize(mutableBitmap.getWidth() / 15f);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        // Чорна тінь для контрасту на світлому фоні
        paint.setShadowLayer(8f, 4f, 4f, Color.BLACK);

        // Малюємо текст по центру вгорі
        float x = (mutableBitmap.getWidth() - paint.measureText(text)) / 2f;
        float y = paint.getTextSize() + (mutableBitmap.getHeight() * 0.05f);
        canvas.drawText(text, x, y, paint);

        // Зберігаємо змінене фото як нове оригінальне
        originalBitmap = mutableBitmap;
        currentBitmap = originalBitmap;
        ivPhoto.setImageBitmap(currentBitmap);
        etWatermark.setText(""); // Очищуємо поле
    }

    private void transformImage(float scaleX, float scaleY, float degrees) {
        if (originalBitmap == null) return;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        matrix.postRotate(degrees);
        Bitmap transformedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0,
                originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
        originalBitmap = transformedBitmap;
        currentBitmap = originalBitmap;
        ivPhoto.setImageBitmap(currentBitmap);
    }

    private void applyFilter(String filterType) {
        if (originalBitmap == null) return;
        currentBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getConfig());
        Canvas canvas = new Canvas(currentBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();

        switch (filterType) {
            case "BW":
                colorMatrix.setSaturation(0);
                break;
            case "SEPIA":
                colorMatrix.setSaturation(0);
                ColorMatrix sepia = new ColorMatrix();
                sepia.setScale(1.2f, 1.0f, 0.8f, 1.0f);
                colorMatrix.postConcat(sepia);
                break;
            case "INVERT":
                colorMatrix.set(new float[]{
                        -1,  0,  0,  0, 255,
                        0, -1,  0,  0, 255,
                        0,  0, -1,  0, 255,
                        0,  0,  0,  1,   0
                });
                break;
            case "SATURATE":
                // Посилюємо кольори вдвічі
                colorMatrix.setSaturation(2.0f);
                break;
            case "HUE":
                // Математичний зсув спектру (Hue Rotation) на 90 градусів
                adjustHue(colorMatrix, 90f);
                break;
            case "BRIGHTNESS":
                colorMatrix.set(new float[]{
                        1, 0, 0, 0, 50,
                        0, 1, 0, 0, 50,
                        0, 0, 1, 0, 50,
                        0, 0, 0, 1, 0
                });
                break;
            case "CONTRAST":
                float scale = 1.5f;
                float translate = (-.5f * scale + .5f) * 255f;
                colorMatrix.set(new float[]{
                        scale, 0, 0, 0, translate,
                        0, scale, 0, 0, translate,
                        0, 0, scale, 0, translate,
                        0, 0, 0, 1, 0
                });
                break;
        }

        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(originalBitmap, 0, 0, paint);
        ivPhoto.setImageBitmap(currentBitmap);
    }

    // Тригонометрична матриця обертання відтінку кольору (Hue Shift)
    private void adjustHue(ColorMatrix cm, float degree) {
        float radians = (float) (degree * Math.PI / 180f);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float lumR = 0.213f;
        float lumG = 0.715f;
        float lumB = 0.072f;
        float[] mat = new float[]{
                lumR + cos * (1 - lumR) + sin * (-lumR), lumG + cos * (-lumG) + sin * (-lumG), lumB + cos * (-lumB) + sin * (1 - lumB), 0, 0,
                lumR + cos * (-lumR) + sin * (0.143f), lumG + cos * (1 - lumG) + sin * (0.140f), lumB + cos * (-lumB) + sin * (-0.283f), 0, 0,
                lumR + cos * (-lumR) + sin * (-(1 - lumR)), lumG + cos * (-lumG) + sin * (lumG), lumB + cos * (1 - lumB) + sin * (lumB), 0, 0,
                0f, 0f, 0f, 1f, 0f
        };
        cm.postConcat(new ColorMatrix(mat));
    }

    private void saveImageToGallery(boolean shareAfterSave) {
        if (currentBitmap == null) return;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "secret_lab_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SecretLab");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.close();
                if (shareAfterSave) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/jpeg");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, "Поділитися доказом"));
                } else {
                    Toast.makeText(this, "Збережено в галерею!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}