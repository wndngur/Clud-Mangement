package com.example.clubmanagement.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * 핀치 줌과 드래그를 지원하는 이미지 크롭 뷰
 */
public class CropImageView extends AppCompatImageView {

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    private float[] matrixValues = new float[9];

    // 터치 모드
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // 시작점
    private float startX, startY;

    // 스케일 관련
    private float minScale = 0.5f;
    private float maxScale = 5f;
    private float currentScale = 1f;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    // 크롭 영역 (정사각형)
    private RectF cropRect = new RectF();
    private Paint cropPaint;
    private Paint dimPaint;

    // 이미지 원본 크기
    private int imageWidth, imageHeight;
    private boolean isInitialized = false;

    public CropImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CropImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CropImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        // 크롭 영역 테두리 페인트
        cropPaint = new Paint();
        cropPaint.setColor(0xFFFFFFFF);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(3f);

        // 어두운 영역 페인트
        dimPaint = new Paint();
        dimPaint.setColor(0x88000000);
        dimPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setupCropRect();
        if (!isInitialized) {
            centerImage();
        }
    }

    private void setupCropRect() {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        // 정사각형 크롭 영역 (화면 너비의 90%)
        float cropSize = Math.min(width, height) * 0.9f;
        float left = (width - cropSize) / 2f;
        float top = (height - cropSize) / 2f;

        cropRect.set(left, top, left + cropSize, top + cropSize);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (bm != null) {
            imageWidth = bm.getWidth();
            imageHeight = bm.getHeight();
            isInitialized = false;
            post(this::centerImage);
        }
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        if (drawable != null) {
            imageWidth = drawable.getIntrinsicWidth();
            imageHeight = drawable.getIntrinsicHeight();
            isInitialized = false;
            post(this::centerImage);
        }
    }

    private void centerImage() {
        if (imageWidth == 0 || imageHeight == 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        setupCropRect();

        // 크롭 영역에 맞게 이미지 스케일 계산
        float cropSize = cropRect.width();
        float scaleX = cropSize / imageWidth;
        float scaleY = cropSize / imageHeight;

        // 이미지가 크롭 영역을 채우도록 (더 큰 스케일 사용)
        currentScale = Math.max(scaleX, scaleY);
        minScale = currentScale * 0.5f;
        maxScale = currentScale * 5f;

        matrix.reset();
        matrix.postScale(currentScale, currentScale);

        // 이미지를 크롭 영역 중앙에 배치
        float scaledWidth = imageWidth * currentScale;
        float scaledHeight = imageHeight * currentScale;
        float dx = cropRect.centerX() - scaledWidth / 2f;
        float dy = cropRect.centerY() - scaledHeight / 2f;

        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);

        isInitialized = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cropRect.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        // 크롭 영역 외부를 어둡게
        // 상단
        canvas.drawRect(0, 0, width, cropRect.top, dimPaint);
        // 하단
        canvas.drawRect(0, cropRect.bottom, width, height, dimPaint);
        // 좌측
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, dimPaint);
        // 우측
        canvas.drawRect(cropRect.right, cropRect.top, width, cropRect.bottom, dimPaint);

        // 크롭 영역 테두리
        canvas.drawRect(cropRect, cropPaint);

        // 그리드 라인 (3x3)
        float thirdW = cropRect.width() / 3f;
        float thirdH = cropRect.height() / 3f;

        cropPaint.setAlpha(100);
        // 수직선
        canvas.drawLine(cropRect.left + thirdW, cropRect.top, cropRect.left + thirdW, cropRect.bottom, cropPaint);
        canvas.drawLine(cropRect.left + thirdW * 2, cropRect.top, cropRect.left + thirdW * 2, cropRect.bottom, cropPaint);
        // 수평선
        canvas.drawLine(cropRect.left, cropRect.top + thirdH, cropRect.right, cropRect.top + thirdH, cropPaint);
        canvas.drawLine(cropRect.left, cropRect.top + thirdH * 2, cropRect.right, cropRect.top + thirdH * 2, cropPaint);
        cropPaint.setAlpha(255);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                startX = event.getX();
                startY = event.getY();
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                savedMatrix.set(matrix);
                mode = ZOOM;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG && !scaleGestureDetector.isInProgress()) {
                    float dx = event.getX() - startX;
                    float dy = event.getY() - startY;

                    matrix.set(savedMatrix);
                    matrix.postTranslate(dx, dy);

                    constrainMatrix();
                    setImageMatrix(matrix);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                constrainMatrix();
                setImageMatrix(matrix);
                break;
        }

        return true;
    }

    private void constrainMatrix() {
        matrix.getValues(matrixValues);

        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scaleX = matrixValues[Matrix.MSCALE_X];

        float scaledWidth = imageWidth * scaleX;
        float scaledHeight = imageHeight * scaleX;

        // 이미지가 크롭 영역보다 작아지지 않도록
        float minWidth = cropRect.width();
        float minHeight = cropRect.height();

        float dx = 0, dy = 0;

        // 이미지의 오른쪽 가장자리가 크롭 영역 왼쪽보다 왼쪽에 있으면 안됨
        if (transX + scaledWidth < cropRect.right) {
            dx = cropRect.right - (transX + scaledWidth);
        }
        // 이미지의 왼쪽 가장자리가 크롭 영역 오른쪽보다 오른쪽에 있으면 안됨
        if (transX > cropRect.left) {
            dx = cropRect.left - transX;
        }

        // 세로도 동일
        if (transY + scaledHeight < cropRect.bottom) {
            dy = cropRect.bottom - (transY + scaledHeight);
        }
        if (transY > cropRect.top) {
            dy = cropRect.top - transY;
        }

        if (dx != 0 || dy != 0) {
            matrix.postTranslate(dx, dy);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            matrix.getValues(matrixValues);
            float currentScaleX = matrixValues[Matrix.MSCALE_X];

            float newScale = currentScaleX * scaleFactor;

            // 최소/최대 스케일 제한
            if (newScale < minScale) {
                scaleFactor = minScale / currentScaleX;
            } else if (newScale > maxScale) {
                scaleFactor = maxScale / currentScaleX;
            }

            matrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
            constrainMatrix();
            setImageMatrix(matrix);

            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 더블 탭으로 초기 위치로 복귀
            centerImage();
            return true;
        }
    }

    /**
     * 크롭 영역의 이미지를 비트맵으로 반환
     */
    public Bitmap getCroppedBitmap() {
        Drawable drawable = getDrawable();
        if (drawable == null || !(drawable instanceof BitmapDrawable)) {
            return null;
        }

        Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();
        if (originalBitmap == null) return null;

        // 현재 매트릭스 값 가져오기
        matrix.getValues(matrixValues);
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];

        // 크롭 영역을 원본 이미지 좌표로 변환
        float srcLeft = (cropRect.left - transX) / scaleX;
        float srcTop = (cropRect.top - transY) / scaleX;
        float srcWidth = cropRect.width() / scaleX;
        float srcHeight = cropRect.height() / scaleX;

        // 범위 검증
        srcLeft = Math.max(0, srcLeft);
        srcTop = Math.max(0, srcTop);
        srcWidth = Math.min(srcWidth, originalBitmap.getWidth() - srcLeft);
        srcHeight = Math.min(srcHeight, originalBitmap.getHeight() - srcTop);

        if (srcWidth <= 0 || srcHeight <= 0) {
            return originalBitmap;
        }

        // 크롭된 비트맵 생성
        try {
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    (int) srcLeft,
                    (int) srcTop,
                    (int) srcWidth,
                    (int) srcHeight
            );

            // 정사각형으로 리사이즈 (800x800)
            int outputSize = 800;
            return Bitmap.createScaledBitmap(croppedBitmap, outputSize, outputSize, true);
        } catch (Exception e) {
            e.printStackTrace();
            return originalBitmap;
        }
    }

    /**
     * 이미지 초기화
     */
    public void resetImage() {
        isInitialized = false;
        centerImage();
    }
}
