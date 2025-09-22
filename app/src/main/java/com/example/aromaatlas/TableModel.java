package com.example.aromaatlas;

public class TableModel {
    private String tableNo;
    private int capacity;
    private String status;
    private String cafeId;
    private String ownerId;

    public TableModel() {}

    public String getTableNo() { return tableNo; }
    public int getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public String getCafeId() { return cafeId; }
    public String getOwnerId() { return ownerId; }

    public void setTableNo(String tableNo) { this.tableNo = tableNo; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setStatus(String status) { this.status = status; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}

