package com.example.clubmanagement.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class SignaturePadView extends View {
    private Path path;
    private Paint paint;
    private Paint canvasPaint;
    private Canvas canvas;
    private Bitmap bitmap;

    public SignaturePadView(Context context) {
        super(context);
        init();
    }

    public SignaturePadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignaturePadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        path = new Path();

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5f);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, canvasPaint);
        }
        canvas.drawPath(path, paint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touchStart(float x, float y) {
        path.reset();
        path.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        path.lineTo(mX, mY);

        if (canvas != null) {
            canvas.drawPath(path, paint);
        }

        path.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }

        return true;
    }

    /**
     * 서명을 지웁니다
     */
    public void clear() {
        if (bitmap != null) {
            bitmap.eraseColor(Color.WHITE);
            path.reset();
            invalidate();
        }
    }

    /**
     * 서명이 있는지 확인
     */
    public boolean hasSignature() {
        if (bitmap == null) return false;

        // 흰색이 아닌 픽셀이 있는지 확인
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        for (int x = 0; x < width; x += 10) {
            for (int y = 0; y < height; y += 10) {
                int pixel = bitmap.getPixel(x, y);
                if (pixel != Color.WHITE && Color.alpha(pixel) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 서명 Bitmap 가져오기 (투명 배경)
     */
    public Bitmap getSignatureBitmap() {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap transparentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(transparentBitmap);

        // 투명 배경으로 시작
        tempCanvas.drawColor(Color.TRANSPARENT);

        // 원본 비트맵에서 흰색이 아닌 픽셀만 복사
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);

            // 흰색에 가까운 픽셀을 투명하게
            if (red > 240 && green > 240 && blue > 240) {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        transparentBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return transparentBitmap;
    }
}
