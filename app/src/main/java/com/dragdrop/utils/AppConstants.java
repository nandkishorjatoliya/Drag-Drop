package com.dragdrop.utils;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.DragEvent;
import android.view.View;

public class AppConstants {
    public static final String VERTICAL_LINE_SMALL = "verticalLineSmall";
    public static final String VERTICAL_LINE_MEDIUM = "verticalLineMedium";
    public static final String VERTICAL_LINE_LARGE = "verticalLineLarge";
    public static final String HORIZONTAL_LINE_SMALL = "horizontalLineSmall";
    public static final String HORIZONTAL_LINE_MEDIUM = "horizontalLineMedium";
    public static final String HORIZONTAL_LINE_LARGE = "horizontalLineLarge";
    public static final String TOP_RIGHT = "topRight";
    public static final String TOP_LEFT = "topLeft";
    public static final String BOTTOM_RIGHT = "bottomRight";
    public static final String BOTTOM_LEFT = "bottomLeft";
    public static final String HORIZONTAL_BLOCK = "horizontalBlock";
    public static final String VERTICAL_BLOCK = "verticalBlock";
    public static final int EXISTING = 1;
    public static final int NEW = 2;
    public static boolean isTouchInsideOfView(View view, Point touchPosition) {
        Rect rScroll = new Rect();
        view.getGlobalVisibleRect(rScroll);
        return isTouchInsideOfRect(touchPosition, rScroll);
    }

    public static boolean isTouchInsideOfRect(Point touchPosition, Rect rScroll) {
        return touchPosition.x > rScroll.left && touchPosition.x < rScroll.right //within x axis / width
                && touchPosition.y > rScroll.top && touchPosition.y < rScroll.bottom; //withing y axis / height
    }
    public static Point getTouchPositionFromDragEvent(View item, DragEvent event) {
        Rect rItem = new Rect();
        item.getGlobalVisibleRect(rItem);
        return new Point(rItem.left + Math.round(event.getX()), rItem.top + Math.round(event.getY()));
    }
}
