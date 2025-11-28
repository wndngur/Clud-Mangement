package com.example.clubmanagement.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
        private ImageView ivCarouselImage;
        private ImageView ivCarouselBackground;
        private TextView tvCarouselTitle;
        private TextView tvCarouselDescription;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCarouselImage = itemView.findViewById(R.id.ivCarouselImage);
            ivCarouselBackground = itemView.findViewById(R.id.ivCarouselBackground);
            tvCarouselTitle = itemView.findViewById(R.id.tvCarouselTitle);
            tvCarouselDescription = itemView.findViewById(R.id.tvCarouselDescription);
        }

        public void bind(CarouselItem item, int position) {
            // Set title and description
            tvCarouselTitle.setText(item.getTitle());
            tvCarouselDescription.setText(item.getDescription());

            // Load image from Firebase URL or local resource
            if (item.hasFirebaseImage()) {
                // Load from Firebase Storage URL
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .centerCrop()
                        .into(ivCarouselImage);
                ivCarouselImage.setVisibility(View.VISIBLE);

                // Hide background when image is loaded
                ivCarouselBackground.setVisibility(View.GONE);
            } else if (item.getImageRes() != 0) {
                // Load from local resource
                ivCarouselImage.setImageResource(item.getImageRes());
                ivCarouselImage.setVisibility(View.VISIBLE);
                ivCarouselBackground.setVisibility(View.GONE);
            } else {
                // Show colored background if no image
                ivCarouselImage.setVisibility(View.GONE);
                ivCarouselBackground.setVisibility(View.VISIBLE);

                // Use custom background color or default
                if (item.getBackgroundColor() != null && !item.getBackgroundColor().isEmpty()) {
                    try {
                        ivCarouselBackground.setBackgroundColor(Color.parseColor(item.getBackgroundColor()));
                    } catch (Exception e) {
                        ivCarouselBackground.setBackgroundColor(getDefaultColor(position));
                    }
                } else {
                    ivCarouselBackground.setBackgroundColor(getDefaultColor(position));
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
