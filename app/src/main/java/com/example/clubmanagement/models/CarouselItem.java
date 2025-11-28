package com.example.clubmanagement.models;

public class CarouselItem {
    private String id;              // Firestore document ID
    private int imageRes;           // Local drawable resource (for default)
    private String imageUrl;        // Firebase Storage URL
    private String title;
    private String description;
    private int position;           // Order in carousel (0, 1, 2)
    private String backgroundColor; // Hex color code

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
}
