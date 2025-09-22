package com.example.aromaatlas;

import java.util.List;
import java.util.Map;
import com.google.firebase.Timestamp;

public class OrderModel {
    private List<Map<String, Object>> items;
    private String status;
    private String cafeId;
    private String ownerId;
    private Timestamp pickupDateTime; // ✅ ADD THIS

    public OrderModel() {}

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public String getStatus() {
        return status;
    }

    public String getCafeId() {
        return cafeId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Timestamp getPickupDateTime() { // ✅ GETTER
        return pickupDateTime;
    }
}
