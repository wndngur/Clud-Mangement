package com.example.clubmanagement.models;

import java.util.ArrayList;
import java.util.List;

public class CarouselItem {
    private String id;              // Firestore document ID
    private int imageRes;           // Local drawable resource (for default)
    private String imageUrl;        // Firebase Storage URL (single image for backward compatibility)
    private List<String> imageUrls; // Multiple image URLs (max 3)
    private String title;
    private String description;
    private int position;           // Order in carousel (0, 1, 2)
    private String backgroundColor; // Hex color code
    private String clubId;          // 연결된 동아리 ID
    private String clubName;        // 연결된 동아리 이름

    public static final int MAX_IMAGES = 3;

    // Firebase requires no-argument constructor
    public CarouselItem() {
    }

    // Constructor for local resources (backward compatibility)
    public CarouselItem(int imageRes, String title, String description) {
        this.imageRes = imageRes;
        this.title = title;
        this.description = description;
        this.imageUrl = null;
    }

    // Constructor for Firebase data
    public CarouselItem(String id, String imageUrl, String title, String description, int position, String backgroundColor) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
        this.position = position;
        this.backgroundColor = backgroundColor;
        this.imageRes = 0;
    }

    // Getters
    public String getId() {
        return id;
    }

    public int getImageRes() {
        return imageRes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPosition() {
        return position;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setImageRes(int imageRes) {
        this.imageRes = imageRes;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    // Helper method to check if using Firebase image
    public boolean hasFirebaseImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public String getClubId() {
        return clubId;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    // Multiple images support
    public List<String> getImageUrls() {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    // Get all available images (combines single imageUrl and imageUrls list)
    public List<String> getAllImageUrls() {
        List<String> allUrls = new ArrayList<>();

        // Add imageUrls list first
        if (imageUrls != null && !imageUrls.isEmpty()) {
            allUrls.addAll(imageUrls);
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            // Fall back to single imageUrl for backward compatibility
            allUrls.add(imageUrl);
        }

        return allUrls;
    }

    // Add an image URL (max 3)
    public boolean addImageUrl(String url) {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        if (imageUrls.size() >= MAX_IMAGES) {
            return false;
        }
        imageUrls.add(url);
        // Also update single imageUrl for backward compatibility
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = url;
        }
        return true;
    }

    // Remove an image URL at index
    public void removeImageUrl(int index) {
        if (imageUrls != null && index >= 0 && index < imageUrls.size()) {
            imageUrls.remove(index);
            // Update single imageUrl for backward compatibility
            if (imageUrls.isEmpty()) {
                imageUrl = null;
            } else {
                imageUrl = imageUrls.get(0);
            }
        }
    }

    // Check if can add more images
    public boolean canAddMoreImages() {
        return getImageUrls().size() < MAX_IMAGES;
    }

    // Get image count
    public int getImageCount() {
        return getImageUrls().size();
    }
}
