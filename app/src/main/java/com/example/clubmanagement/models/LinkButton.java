package com.example.clubmanagement.models;

public class LinkButton {
    private String id;
    private String label;        // Button text
    private String url;          // External link
    private int position;        // Order in list
    private String iconName;     // Optional icon name

    // Firebase requires no-argument constructor
    public LinkButton() {
    }

    public LinkButton(String id, String label, String url, int position) {
        this.id = id;
        this.label = label;
        this.url = url;
        this.position = position;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }

    public int getPosition() {
        return position;
    }

    public String getIconName() {
        return iconName;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
}
