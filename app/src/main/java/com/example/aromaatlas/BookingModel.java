package com.example.aromaatlas;

import com.google.firebase.Timestamp;

public class BookingModel {
    private String tableNo;
    private String userId;
    private String bookingType;
    private Timestamp bookingDateTime;
    private String cafeId;
    private String ownerId; // ✅ Required for query to work

    public BookingModel() {
        // Needed for Firebase deserialization
    }

    public String getTableNo() {
        return tableNo;
    }

    public void setTableNo(String tableNo) {
        this.tableNo = tableNo;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBookingType() {
        return bookingType;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

    public Timestamp getBookingDateTime() {
        return bookingDateTime;
    }

    public void setBookingDateTime(Timestamp bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    public String getCafeId() {
        return cafeId;
    }

    public void setCafeId(String cafeId) {
        this.cafeId = cafeId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
