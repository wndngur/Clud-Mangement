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

import java.util.ArrayList;
import java.util.List;

/**
 * 서명을 캡처하기 위한 커스텀 View
 */
public class SignatureView extends View {

    private Paint paint;
    private Path currentPath;
    private List<Path> paths;
    private List<Paint> paints;

    private float lastX, lastY;
    private static final float TOUCH_TOLERANCE = 4;

    private int strokeColor = Color.BLACK;
    private float strokeWidth = 5f;

    private boolean isEmpty = true;

    public SignatureView(Context context) {
        super(context);
        init();
    }

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignatureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paths = new ArrayList<>();
        paints = new ArrayList<>();

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(strokeColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidth);

        currentPath = new Path();

        setBackgroundColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 이전 경로들 그리기
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }

        // 현재 경로 그리기
        canvas.drawPath(currentPath, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void touchStart(float x, float y) {
        currentPath = new Path();
        currentPath.moveTo(x, y);
        lastX = x;
        lastY = y;
        isEmpty = false;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - lastX);
        float dy = Math.abs(y - lastY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
            lastX = x;
            lastY = y;
        }
    }

    private void touchUp() {
        currentPath.lineTo(lastX, lastY);

        // 현재 경로와 페인트 저장
        paths.add(currentPath);
        Paint newPaint = new Paint(paint);
        paints.add(newPaint);

        // 새 경로 시작
        currentPath = new Path();
    }

    /**
     * 서명 초기화
     */
    public void clear() {
        paths.clear();
        paints.clear();
        currentPath = new Path();
        isEmpty = true;
        invalidate();
    }

    /**
     * 서명이 비어있는지 확인
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * 서명을 Bitmap으로 변환
     */
    public Bitmap getSignatureBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    /**
     * 투명 배경의 서명 Bitmap 반환
     */
    public Bitmap getTransparentSignatureBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 투명 배경
        canvas.drawColor(Color.TRANSPARENT);

        // 경로 그리기
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }
        canvas.drawPath(currentPath, paint);

        return bitmap;
    }

    /**
     * 서명 영역만 크롭된 Bitmap 반환
     */
    public Bitmap getCroppedSignatureBitmap() {
        Bitmap fullBitmap = getTransparentSignatureBitmap();

        // 서명 영역 찾기
        int minX = fullBitmap.getWidth();
        int minY = fullBitmap.getHeight();
        int maxX = 0;
        int maxY = 0;

        for (int y = 0; y < fullBitmap.getHeight(); y++) {
            for (int x = 0; x < fullBitmap.getWidth(); x++) {
                int pixel = fullBitmap.getPixel(x, y);
                if (Color.alpha(pixel) > 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        // 여백 추가
        int padding = 10;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(fullBitmap.getWidth() - 1, maxX + padding);
        maxY = Math.min(fullBitmap.getHeight() - 1, maxY + padding);

        if (maxX <= minX || maxY <= minY) {
            return fullBitmap;
        }

        return Bitmap.createBitmap(fullBitmap, minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * 선 색상 설정
     */
    public void setStrokeColor(int color) {
        this.strokeColor = color;
        paint.setColor(color);
    }

    /**
     * 선 두께 설정
     */
    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        paint.setStrokeWidth(width);
    }
}
