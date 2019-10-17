package com.dragdrop.model;

import android.view.View;

public class BoxModel {
    private String id;
    private int posX;
    private int posY;
    private View view;

    public BoxModel(String id, int posX, int posY, View view) {
        this.id = id;
        this.posX = posX;
        this.posY = posY;
        this.view = view;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }
}
