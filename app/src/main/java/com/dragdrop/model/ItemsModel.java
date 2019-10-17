package com.dragdrop.model;

import android.view.View;

public class ItemsModel {

    public ItemsModel(String itemId, View view, NodeModel top, NodeModel left, NodeModel right, NodeModel bottom, float posX, float posY, String type) {
        this.itemId = itemId;
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.posX = posX;
        this.posY = posY;
        this.type = type;
        this.view = view;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public NodeModel getTop() {
        return top;
    }

    public void setTop(NodeModel top) {
        this.top = top;
    }

    public NodeModel getLeft() {
        return left;
    }

    public void setLeft(NodeModel left) {
        this.left = left;
    }

    public NodeModel getRight() {
        return right;
    }

    public void setRight(NodeModel right) {
        this.right = right;
    }

    public NodeModel getBottom() {
        return bottom;
    }

    public void setBottom(NodeModel bottom) {
        this.bottom = bottom;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    private String itemId;
    private NodeModel top;
    private NodeModel left;
    private NodeModel right;
    private NodeModel bottom;
    private float posX, posY;
    private String type;
    private View view;
}
