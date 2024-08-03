package com.haibka.takescreenshot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RectangleSelectionView extends View {
    private Rect rect;
    private Paint paint;

    private int startX, startY, endX, endY;

    public RectangleSelectionView(Context context) {
        super(context);
        init();
    }

    public RectangleSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        rect = new Rect();
        paint = new Paint();
        paint.setColor(Color.RED);
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
                startX = (int) event.getX();
                startY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                endX = (int) event.getX();
                endY = (int) event.getY();
                updateRect();
                break;
            case MotionEvent.ACTION_UP:
                endX = (int) event.getX();
                endY = (int) event.getY();
                updateRect();
                break;
        }
        return true;
    }

    private void updateRect() {
        rect.set(Math.min(startX, endX), Math.min(startY, endY),
                Math.max(startX, endX), Math.max(startY, endY));
        invalidate();
    }

    public Rect getSelectionRect() {
        return rect;
    }
}
