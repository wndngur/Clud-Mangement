package com.example.clubmanagement.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.clubmanagement.R;

/**
 * 이미지 로딩 관련 공통 유틸리티 클래스
 * Glide를 사용한 일관된 이미지 로딩을 제공합니다.
 */
public class ImageHelper {

    // ======================== 기본 이미지 로딩 ========================

    /**
     * URL에서 이미지 로드
     */
    public static void loadImage(@NonNull ImageView imageView, @Nullable String imageUrl) {
        loadImage(imageView.getContext(), imageView, imageUrl, R.drawable.placeholder_image);
    }

    /**
     * URL에서 이미지 로드 (플레이스홀더 지정)
     */
    public static void loadImage(@NonNull ImageView imageView, @Nullable String imageUrl,
                                  @DrawableRes int placeholder) {
        loadImage(imageView.getContext(), imageView, imageUrl, placeholder);
    }

    /**
     * URL에서 이미지 로드 (Context 지정)
     */
    public static void loadImage(@NonNull Context context, @NonNull ImageView imageView,
                                  @Nullable String imageUrl, @DrawableRes int placeholder) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(placeholder);
            return;
        }

        Glide.with(context)
                .load(imageUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    /**
     * Uri에서 이미지 로드
     */
    public static void loadImage(@NonNull ImageView imageView, @Nullable Uri imageUri) {
        loadImage(imageView, imageUri, R.drawable.placeholder_image);
    }

    /**
     * Uri에서 이미지 로드 (플레이스홀더 지정)
     */
    public static void loadImage(@NonNull ImageView imageView, @Nullable Uri imageUri,
                                  @DrawableRes int placeholder) {
        if (imageUri == null) {
            imageView.setImageResource(placeholder);
            return;
        }

        Glide.with(imageView.getContext())
                .load(imageUri)
                .placeholder(placeholder)
                .error(placeholder)
                .into(imageView);
    }

    /**
     * 리소스 ID로 이미지 로드
     */
    public static void loadImage(@NonNull ImageView imageView, @DrawableRes int resourceId) {
        Glide.with(imageView.getContext())
                .load(resourceId)
                .into(imageView);
    }

    // ======================== 원형 이미지 로딩 ========================

    /**
     * URL에서 원형 이미지 로드
     */
    public static void loadCircularImage(@NonNull ImageView imageView, @Nullable String imageUrl) {
        loadCircularImage(imageView, imageUrl, R.drawable.placeholder_profile);
    }

    /**
     * URL에서 원형 이미지 로드 (플레이스홀더 지정)
     */
    public static void loadCircularImage(@NonNull ImageView imageView, @Nullable String imageUrl,
                                          @DrawableRes int placeholder) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(placeholder);
            return;
        }

        Glide.with(imageView.getContext())
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    /**
     * Uri에서 원형 이미지 로드
     */
    public static void loadCircularImage(@NonNull ImageView imageView, @Nullable Uri imageUri) {
        loadCircularImage(imageView, imageUri, R.drawable.placeholder_profile);
    }

    /**
     * Uri에서 원형 이미지 로드 (플레이스홀더 지정)
     */
    public static void loadCircularImage(@NonNull ImageView imageView, @Nullable Uri imageUri,
                                          @DrawableRes int placeholder) {
        if (imageUri == null) {
            imageView.setImageResource(placeholder);
            return;
        }

        Glide.with(imageView.getContext())
                .load(imageUri)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(placeholder)
                .error(placeholder)
                .into(imageView);
    }

    // ======================== 둥근 모서리 이미지 로딩 ========================

    /**
     * URL에서 둥근 모서리 이미지 로드
     */
    public static void loadRoundedImage(@NonNull ImageView imageView, @Nullable String imageUrl,
                                         int cornerRadiusDp) {
        loadRoundedImage(imageView, imageUrl, cornerRadiusDp, R.drawable.placeholder_image);
    }

    /**
     * URL에서 둥근 모서리 이미지 로드 (플레이스홀더 지정)
     */
    public static void loadRoundedImage(@NonNull ImageView imageView, @Nullable String imageUrl,
                                         int cornerRadiusDp, @DrawableRes int placeholder) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(placeholder);
            return;
        }

        Context context = imageView.getContext();
        int radiusPx = dpToPx(context, cornerRadiusDp);

        Glide.with(context)
                .load(imageUrl)
                .transform(new CenterCrop(), new RoundedCorners(radiusPx))
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    // ======================== CenterCrop 이미지 로딩 ========================

    /**
     * URL에서 CenterCrop 이미지 로드
     */
    public static void loadCenterCropImage(@NonNull ImageView imageView, @Nullable String imageUrl) {
        loadCenterCropImage(imageView, imageUrl, R.drawable.placeholder_image);
    }

    /**
     * URL에서 CenterCrop 이미지 로드 (플레이스홀더 지정)
     */
    public static void loadCenterCropImage(@NonNull ImageView imageView, @Nullable String imageUrl,
                                            @DrawableRes int placeholder) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(placeholder);
            return;
        }

        Glide.with(imageView.getContext())
                .load(imageUrl)
                .centerCrop()
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    // ======================== FitCenter 이미지 로딩 ========================

    /**
     * URL에서 FitCenter 이미지 로드
     */
    public static void loadFitCenterImage(@NonNull ImageView imageView, @Nullable String imageUrl) {
        loadFitCenterImage(imageView, imageUrl, R.drawable.placeholder_image);
    }

    /**
     * URL에서 FitCenter 이미지 로드 (플레이스홀더 지정)
     */
    public static void loadFitCenterImage(@NonNull ImageView imageView, @Nullable String imageUrl,
                                           @DrawableRes int placeholder) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(placeholder);
            return;
        }

        Glide.with(imageView.getContext())
                .load(imageUrl)
                .fitCenter()
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    // ======================== 썸네일 이미지 로딩 ========================

    /**
     * URL에서 썸네일 이미지 로드 (빠른 저해상도 미리보기 + 고해상도)
     */
    public static void loadImageWithThumbnail(@NonNull ImageView imageView, @Nullable String imageUrl) {
        loadImageWithThumbnail(imageView, imageUrl, 0.1f);
    }

    /**
     * URL에서 썸네일 이미지 로드 (썸네일 크기 비율 지정)
     */
    public static void loadImageWithThumbnail(@NonNull ImageView imageView, @Nullable String imageUrl,
                                               float thumbnailSizeMultiplier) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder_image);
            return;
        }

        Glide.with(imageView.getContext())
                .load(imageUrl)
                .thumbnail(thumbnailSizeMultiplier)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    // ======================== 비트맵 로딩 ========================

    /**
     * URL에서 Bitmap으로 로드 (콜백)
     */
    public static void loadBitmap(@NonNull Context context, @Nullable String imageUrl,
                                   @NonNull BitmapCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onFailed(new Exception("Image URL is null or empty"));
            return;
        }

        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                        callback.onBitmapLoaded(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 리소스 정리
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        callback.onFailed(new Exception("Failed to load bitmap"));
                    }
                });
    }

    // ======================== 캐시 관리 ========================

    /**
     * 메모리 캐시 클리어
     */
    public static void clearMemoryCache(@NonNull Context context) {
        Glide.get(context).clearMemory();
    }

    /**
     * 디스크 캐시 클리어 (백그라운드 스레드에서 호출해야 함)
     */
    public static void clearDiskCache(@NonNull Context context) {
        new Thread(() -> Glide.get(context).clearDiskCache()).start();
    }

    /**
     * 모든 캐시 클리어
     */
    public static void clearAllCache(@NonNull Context context) {
        clearMemoryCache(context);
        clearDiskCache(context);
    }

    // ======================== 유틸리티 ========================

    /**
     * 이미지 로딩 일시 중지 (스크롤 시 성능 개선)
     */
    public static void pauseRequests(@NonNull Context context) {
        Glide.with(context).pauseRequests();
    }

    /**
     * 이미지 로딩 재개
     */
    public static void resumeRequests(@NonNull Context context) {
        Glide.with(context).resumeRequests();
    }

    /**
     * dp를 px로 변환
     */
    private static int dpToPx(@NonNull Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    /**
     * 이미지 URL이 유효한지 확인
     */
    public static boolean isValidImageUrl(@Nullable String url) {
        return url != null && !url.isEmpty() &&
                (url.startsWith("http://") || url.startsWith("https://") ||
                        url.startsWith("file://") || url.startsWith("content://"));
    }

    // ======================== 콜백 인터페이스 ========================

    /**
     * 비트맵 로딩 콜백
     */
    public interface BitmapCallback {
        void onBitmapLoaded(Bitmap bitmap);
        void onFailed(Exception e);
    }
}
