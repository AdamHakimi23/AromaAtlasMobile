// File: MenuItemModel.java
package com.example.aromaatlas;

public class MenuItemModel {
    private String name;
    private String bean;
    private String drink;
    private double price;
    private String image;
    private String ownerId;
    private String cafeId;
    private boolean isFavorite; // New field
    private String id;

    public MenuItemModel() {
        // Required by Firestore
    }


    public MenuItemModel(String name, String bean, String drink, double price,
                         String image, String ownerId, String cafeId) {
        this.name = name;
        this.bean = bean;
        this.drink = drink;
        this.price = price;
        this.image = image;
        this.ownerId = ownerId;
        this.cafeId = cafeId;
        this.isFavorite = false; // Default false
    }

    // Getters
    public String getName() { return name; }
    public String getBean() { return bean; }
    public String getDrink() { return drink; }
    public double getPrice() { return price; }
    public String getImage() { return image; }
    public String getOwnerId() { return ownerId; }
    public String getCafeId() { return cafeId; }
    public boolean isFavorite() { return isFavorite; }
    public String getId() { return id; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setBean(String bean) { this.bean = bean; }
    public void setDrink(String drink) { this.drink = drink; }
    public void setPrice(double price) { this.price = price; }
    public void setImage(String image) { this.image = image; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setId(String id) { this.id = id; }
}
