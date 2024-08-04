package com.haibka.takescreenshot;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
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
import java.nio.ByteBuffer;

import android.graphics.Canvas;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SCREENSHOT = 59706;

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private Button buttonCapture;
    private CustomOverlayView overlayView;
    private WindowManager windowManager;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        overlayView = new CustomOverlayView(this);
//        overlayView.setBackgroundColor(0x77000000);
        buttonCapture = findViewById(R.id.button_capture);
//        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

//        buttonCapture.setOnClickListener(v -> startScreenCapture());
//        buttonCapture.setOnClickListener(v -> startScreenCaptureProjection());
        buttonCapture.setOnClickListener(v -> {
            // Start the foreground service
            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }

            // Start screen capture process
            startScreenCaptureProjection();
        });
    }

    private void startScreenCaptureForeground() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK) {
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            startProjection();
        }
    }

    private void startScreenCaptureProjection() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
//            if (resultCode == RESULT_OK) {
//                MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//                mediaProjection = projectionManager.getMediaProjection(resultCode, data);
//                startProjection();
//            } else {
//                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    private void startProjection() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                // Xử lý khi MediaProjection bị ngừng
                Log.d("MediaProjection", "MediaProjection stopped");
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());
        mediaProjection.registerCallback(callback, handler);

        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, density, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null, null, null);

        ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        virtualDisplay.setSurface(imageReader.getSurface());
        imageReader.setOnImageAvailableListener(reader -> {
            // Handle image processing on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    saveBitmap(bitmap);
                    image.close();
                }
            });
        }, new Handler(Looper.getMainLooper())); // Handler for listener
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

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_SCREENSHOT) {
//            takeScreenshot();
//        }
//    }

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

//        if (mediaProjection != null) {
//            mediaProjection.stop();
//            mediaProjection = null;
//        }
    }


    private void takeScreenshot() {
        try {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            rootView.draw(canvas);

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
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
