package com.haibka.takescreenshot;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.hardware.display.DisplayManager;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenCaptureActivity extends Activity {
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private SelectionView selectionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectionView = new SelectionView(this);
        setContentView(selectionView);

        // Khởi tạo MediaProjection và ImageReader
        setupImageReader();
    }

    private void setupImageReader() {
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
//                    Image.Plane[] planes = image.getPlanes();
//                    ByteBuffer buffer = planes[0].getBuffer();
//                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                    bitmap.copyPixelsFromBuffer(buffer);
//                    saveBitmap(bitmap);
                    processImage(image);
                    image.close();
                }
            });
        }, new Handler(Looper.getMainLooper())); // Handler for listener
    }

    private void processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        // Cắt ảnh theo vùng đã chọn
        Rect selectionRect = selectionView.getSelectionRect();
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, selectionRect.left, selectionRect.top,
                selectionRect.width(), selectionRect.height());

        // Lưu hoặc xử lý ảnh đã cắt
        saveBitmap(croppedBitmap);
    }

    private void saveBitmap(Bitmap bitmap) {
        // Implement the method to save the bitmap to file or perform other actions
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        Uri uri = getImageUri(this, bitmap);
        ClipData clip = ClipData.newUri(getContentResolver(), "Screenshot", uri);
        clipboard.setPrimaryClip(clip);
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }
}
