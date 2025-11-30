package com.example.clubmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Banner;

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Banner> banners = new ArrayList<>();
    private Context context;
    private OnBannerClickListener listener;

    public interface OnBannerClickListener {
        void onBannerClick(Banner banner);
        void onBannerLongClick(Banner banner);
    }

    public BannerAdapter(Context context) {
        this.context = context;
    }

    public void setOnBannerClickListener(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners != null ? banners : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Banner> getBanners() {
        return banners;
    }

    public Banner getBannerAt(int position) {
        if (position >= 0 && position < banners.size()) {
            return banners.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = banners.get(position);
        holder.bind(banner);
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    class BannerViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivBannerImage;
        private LinearLayout layoutLinkIndicator;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBannerImage = itemView.findViewById(R.id.ivBannerImage);
            layoutLinkIndicator = itemView.findViewById(R.id.layoutLinkIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBannerClick(banners.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBannerLongClick(banners.get(position));
                    return true;
                }
                return false;
            });
        }

        public void bind(Banner banner) {
            // Load image only
            if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(banner.getImageUrl())
                        .centerCrop()
                        .into(ivBannerImage);
            } else {
                ivBannerImage.setImageResource(android.R.color.darker_gray);
            }

            // Show link indicator if link exists
            if (banner.getLinkUrl() != null && !banner.getLinkUrl().isEmpty()) {
                layoutLinkIndicator.setVisibility(View.VISIBLE);
            } else {
                layoutLinkIndicator.setVisibility(View.GONE);
            }
        }
    }
}
