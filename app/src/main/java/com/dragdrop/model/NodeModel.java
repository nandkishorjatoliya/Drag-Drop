package com.dragdrop.model;

public class NodeModel {

    private boolean status;
    private int address;
    private String itemId;


    public NodeModel(boolean status, int address, String itemId) {
        this.status = status;
        this.address = address;
        this.itemId = itemId;
    }

    public NodeModel(boolean status) {
        if (status) {
            this.address = -1;
        } else {
            this.address = -2;
        }
        this.status = status;
        this.itemId = "";
    }


    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }


}
