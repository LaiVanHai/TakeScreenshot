package com.haibka.takescreenshot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

public class SelectionView extends View {
    private Paint paint;
    private Rect selectionRect;
    private boolean isSelecting = false;

    public SelectionView(Context context) {
        super(context);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFFF0000); // Màu đỏ
        paint.setStrokeWidth(5);
        selectionRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isSelecting) {
            canvas.drawRect(selectionRect, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectionRect.left = (int) event.getX();
                selectionRect.top = (int) event.getY();
                selectionRect.right = selectionRect.left;
                selectionRect.bottom = selectionRect.top;
                isSelecting = true;
                break;
            case MotionEvent.ACTION_MOVE:
                selectionRect.right = (int) event.getX();
                selectionRect.bottom = (int) event.getY();
                break;
            case MotionEvent.ACTION_UP:
                isSelecting = false;
                break;
        }
        invalidate();
        return true;
    }

    public Rect getSelectionRect() {
        return selectionRect;
    }
}

