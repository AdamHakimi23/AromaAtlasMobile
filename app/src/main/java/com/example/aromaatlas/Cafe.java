package com.example.aromaatlas;

public class Cafe {
    private String name;
    private String description;
    private String workingDay;
    private String workingHours;
    private String imageUrl;
    private String cafeId;
    private String ownerId;
    private double latitude;
    private double longitude;
    private String bean;

    public Cafe() {} // Required by Firestore

    public Cafe(String name, String description, String workingDay, String workingHours, String imageUrl, String cafeId, String ownerId, double latitude, double longitude, String bean) {
        this.name = name;
        this.description = description;
        this.workingDay = workingDay;
        this.workingHours = workingHours;
        this.imageUrl = imageUrl;
        this.cafeId = cafeId;
        this.ownerId = ownerId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bean = bean;
    }

    @Override
    public String toString() {
        return name != null ? name : "Unnamed Cafe";
    }


    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getWorkingDay() { return workingDay; }
    public String getWorkingHours() { return workingHours; }
    public String getImageUrl() { return imageUrl; }
    public String getCafeId() { return cafeId; }
    public String getOwnerId() { return ownerId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getBean() { return bean; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setWorkingDay(String workingDay) { this.workingDay = workingDay; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setBean(String bean) { this.bean = bean; }
}
