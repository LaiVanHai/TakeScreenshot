package com.haibka.takescreenshot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

public class CustomOverlayView extends View {

    private Rect rect;
    private Paint paint;
    private float startX, startY, endX, endY;
    private OnSelectionCompleteListener listener;

    public CustomOverlayView(Context context) {
        super(context);
        rect = new Rect();
        paint = new Paint();
        paint.setColor(Color.parseColor("#80FF0000")); // Semi-transparent red
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rect, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                endX = event.getX();
                endY = event.getY();
                rect.set(
                        (int) Math.min(startX, endX),
                        (int) Math.min(startY, endY),
                        (int) Math.max(startX, endX),
                        (int) Math.max(startY, endY)
                );
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                endX = event.getX();
                endY = event.getY();
                rect.set(
                        (int) Math.min(startX, endX),
                        (int) Math.min(startY, endY),
                        (int) Math.max(startX, endX),
                        (int) Math.max(startY, endY)
                );
                invalidate();
                if (listener != null) {
                    listener.onSelectionComplete(rect);
                }
                break;
        }
        return true;
    }

    public void setOnSelectionCompleteListener(OnSelectionCompleteListener listener) {
        this.listener = listener;
    }

    public interface OnSelectionCompleteListener {
        void onSelectionComplete(Rect rect);
    }

    public Rect getRect() {
        return rect;
    }
}
