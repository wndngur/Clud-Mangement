package com.example.clubmanagement.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.CarouselItem;

import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private List<CarouselItem> items;

    public CarouselAdapter(List<CarouselItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        CarouselItem item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCarouselBackground;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCarouselBackground = itemView.findViewById(R.id.ivCarouselBackground);
        }

        public void bind(CarouselItem item, int position) {
            // 기본 배경색 먼저 설정 (이미지 로드 실패 시를 대비)
            ivCarouselBackground.setBackgroundColor(getDefaultColor(position));

            // Load background image from Firebase URL or local resource
            if (item.hasFirebaseImage()) {
                // Load background from Firebase Storage URL
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .centerCrop()
                        .placeholder(new android.graphics.drawable.ColorDrawable(getDefaultColor(position)))
                        .error(new android.graphics.drawable.ColorDrawable(getDefaultColor(position)))
                        .into(ivCarouselBackground);
            } else if (item.getImageRes() != 0) {
                // Load background from local drawable resource
                Glide.with(itemView.getContext())
                        .load(item.getImageRes())
                        .centerCrop()
                        .placeholder(new android.graphics.drawable.ColorDrawable(getDefaultColor(position)))
                        .error(new android.graphics.drawable.ColorDrawable(getDefaultColor(position)))
                        .into(ivCarouselBackground);
            } else {
                // Use custom background color or default
                if (item.getBackgroundColor() != null && !item.getBackgroundColor().isEmpty()) {
                    try {
                        ivCarouselBackground.setBackgroundColor(Color.parseColor(item.getBackgroundColor()));
                    } catch (Exception e) {
                        ivCarouselBackground.setBackgroundColor(getDefaultColor(position));
                    }
                }
            }
        }

        private int getDefaultColor(int position) {
            int[] colors = {
                0xFF6200EA,  // 보라색
                0xFF00C853,  // 초록색
                0xFFFF6D00   // 주황색
            };
            return colors[position % colors.length];
        }
    }
}
