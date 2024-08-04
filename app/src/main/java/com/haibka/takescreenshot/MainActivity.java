package com.haibka.takescreenshot;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.haibka.takescreenshot.databinding.ActivityMainBinding;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Canvas;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SCREENSHOT = 59706;

    private ImageView imageView;
    private Button buttonCapture;
    private CustomOverlayView overlayView;
    private WindowManager windowManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        overlayView = new CustomOverlayView(this);
        overlayView.setBackgroundColor(0x77000000);
        buttonCapture = findViewById(R.id.button_capture);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        buttonCapture.setOnClickListener(v -> startScreenCapture());
    }


    private void startScreenCapture() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(overlayView, params);

        overlayView.setOnSelectionCompleteListener(new CustomOverlayView.OnSelectionCompleteListener() {
            @Override
            public void onSelectionComplete(Rect rect) {
//                captureScreen(rect);
                captureScreen();
            }
        });
    }

    private void captureScreen(Rect rect) {
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);

        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
        saveBitmap(croppedBitmap);

        // Remove overlay view
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREENSHOT) {
            takeScreenshot();
        }
    }

    private void captureScreen() {
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);

        saveBitmap(bitmap);

        // Remove overlay view
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }


    private void takeScreenshot() {
        try {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            rootView.draw(canvas);

            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);

            // Lưu ảnh vào clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            Uri uri = getImageUri(this, bitmap);
            ClipData clip = ClipData.newUri(getContentResolver(), "Screenshot", uri);
            clipboard.setPrimaryClip(clip);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void saveBitmap(Bitmap bitmap) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        Uri uri = getImageUri(this, bitmap);
        ClipData clip = ClipData.newUri(getContentResolver(), "Screenshot", uri);
        clipboard.setPrimaryClip(clip);

    }
}
