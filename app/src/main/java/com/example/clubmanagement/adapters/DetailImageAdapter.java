package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.R;

import java.util.List;

public class DetailImageAdapter extends RecyclerView.Adapter<DetailImageAdapter.ImageViewHolder> {

    private List<Object> images; // Can be Integer (drawable res) or String (URL)

    public DetailImageAdapter(List<Object> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detail_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Object image = images.get(position);
        holder.bind(image);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void updateImages(List<Object> newImages) {
        this.images = newImages;
        notifyDataSetChanged();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivDetailImage);
        }

        public void bind(Object image) {
            if (image instanceof Integer) {
                // Load from drawable resource
                Glide.with(itemView.getContext())
                        .load((Integer) image)
                        .centerCrop()
                        .into(ivImage);
            } else if (image instanceof String) {
                // Load from URL
                Glide.with(itemView.getContext())
                        .load((String) image)
                        .centerCrop()
                        .into(ivImage);
            }
        }
    }
}
