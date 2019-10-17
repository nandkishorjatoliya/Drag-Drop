package com.dragdrop.model;

public class SidesLength {
    public SidesLength(String side, int length) {
        this.side = side;
        this.length = length;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    String side;
    int length;
}
