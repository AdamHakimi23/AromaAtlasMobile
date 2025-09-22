package com.example.aromaatlas;

public class CartItem {
    private String name;
    private String drinkType;
    private double price;
    private int quantity;
    private String note;

    public CartItem() {
        // Default constructor for serialization/deserialization
    }

    public CartItem(String name, String drinkType, double price, int quantity, String note) {
        this.name = name;
        this.drinkType = drinkType;
        this.price = price;
        this.quantity = quantity;
        this.note = note;
    }

    public String getName() {return name;}

    public void setName(String name) {
        this.name = name;
    }

    public String getDrinkType() {
        return drinkType;
    }

    public void setDrinkType(String drinkType) {
        this.drinkType = drinkType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
