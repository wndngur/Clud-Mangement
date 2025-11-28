package com.example.clubmanagement.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class SignatureUtil {
    private static final String TAG = "SignatureUtil";

    /**
     * Bitmap을 PNG byte array로 변환
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 서명 패드용 빈 Bitmap 생성
     */
    public static Bitmap createSignatureBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        return bitmap;
    }

    /**
     * Path를 Bitmap에 그리기
     */
    public static Bitmap drawPathOnBitmap(Path path, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 투명 배경
        canvas.drawColor(Color.TRANSPARENT);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        canvas.drawPath(path, paint);

        return bitmap;
    }

    /**
     * 배경 제거 (흰색 배경을 투명하게)
     * 간단한 알고리즘: 흰색에 가까운 픽셀을 투명하게 변환
     */
    public static Bitmap removeWhiteBackground(Bitmap originalBitmap) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap transparentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] pixels = new int[width * height];
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];

            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);

            // 흰색에 가까운 픽셀을 투명하게 (threshold: 240)
            if (red > 240 && green > 240 && blue > 240) {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        transparentBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return transparentBitmap;
    }

    /**
     * Bitmap 크기 조정
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Bitmap 자르기 (서명 영역만 추출)
     */
    public static Bitmap cropSignature(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int minX = width, minY = height, maxX = 0, maxY = 0;
        boolean foundSignature = false;

        // 서명이 있는 영역 찾기
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = Color.alpha(pixel);

                // 투명하지 않은 픽셀 = 서명
                if (alpha > 0) {
                    foundSignature = true;
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (!foundSignature) {
            return bitmap;
        }

        // 여백 추가 (10px)
        int padding = 10;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(width - 1, maxX + padding);
        maxY = Math.min(height - 1, maxY + padding);

        int cropWidth = maxX - minX + 1;
        int cropHeight = maxY - minY + 1;

        if (cropWidth <= 0 || cropHeight <= 0) {
            return bitmap;
        }

        return Bitmap.createBitmap(bitmap, minX, minY, cropWidth, cropHeight);
    }

    /**
     * 서명 이미지 전처리 (업로드용)
     * 배경 제거 -> 크롭 -> 리사이즈
     */
    public static Bitmap processSignatureImage(Bitmap originalBitmap) {
        // 1. 배경 제거
        Bitmap transparent = removeWhiteBackground(originalBitmap);

        // 2. 크롭
        Bitmap cropped = cropSignature(transparent);

        // 3. 리사이즈 (최대 800x400)
        Bitmap resized = resizeBitmap(cropped, 800, 400);

        return resized;
    }
}
