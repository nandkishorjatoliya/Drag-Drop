package com.dragdrop.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dragdrop.R;
import com.dragdrop.model.BoxModel;
import com.dragdrop.model.ItemsModel;
import com.dragdrop.model.NodeModel;
import com.dragdrop.model.SidesLength;
import com.dragdrop.utils.AppConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnLongClick;

public class DragActivity extends AppCompatActivity implements View.OnLongClickListener, View.OnDragListener {
    public int oldX, oldY;
    private static final String TAG = DragActivity.class.getCanonicalName();
    @BindView(R.id.line_horizontal_small)
    ImageView lineHorizontalSmall;
    @BindView(R.id.line_horizontal_medium)
    ImageView lineHorizontalMedium;
    @BindView(R.id.line_horizontal_large)
    ImageView lineHorizontalLarge;
    @BindView(R.id.top_right)
    ImageView topRight;
    @BindView(R.id.bottom_right)
    ImageView bottomRight;
    @BindView(R.id.bottom_left)
    ImageView bottomLeft;
    @BindView(R.id.top_left)
    ImageView topLeft;
    @BindView(R.id.line_vertical_small)
    ImageView lineVerticalSmall;
    @BindView(R.id.line_vertical_medium)
    ImageView lineVerticalMedium;
    @BindView(R.id.line_vertical_large)
    ImageView lineVerticalLarge;
    @BindView(R.id.ivHorizonal)
    RelativeLayout ivHorizonalBlock;

    @BindView(R.id.ivVertical)
    RelativeLayout ivVerticalBlock;

    @BindView(R.id.flMainDragContainer)
    FrameLayout flMainDragContainer;
    ArrayList<BoxModel> boxList = new ArrayList<>();
    private long currentTime;
    private View tempDeleteView;
    private int count = 0;
    private Activity activity;
    private List<ItemsModel> box2Item = new ArrayList<>();
    private List<ItemsModel> itemsModels = new ArrayList<>();
    private List<ItemsModel> box1Items = new ArrayList<>();
    private NodeModel falseNode = new NodeModel(false);
    private NodeModel trueNode = new NodeModel(true);
    private List<Integer> tempItemPos = new ArrayList<>();
    private boolean isConnected = false;
    private List<SidesLength> sidesLengths = new ArrayList<>();
    int viewStatus;
    View.OnLongClickListener existingLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            viewStatus = AppConstants.EXISTING;
            ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
            String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
            ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
            View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
            v.startDrag(data, dragshadow, v, 0);
            return true;
        }
    };
    private ImageView currentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drag);
        ButterKnife.bind(this);
        activity = this;
        setTagToViews();
        setDragListeners();
    }

    private void setLineView(ViewGroup container, DragEvent event) {

        View vw = (View) event.getLocalState();
        //final RelativeLayout container = (RelativeLayout) v;
        ImageView oldView = (ImageView) vw;
        if (!isViewInsideForLine(oldView, event)) {
            return;
        }
        boolean isViewNew = true;
        int pos = checkBoxPosition(event, container, oldView);
        Log.i(TAG, "setLineView: boxPOsition: " + pos);
        if (pos == 0)
            itemsModels = box1Items;
        else
            itemsModels = box2Item;
        isViewNew = checkIsViewIsNew(oldView);
        tempDeleteView = oldView;
        if (!isViewNew) {
            tempDeleteView = oldView;
            deleteItem(oldView, container);
        }

        final ImageView newView = new ImageView(activity);
        newView.setImageDrawable(((oldView.getDrawable())));
        newView.setTag(oldView.getTag().toString());
        newView.setOnDragListener(this);
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tempDeleteView = v;
            }
        });
        newView.setOnLongClickListener(existingLongClickListener);
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long diff = System.currentTimeMillis() - currentTime;
                if (diff < 1000) {
                    int pos = checkBoxPosition(event, container, oldView);
                    showDeleteDialog(newView, container, false, pos);
                }
                currentTime = System.currentTimeMillis();
            }
        });

        if (itemsModels.size() == 0)
            addItemToBox(oldView, newView, event);
        else
            findItemWithMinDistance(oldView, newView, event);
        container.addView(newView);
        vw.setVisibility(View.VISIBLE);
        newView.setVisibility(View.INVISIBLE);
        boolean finalIsViewNew = isViewNew;
        new Handler().postDelayed(() -> {
            if (viewIsOverlaping(newView)) {
                activity.runOnUiThread(() -> {
                    tempDeleteView = newView;
                    deleteItem(newView, container);
                    if (!finalIsViewNew) {
                        container.addView(oldView);
                    }
                });
            } else {
                activity.runOnUiThread(() -> {
                    newView.setVisibility(View.VISIBLE);
                });
            }
        }, 100);
        count++;
    }

    private int checkBoxPosition(DragEvent event, View container, View oldView) {
        Point p = AppConstants.getTouchPositionFromDragEvent(container, event);
        for (int i = 0; i < boxList.size(); i++) {
            View secondView = boxList.get(i).getView();
            boolean b = AppConstants.isTouchInsideOfView(secondView, p);
            if (b) {
                return i;
            }
        }
        return 0;
    }

    private boolean viewIsOverlaping(View v) {
        boolean isOverlap = false;
        if (itemsModels.size() != 0) {
            for (int i = 0; i < itemsModels.size(); i++) {
                if (!v.getTag().toString().equals(itemsModels.get(i).getItemId())
                        && isViewOverlapping(itemsModels.get(i).getView(), v)) {
                    isOverlap = true;
                    break;
                }
            }
        }
        if (isOverlap) {
            Toast.makeText(activity, "View is overlapping.", Toast.LENGTH_SHORT).show();
        }
        return isOverlap;
    }

    private boolean isViewOverlapping(View firstView, View secondView) {
        int[] firstPosition = new int[2];
        int[] secondPosition = new int[2];

        firstView.getLocationOnScreen(firstPosition);
        secondView.getLocationOnScreen(secondPosition);

        // Rect constructor parameters: left, top, right, bottom
        Rect rectFirstView = new Rect(firstPosition[0], firstPosition[1],
                firstPosition[0] + firstView.getMeasuredWidth(), firstPosition[1] + firstView.getMeasuredHeight());
        Rect rectSecondView = new Rect(secondPosition[0], secondPosition[1],
                secondPosition[0] + secondView.getMeasuredWidth(), secondPosition[1] + secondView.getMeasuredHeight());
        return rectFirstView.intersect(rectSecondView);
    }

    private boolean isViewInside(ImageView firstView) {
        for (BoxModel model : boxList) {
            View secondView = model.getView();
            int[] firstPosition = new int[2];
            int[] secondPosition = new int[2];

            firstView.getLocationOnScreen(firstPosition);
            secondView.getLocationOnScreen(secondPosition);

            // Rect constructor parameters: left, top, right, bottom
            Rect rectFirstView = new Rect(firstPosition[0], firstPosition[1],
                    firstPosition[0] + firstView.getMeasuredWidth(), firstPosition[1] + firstView.getMeasuredHeight());
            Rect rectSecondView = new Rect(secondPosition[0], secondPosition[1],
                    secondPosition[0] + secondView.getMeasuredWidth(), secondPosition[1] + secondView.getMeasuredHeight());
            boolean isInside = rectSecondView.contains(rectFirstView);
            if (!isInside) {
                Toast.makeText(activity, "View outside Container.", Toast.LENGTH_SHORT).show();
            }
            return isInside;
        }
        return false;
    }

    private void addItemToBox(ImageView oldView, ImageView newView, DragEvent event) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(oldView.getWidth(), oldView.getHeight());
        lp.setMargins((int) event.getX() - oldView.getWidth() / 2, (int) event.getY() - oldView.getHeight() / 2, 0, 0);
        newView.setLayoutParams(lp);
        addItemtoPojo(newView, event.getX() - oldView.getWidth() / 2, event.getY() - oldView.getHeight() / 2);
    }

    private void addItemtoPojo(ImageView imageView, float posX, float posy) {


        String newTag = imageView.getTag().toString() + count;
        switch (imageView.getTag().toString().replaceAll("\\d", "")) {

            case AppConstants.HORIZONTAL_LINE_SMALL:
                newTag = AppConstants.HORIZONTAL_LINE_SMALL + count;
                itemsModels.add(new ItemsModel(newTag, imageView, falseNode, trueNode, trueNode, falseNode, posX, posy, AppConstants.HORIZONTAL_LINE_SMALL));
                break;

            case AppConstants.HORIZONTAL_LINE_MEDIUM:
                newTag = AppConstants.HORIZONTAL_LINE_MEDIUM + count;
                itemsModels.add(new ItemsModel(newTag, imageView, falseNode, trueNode, trueNode, falseNode, posX, posy, AppConstants.HORIZONTAL_LINE_MEDIUM));
                break;

            case AppConstants.HORIZONTAL_LINE_LARGE:
                newTag = AppConstants.HORIZONTAL_LINE_LARGE + count;
                itemsModels.add(new ItemsModel(newTag, imageView, falseNode, trueNode, trueNode, falseNode, posX, posy, AppConstants.HORIZONTAL_LINE_LARGE));
                break;

            case AppConstants.VERTICAL_LINE_SMALL:
                newTag = AppConstants.VERTICAL_LINE_SMALL + count;
                itemsModels.add(new ItemsModel(newTag, imageView, trueNode, falseNode, falseNode, trueNode, posX, posy, AppConstants.VERTICAL_LINE_SMALL));
                break;

            case AppConstants.VERTICAL_LINE_MEDIUM:
                newTag = AppConstants.VERTICAL_LINE_MEDIUM + count;
                itemsModels.add(new ItemsModel(newTag, imageView, trueNode, falseNode, falseNode, trueNode, posX, posy, AppConstants.VERTICAL_LINE_MEDIUM));
                break;

            case AppConstants.VERTICAL_LINE_LARGE:
                newTag = AppConstants.VERTICAL_LINE_LARGE + count;
                itemsModels.add(new ItemsModel(newTag, imageView, trueNode, falseNode, falseNode, trueNode, posX, posy, AppConstants.VERTICAL_LINE_LARGE));
                break;

            case AppConstants.TOP_RIGHT:
                newTag = AppConstants.TOP_RIGHT + count;
                itemsModels.add(new ItemsModel(newTag, imageView, falseNode, trueNode, falseNode, trueNode, posX, posy, AppConstants.TOP_RIGHT));
                break;

            case AppConstants.TOP_LEFT:
                newTag = AppConstants.TOP_LEFT + count;
                itemsModels.add(new ItemsModel(newTag, imageView, falseNode, falseNode, trueNode, trueNode, posX, posy, AppConstants.TOP_LEFT));
                break;

            case AppConstants.BOTTOM_RIGHT:
                newTag = AppConstants.BOTTOM_RIGHT + count;
                itemsModels.add(new ItemsModel(newTag, imageView, trueNode, trueNode, falseNode, falseNode, posX, posy, AppConstants.BOTTOM_RIGHT));
                break;

            case AppConstants.BOTTOM_LEFT:
                newTag = AppConstants.BOTTOM_LEFT + count;
                itemsModels.add(new ItemsModel(newTag, imageView, trueNode, falseNode, trueNode, falseNode, posX, posy, AppConstants.BOTTOM_LEFT));
                break;
        }
        imageView.setTag(newTag);
    }

    private boolean checkIfBoxIsNew(ImageView oldView) {
        if (boxList.size() != 0) {
            for (int i = 0; i < boxList.size(); i++) {
                if (oldView.getTag().equals(boxList.get(i).getId())) {
                    Log.e("Matched", oldView.getTag().toString() + " ");
                    return false;
                }
            }
        }
        return true;
    }

    private void setBoxView(View v, DragEvent event) {
        View vw = (View) event.getLocalState();
        final FrameLayout container = (FrameLayout) v;
        RelativeLayout oldView = (RelativeLayout) vw;
        if (!isViewInsideForBox(oldView, event)) {
            return;
        }
        int pos = boxIsToUsed(vw);
        boolean isNewView = checkIfBoxIsNew(oldView);
        Log.i(TAG, "setBoxView: isViewNew " + isNewView);
        if (!isNewView) {
            tempDeleteView = deleteBox(vw);
            container.removeView(vw);
        } else {

            if (boxList.size() >= 2) {
                Toast.makeText(activity, "At most 2 boxes can be dropped", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        final RelativeLayout newView = new RelativeLayout(this);
        newView.setBackground(oldView.getBackground());
        newView.setTag(oldView.getTag().toString());
        newView.setOnDragListener(this);
        newView.setOnLongClickListener(existingLongClickListener);
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long diff = System.currentTimeMillis() - currentTime;
                if (diff < 1000) {
                    showDeleteDialog(newView, container, true, 0);
                }
                currentTime = System.currentTimeMillis();
            }
        });
        if (!isNewView) {
            List<ItemsModel> tempList;
            if (pos == 0)
                tempList = box1Items;
            else
                tempList = box2Item;
            for (ItemsModel model : tempList) {
                ImageView imageView = new ImageView(this);
                imageView.setOnLongClickListener(this);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long diff = System.currentTimeMillis() - currentTime;
                        if (diff < 1000) {
                            showDeleteDialog(imageView, newView, false, pos);
                        }
                        currentTime = System.currentTimeMillis();
                    }
                });
                imageView.setImageDrawable(((ImageView) model.getView()).getDrawable());
                imageView.setTag(model.getItemId());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(model.getView().getWidth(), model.getView().getHeight());
                lp.setMargins((int) model.getPosX(), (int) model.getPosY(), 0, 0);
                imageView.setLayoutParams(lp);
                newView.addView(imageView);
            }
        }
        if (isNewViewOverlapping(oldView, event)) {
            return;
        }
        Log.i(TAG, "setBoxView: child count: " + count);
        setLayoutParamsView(oldView, newView, event);
        container.addView(newView);
        vw.setVisibility(View.VISIBLE);
        newView.setVisibility(View.VISIBLE);

        String tag = oldView.getTag().toString().replaceAll("\\d", "") + count;
        boxList.add(new BoxModel(tag, (int) event.getX(), (int) event.getY(), newView));
        newView.setTag(tag);
        count++;
    }

    private int boxIsToUsed(View oldView) {
        String tag = oldView.getTag().toString();
        for (int i = 0; i < boxList.size(); i++) {
            if (tag.contains(boxList.get(i).getId())) {
                Log.e("Matched", oldView.getTag().toString() + " ");
                return i;
            }
        }
        return 0;

    }

    private boolean isNewViewOverlapping(View oldView, DragEvent event) {
        for (BoxModel model : boxList) {
            if (isViewOverLapping(oldView, model, event)) {
                Toast.makeText(this, "Box overlapping", Toast.LENGTH_SHORT).show();
                return true;
            }

        }
        return false;
    }

    private boolean isViewOverLapping(View oldView, BoxModel model, DragEvent event) {
        Rect rect1 = new Rect((int) event.getX() - oldView.getWidth() / 2, (int) event.getY() - oldView.getHeight() / 2,
                (int) event.getX() + oldView.getWidth() / 2, (int) event.getY() + oldView.getHeight() / 2);
        Rect rect2 = new Rect((int) model.getPosX() - model.getView().getWidth() / 2, (int) model.getPosY() - model.getView().getHeight() / 2,
                (int) model.getPosX() + model.getView().getWidth() / 2, (int) model.getPosY() + model.getView().getHeight() / 2);

        return rect1.intersect(rect2);
    }

    private void showDeleteDialog(final View view, View container, boolean isBox, int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete!");
        builder.setMessage("Do you want to delete this element?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (isBox) {
                    deleteBox(view);
                    if (pos == 0) {
                        box1Items.clear();
                    } else {
                        box2Item.clear();
                    }
                    ((ViewGroup) container).removeView(view);
                } else {
                    if (pos == 0) {
                        itemsModels = box1Items;
                    } else {
                        itemsModels = box2Item;
                    }
                    deleteItem(view, container);
                }

            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.create().show();
    }

    /*
    Method used to remove box from list
     */
    private View deleteBox(View oldView) {
        ListIterator<BoxModel> iterator = boxList.listIterator();
        while (iterator.hasNext()) {
            BoxModel boxModel = iterator.next();
            if (oldView.getTag().toString().contains(boxModel.getId())) {
                iterator.remove();
                return boxModel.getView();

            }
        }
        return null;

    }

    private boolean isViewsAreEqual(View oldView, View view2, DragEvent event) {
        int eventstartx = (int) event.getX();
        int eventstarty = (int) event.getY();
        int eventendx = (int) event.getX() + oldView.getWidth();
        int eventendy = (int) event.getY() + oldView.getHeight();


        int event1startx = (int) view2.getX();
        int event1starty = (int) view2.getY();
        int event1endx = (int) view2.getX() + view2.getWidth();
        int event1endy = (int) view2.getY() + view2.getHeight();
        return eventstartx == event1startx && eventstarty == event1starty
                && eventendx == event1endx && eventendy == event1endy;
    }


    private boolean isViewsAreEqual(View oldView, View view2) {
        int eventstartx = (int) oldView.getX();
        int eventstarty = (int) oldView.getY();
        int eventendx = (int) oldView.getX() + oldView.getWidth();
        int eventendy = (int) oldView.getY() + oldView.getHeight();


        int event1startx = (int) view2.getX();
        int event1starty = (int) view2.getY();
        int event1endx = (int) view2.getX() + view2.getWidth();
        int event1endy = (int) view2.getY() + view2.getHeight();
        return eventstartx == event1startx && eventstarty == event1starty
                && eventendx == event1endx && eventendy == event1endy;
    }

    private boolean checkIsViewIsNew(View oldView) {
        for (int i = 0; i < itemsModels.size(); i++) {
            if (oldView.getTag().equals(itemsModels.get(i).getItemId())) {
                Log.e("Matched", oldView.getTag().toString() + " ");
                return false;
            }
        }
        return true;
    }

    private boolean checkIfBoxIsNew(View oldView) {
        String tag = oldView.getTag().toString();
        for (int i = 0; i < boxList.size(); i++) {
            if (tag.contains(boxList.get(i).getId())) {
                Log.e("Matched", oldView.getTag().toString() + " ");
                return false;
            }
        }
        return true;
    }


    private void setDragListeners() {
        flMainDragContainer.setOnDragListener(this);
    }

    private void setTagToViews() {
        ivHorizonalBlock.setTag(AppConstants.HORIZONTAL_BLOCK);
        ivVerticalBlock.setTag(AppConstants.VERTICAL_BLOCK);
        lineVerticalSmall.setTag(AppConstants.VERTICAL_LINE_SMALL);
        lineVerticalMedium.setTag(AppConstants.VERTICAL_LINE_MEDIUM);
        lineVerticalLarge.setTag(AppConstants.VERTICAL_LINE_LARGE);
        lineHorizontalSmall.setTag(AppConstants.HORIZONTAL_LINE_SMALL);
        lineHorizontalMedium.setTag(AppConstants.HORIZONTAL_LINE_MEDIUM);
        lineHorizontalLarge.setTag(AppConstants.HORIZONTAL_LINE_LARGE);
        topRight.setTag(AppConstants.TOP_RIGHT);
        topLeft.setTag(AppConstants.TOP_LEFT);
        bottomRight.setTag(AppConstants.BOTTOM_RIGHT);
        bottomLeft.setTag(AppConstants.BOTTOM_LEFT);
    }

    @OnLongClick({R.id.ivVertical, R.id.ivHorizonal, R.id.line_vertical_small, R.id.line_vertical_medium, R.id.line_vertical_large,
            R.id.line_horizontal_small, R.id.line_horizontal_medium, R.id.line_horizontal_large, R.id.top_left, R.id.top_right,
            R.id.bottom_left, R.id.bottom_right})
    @Override
    public boolean onLongClick(View v) {
        viewStatus = AppConstants.NEW;
        ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
        String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
        ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
        View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
        v.startDrag(data, dragshadow, v, 0);
        return true;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        int action = event.getAction();
        String tag = ((View) event.getLocalState()).getTag().toString().replaceAll("\\d", "");
        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

            case DragEvent.ACTION_DRAG_ENTERED:
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                return true;

            case DragEvent.ACTION_DROP:
                if (tag.equalsIgnoreCase(AppConstants.VERTICAL_BLOCK) || tag.equalsIgnoreCase(AppConstants.HORIZONTAL_BLOCK)) {
                    if (v instanceof FrameLayout) {
                        setBoxView(v, event);
                    }
                } else {
                    if (v instanceof RelativeLayout)
                        setLineView((ViewGroup) v, event);
                }
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                return event.getResult();
        }
        return false;
    }

    /*
    Setting layout properties
     */
    private void setLayoutParamsView(View oldView, View newView, DragEvent event) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(oldView.getWidth(), oldView.getHeight());
        int x = (int) event.getX();
        int y = (int) event.getY();
        int posX = x - oldView.getWidth() / 2;
        int posY = y - oldView.getHeight() / 2;
        lp.setMargins(posX, posY, 0, 0);
        newView.setLayoutParams(lp);
    }

    private boolean isViewInsideForBox(View oldView, DragEvent event) {
        int viewMinX = (int) event.getX() - oldView.getWidth() / 2;
        int viewMinY = (int) event.getY() - oldView.getHeight() / 2;
        int viewMaxX = (int) event.getX() + oldView.getWidth() / 2;
        int viewMaxY = (int) event.getY() + oldView.getHeight() / 2;

        int containerMinX = 0;
        int containerMinY = 0;
        int containerMaxX = flMainDragContainer.getWidth();
        int containerMaxY = flMainDragContainer.getHeight();

        if (viewMinX < containerMinX || viewMinY < containerMinY || viewMaxX > containerMaxX || viewMaxY > containerMaxY) {
            Toast.makeText(this, "View outside of container", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean isViewInsideForLine(View oldView, DragEvent event) {
        int viewMinX = (int) event.getX() - oldView.getWidth() / 2;
        int viewMinY = (int) event.getY() - oldView.getHeight() / 2;
        int viewMaxX = (int) event.getX() + oldView.getWidth() / 2;
        int viewMaxY = (int) event.getY() + oldView.getHeight() / 2;


        if (viewMinX < 0 || viewMinY < 0) {
            return false;
        }

        if (boxList.size() == 0) {
            // let add the box
            return true;
        }
        for (int i = 0, boxListSize = boxList.size(); i < boxListSize; i++) {
            BoxModel model = boxList.get(i);
            int containerMaxX = model.getView().getWidth();
            int containerMaxY = model.getView().getHeight();

            if (viewMaxX <= containerMaxX && viewMaxY <= containerMaxY) {
                return true;
            }
        }
        return false;

    }

    private void findItemWithMinDistance(ImageView oldView, ImageView newView, DragEvent event) {

        int distance = 0, newX = 0, newY = 0, oldX = 0, oldY = 0;
        int tempDistance = 0;
        int minDistanceItemPos = 0;

        newX = (int) event.getX() - oldView.getWidth() / 2;
        newY = (int) event.getY() - oldView.getHeight() / 2;


        for (int i = 0; i < itemsModels.size(); i++) {

            switch (newView.getTag().toString().replaceAll("\\d", "")) {

                case AppConstants.VERTICAL_LINE_SMALL:
                    if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topRight.getWidth() - lineVerticalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + topRight.getHeight() - lineVerticalSmall.getWidth();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomRight.getHeight() - lineVerticalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalSmall.getHeight();
                        break;
                    }
                    break;

                case AppConstants.VERTICAL_LINE_MEDIUM:
                    if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topRight.getWidth() - lineVerticalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + topRight.getHeight() - lineVerticalMedium.getWidth();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomRight.getHeight() - lineVerticalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalMedium.getHeight();
                        break;
                    }
                    break;

                case AppConstants.VERTICAL_LINE_LARGE:
                    if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topRight.getWidth() - lineVerticalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + topRight.getHeight() - lineVerticalLarge.getWidth();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomRight.getHeight() - lineVerticalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalLarge.getHeight();
                        break;
                    }
                    break;

                case AppConstants.HORIZONTAL_LINE_SMALL:
                    if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalSmall.getWidth() - bottomRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomRight.getHeight() - lineHorizontalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    }
                    break;

                case AppConstants.HORIZONTAL_LINE_MEDIUM:
                    if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalMedium.getWidth() - bottomRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomRight.getHeight() - lineHorizontalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    }
                    break;

                case AppConstants.HORIZONTAL_LINE_LARGE:
                    if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalLarge.getWidth() - bottomRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomRight.getHeight() - lineHorizontalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    }
                    break;

                case AppConstants.TOP_RIGHT:
                    if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - topRight.getHeight();

                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - topRight.getHeight();

                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - topRight.getHeight();

                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - topRight.getHeight();
                        break;
                    } /*else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomRight.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - topRight.getHeight();
                        break;
                    }*/
                    break;

                case AppConstants.TOP_LEFT:
                    if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() - topLeft.getHeight();
                        break;
                    } /*else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + bottomRight.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getTop().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - topLeft.getHeight();
                        break;
                    }*/
                    break;

                case AppConstants.BOTTOM_RIGHT:
                    if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomRight.getHeight() + lineHorizontalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomRight.getHeight() + lineHorizontalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + lineHorizontalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomRight.getHeight() + lineHorizontalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - bottomRight.getWidth() + lineVerticalSmall.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - bottomRight.getWidth() + lineVerticalMedium.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - bottomRight.getWidth() + lineVerticalLarge.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + topRight.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + bottomLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();
                        break;
                    } /*else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - bottomRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getRight().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomRight.getHeight();
                        break;
                    }*/
                    break;

                case AppConstants.BOTTOM_LEFT:
                    if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomRight.getHeight() + lineHorizontalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomRight.getHeight() + lineHorizontalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - topLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomRight.getHeight() + lineHorizontalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalSmall.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalMedium.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + lineVerticalLarge.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX();
                        oldY = (int) itemsModels.get(i).getPosY() + topLeft.getHeight();
                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - bottomLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY();

                        break;
                    } /*else if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getBottom().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() + topRight.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() + topRight.getHeight();

                        break;
                    } else if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(i).getLeft().isStatus()) {
                        oldX = (int) itemsModels.get(i).getPosX() - bottomLeft.getWidth();
                        oldY = (int) itemsModels.get(i).getPosY() - bottomLeft.getHeight();
                        break;
                    }*/
                    break;
            }

            if (oldX != 0 && oldY != 0) {
                tempDistance = Math.abs(oldX - newX) + Math.abs(oldY - newY);

                if (distance != 0) {
                    if (distance > tempDistance) {
                        distance = tempDistance;
                        minDistanceItemPos = i;
                    }
                } else {
                    distance = tempDistance;
                    minDistanceItemPos = i;
                }
            }
        }

        if (oldX != 0 && oldY != 0) {
            addItemWithMinDistance(minDistanceItemPos, newView, oldView);
        } else {
            // send it to the top corner at initial position
            // no if consition captured.
            addItemToBox(oldView, newView, event);
        }
    }

    private void addItemWithMinDistance(int pos, ImageView newView, ImageView oldView) {
        currentView = oldView;

        switch (newView.getTag().toString().replaceAll("\\d", "")) {

            case AppConstants.VERTICAL_LINE_SMALL:
                if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topRight.getWidth() - lineVerticalSmall.getWidth(), itemsModels.get(pos).getPosY() + topRight.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + topLeft.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomRight.getHeight() - lineVerticalSmall.getWidth(), itemsModels.get(pos).getPosY() - lineVerticalSmall.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalSmall.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalSmall.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalSmall.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalSmall.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalMedium.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalSmall.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalLarge.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }
                break;

            case AppConstants.VERTICAL_LINE_MEDIUM:
                if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topRight.getWidth() - lineVerticalMedium.getWidth(), itemsModels.get(pos).getPosY() + topRight.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + topLeft.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomRight.getHeight() - lineVerticalMedium.getWidth(), itemsModels.get(pos).getPosY() - lineVerticalMedium.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalMedium.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalMedium.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalSmall.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalMedium.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalMedium.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalMedium.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalLarge.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }
                break;

            case AppConstants.VERTICAL_LINE_LARGE:
                if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topRight.getWidth() - lineVerticalLarge.getWidth(), itemsModels.get(pos).getPosY() + topRight.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + topLeft.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomRight.getHeight() - lineVerticalLarge.getWidth(), itemsModels.get(pos).getPosY() - lineVerticalLarge.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalLarge.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalLarge.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalSmall.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalLarge.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalMedium.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - lineVerticalLarge.getHeight(), newView, trueNode, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModels.get(pos).getBottom().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalLarge.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode, trueNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }
                break;

            case AppConstants.HORIZONTAL_LINE_SMALL:
                if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topLeft.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY() + bottomRight.getHeight() - lineHorizontalSmall.getHeight(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomLeft.getWidth(), itemsModels.get(pos).getPosY() + bottomLeft.getHeight() - lineHorizontalSmall.getHeight(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }
                break;

            case AppConstants.HORIZONTAL_LINE_MEDIUM:
                if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topLeft.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY() + bottomRight.getHeight() - lineHorizontalMedium.getHeight(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomLeft.getWidth(), itemsModels.get(pos).getPosY() + bottomLeft.getHeight() - lineHorizontalMedium.getHeight(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }
                break;

            case AppConstants.HORIZONTAL_LINE_LARGE:
                if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topLeft.getWidth(), itemsModels.get(pos).getPosY(),
                            newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY() + bottomRight.getHeight() - lineHorizontalLarge.getHeight(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomLeft.getWidth(), itemsModels.get(pos).getPosY() + bottomLeft.getHeight() - lineHorizontalLarge.getHeight(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(pos).getRight().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModels.get(pos).getLeft().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }
                break;

            case AppConstants.TOP_RIGHT:
                if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalSmall.getWidth(),
                            itemsModels.get(pos).getPosY(),
                            newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topRight.getWidth() + lineVerticalSmall.getWidth(), itemsModels.get(pos).getPosY() - topRight.getHeight(), newView, falseNode, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topRight.getWidth() + lineVerticalMedium.getWidth(), itemsModels.get(pos).getPosY() - topRight.getHeight(), newView, falseNode, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topRight.getWidth() + lineVerticalLarge.getWidth(),
                            itemsModels.get(pos).getPosY() - topRight.getHeight(),
                            newView, falseNode, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topLeft.getWidth(),
                            itemsModels.get(pos).getPosY(),
                            newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {

                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(),
                            itemsModels.get(pos).getPosY() - topRight.getHeight(),
                            newView, falseNode, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));
                    break;
                } /*else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(pos).getTop().isStatus()) {

                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomLeft.getWidth(),
                            itemsModels.get(pos).getPosY() + bottomRight.getHeight(),
                            newView, falseNode, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));
                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT) && itemsModels.get(pos).getRight().isStatus()) {

                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topRight.getWidth(),
                            itemsModels.get(pos).getPosY() - topRight.getHeight(),
                            newView, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));
                    break;
                }*/


                break;

            case AppConstants.TOP_LEFT:
                if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topLeft.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topLeft.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topLeft.getWidth(), itemsModels.get(pos).getPosY(), newView, falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - topLeft.getHeight(), newView, falseNode, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() - topLeft.getHeight(), newView, falseNode, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(),
                            itemsModels.get(pos).getPosY() - topLeft.getHeight(), newView,
                            falseNode, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topLeft.getWidth(),
                            itemsModels.get(pos).getPosY(), newView,
                            falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(),
                            itemsModels.get(pos).getPosY() - topLeft.getHeight(), newView,
                            falseNode, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }/* else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(pos).getLeft().isStatus()) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - topLeft.getWidth(),
                            itemsModels.get(pos).getPosY() + bottomRight.getHeight(), newView,
                            falseNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModels.get(pos).getTop().isStatus()) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomRight.getWidth(),
                            itemsModels.get(pos).getPosY() - topLeft.getHeight(), newView,
                            falseNode, falseNode, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()));
                    itemsModels.get(pos).setTop(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }*/

                break;

            case AppConstants.BOTTOM_RIGHT:
                if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalSmall.getWidth(), itemsModels.get(pos).getPosY() - bottomRight.getHeight() + lineHorizontalSmall.getHeight(), newView, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalMedium.getWidth(), itemsModels.get(pos).getPosY() - bottomRight.getHeight() + lineHorizontalMedium.getHeight(), newView, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + lineHorizontalLarge.getWidth(), itemsModels.get(pos).getPosY() - bottomRight.getHeight() + lineHorizontalLarge.getHeight(), newView, trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomRight.getWidth() + lineVerticalSmall.getWidth(), itemsModels.get(pos).getPosY() + lineVerticalSmall.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomRight.getWidth() + lineVerticalMedium.getWidth(), itemsModels.get(pos).getPosY() + lineVerticalMedium.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomRight.getWidth() + lineVerticalLarge.getWidth(), itemsModels.get(pos).getPosY() + lineVerticalLarge.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT)) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(),
                            itemsModels.get(pos).getPosY() + topRight.getHeight(), newView,
                            new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_LEFT)) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + bottomLeft.getWidth(),
                            itemsModels.get(pos).getPosY(), newView,
                            trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }/* else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(pos).getBottom().isStatus()) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomRight.getWidth(),
                            itemsModels.get(pos).getPosY() + topLeft.getHeight(), newView,
                            new NodeModel(false, pos, itemsModels.get(pos).getType()), trueNode, falseNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT) && itemsModels.get(pos).getRight().isStatus()) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topLeft.getWidth(),
                            itemsModels.get(pos).getPosY() - bottomRight.getHeight(), newView,
                            trueNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, falseNode);
                    itemsModels.get(pos).setRight(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                }*/
                break;

            case AppConstants.BOTTOM_LEFT:
                if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomLeft.getWidth(), itemsModels.get(pos).getPosY() - bottomLeft.getHeight() + lineHorizontalSmall.getHeight(), newView, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomLeft.getWidth(), itemsModels.get(pos).getPosY() - bottomLeft.getHeight() + lineHorizontalMedium.getHeight(), newView, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.HORIZONTAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomLeft.getWidth(), itemsModels.get(pos).getPosY() - bottomLeft.getHeight() + lineHorizontalLarge.getHeight(), newView, trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_SMALL)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalSmall.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_MEDIUM)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalMedium.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.VERTICAL_LINE_LARGE)) {
                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(), itemsModels.get(pos).getPosY() + lineVerticalLarge.getHeight(), newView, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_LEFT)) {

                    addViewToSideByOtherView(itemsModels.get(pos).getPosX(),
                            itemsModels.get(pos).getPosY() + topLeft.getHeight(), newView,
                            new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));

                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.BOTTOM_RIGHT)) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomLeft.getWidth(),
                            itemsModels.get(pos).getPosY(), newView,
                            trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));
                    break;
                } /*else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(pos).getBottom().isStatus()) {

                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() + topRight.getWidth(),
                            itemsModels.get(pos).getPosY() + topRight.getHeight(), newView,
                            new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode, trueNode, falseNode);
                    itemsModels.get(pos).setBottom(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));


                    break;
                } else if (itemsModels.get(pos).getType().equals(AppConstants.TOP_RIGHT) && itemsModels.get(pos).getLeft().isStatus()) {


                    addViewToSideByOtherView(itemsModels.get(pos).getPosX() - bottomLeft.getWidth(),
                            itemsModels.get(pos).getPosY() - bottomLeft.getHeight(), newView,
                            trueNode, falseNode, new NodeModel(false, pos, itemsModels.get(pos).getType()), falseNode);
                    itemsModels.get(pos).setLeft(new NodeModel(false, itemsModels.size() - 1, newView.getTag().toString() + count));


                    break;
                }*/
                break;
        }

    }

    private void addViewToSideByOtherView(float posX, float posY, ImageView newView, NodeModel top, NodeModel left, NodeModel right, NodeModel bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(currentView.getWidth(), currentView.getHeight());
        lp.setMargins((int) posX, (int) posY, 0, 0);
        newView.setLayoutParams(lp);
        addItemtoPojo2(newView, posX, posY, top, left, right, bottom);

    }

    private String getItemTag(ImageView newView) {
        return newView.getTag().toString().replaceAll("\\d", "");
    }

    private void addItemtoPojo2(ImageView imageView, float posX, float posy, NodeModel top, NodeModel left, NodeModel right, NodeModel bottom) {

        itemsModels.add(new ItemsModel(getItemTag(imageView) + count, imageView, top, left, right, bottom, posX, posy, getItemTag(imageView)));
        imageView.setTag(getItemTag(imageView) + count);
        checkIfLastItem();
        calculateDistance();
    }

    private int getLengthOfItem(String type) {
        switch (type) {
            case AppConstants.VERTICAL_LINE_SMALL:
                return 3;

            case AppConstants.VERTICAL_LINE_MEDIUM:
                return 4;

            case AppConstants.VERTICAL_LINE_LARGE:
                return 5;

            case AppConstants.HORIZONTAL_LINE_SMALL:
                return 3;

            case AppConstants.HORIZONTAL_LINE_MEDIUM:
                return 4;

            case AppConstants.HORIZONTAL_LINE_LARGE:
                return 5;

            default:
                return 0;
        }
    }

    private void calculateDistance() {
        if (isConnected) {
            //find node
            int startingPos = -1;
            boolean isNodeReached = false;
            int tempPos = -1, length = 2;
            for (int i = 0; i < itemsModels.size(); i++) {
                if (itemsModels.get(i).getType().equals(AppConstants.TOP_RIGHT) ||
                        itemsModels.get(i).getType().equals(AppConstants.TOP_LEFT) ||
                        itemsModels.get(i).getType().equals(AppConstants.BOTTOM_RIGHT) ||
                        itemsModels.get(i).getType().equals(AppConstants.BOTTOM_LEFT)) {

                    if (itemsModels.get(i).getTop().isStatus() ||
                            itemsModels.get(i).getLeft().isStatus() ||
                            itemsModels.get(i).getRight().isStatus() ||
                            itemsModels.get(i).getBottom().isStatus()) {
                        startingPos = i;
                        break;
                    }

                }
            }
            tempPos = startingPos;
            do {
                switch (itemsModels.get(tempPos).getType()) {
                    case AppConstants.TOP_RIGHT:
                        length = 2;

                        do {
                            tempPos = itemsModels.get(tempPos).getBottom().getAddress();
                            if (itemsModels.get(tempPos).getType().equals(AppConstants.TOP_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.TOP_LEFT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                                length = length + 2;
                                isNodeReached = true;
                            } else {
                                length = length + getLengthOfItem(itemsModels.get(tempPos).getType());
                            }

                        } while (!isNodeReached);
                        Toast.makeText(activity, "top_bottom :" + length, Toast.LENGTH_LONG).show();
                        sidesLengths.add(new SidesLength("sides_top_bottom", length));
                        break;
                    case AppConstants.TOP_LEFT:
                        length = 2;
                        do {
                            tempPos = itemsModels.get(tempPos).getRight().getAddress();
                            if (itemsModels.get(tempPos).getType().equals(AppConstants.TOP_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.TOP_LEFT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                                length = length + 2;
                                isNodeReached = true;
                            } else {
                                length = length + getLengthOfItem(itemsModels.get(tempPos).getType());
                            }

                        } while (!isNodeReached);

                        Toast.makeText(activity, "left_right :" + length, Toast.LENGTH_LONG).show();
                        sidesLengths.add(new SidesLength("sides_left_right", length));
                        break;
                    case AppConstants.BOTTOM_RIGHT:
                        length = 2;
                        do {
                            tempPos = itemsModels.get(tempPos).getLeft().getAddress();
                            if (itemsModels.get(tempPos).getType().equals(AppConstants.TOP_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.TOP_LEFT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                                length = length + 2;
                                isNodeReached = true;
                            } else {
                                length = length + getLengthOfItem(itemsModels.get(tempPos).getType());
                            }

                        } while (!isNodeReached);

                        Toast.makeText(activity, "right_left :" + length, Toast.LENGTH_LONG).show();
                        sidesLengths.add(new SidesLength("sides_right_left", length));
                        break;
                    case AppConstants.BOTTOM_LEFT:
                        length = 2;

                        do {
                            tempPos = itemsModels.get(tempPos).getTop().getAddress();
                            if (itemsModels.get(tempPos).getType().equals(AppConstants.TOP_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.TOP_LEFT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_RIGHT) ||
                                    itemsModels.get(tempPos).getType().equals(AppConstants.BOTTOM_LEFT)) {
                                length = length + 2;
                                isNodeReached = true;
                            } else {
                                length = length + getLengthOfItem(itemsModels.get(tempPos).getType());
                            }

                        } while (!isNodeReached);

                        Toast.makeText(activity, "bottom_top :" + length, Toast.LENGTH_LONG).show();
                        sidesLengths.add(new SidesLength("sides_bottom_top", length));
                        break;
                }
                isNodeReached = false;
            } while (startingPos != tempPos);

        } else {
            itemWithEmptyCorner();
            Log.e("Empty Corner Size", tempItemPos.size() + " ");
        }
    }

    public int posX(ItemsModel itemsModel) {

        if (itemsModel.getType().equals(AppConstants.TOP_RIGHT) && itemsModel.getBottom().isStatus()) {
            oldX = (int) itemsModel.getPosX() + topRight.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.TOP_RIGHT) && itemsModel.getLeft().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.TOP_LEFT) && itemsModel.getBottom().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.TOP_LEFT) && itemsModel.getRight().isStatus()) {
            oldX = (int) itemsModel.getPosX() + topLeft.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModel.getTop().isStatus()) {
            oldX = (int) itemsModel.getPosX() + bottomRight.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModel.getLeft().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_LEFT) && itemsModel.getTop().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_LEFT) && itemsModel.getRight().isStatus()) {
            oldX = (int) itemsModel.getPosX() + bottomLeft.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModel.getTop().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModel.getBottom().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModel.getTop().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModel.getBottom().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModel.getTop().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModel.getBottom().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModel.getRight().isStatus()) {
            oldX = (int) itemsModel.getPosX() + lineHorizontalSmall.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModel.getLeft().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModel.getRight().isStatus()) {
            oldX = (int) itemsModel.getPosX() + lineHorizontalMedium.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModel.getLeft().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModel.getRight().isStatus()) {
            oldX = (int) itemsModel.getPosX() + lineHorizontalLarge.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModel.getLeft().isStatus()) {
            oldX = (int) itemsModel.getPosX();

        }

        return oldX;
    }

    public int posY(ItemsModel itemsModel) {

        if (itemsModel.getType().equals(AppConstants.TOP_RIGHT) && itemsModel.getBottom().isStatus()) {
            oldY = (int) itemsModel.getPosY() + topRight.getWidth();

        } else if (itemsModel.getType().equals(AppConstants.TOP_RIGHT) && itemsModel.getLeft().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.TOP_LEFT) && itemsModel.getBottom().isStatus()) {
            oldY = (int) itemsModel.getPosY() + topLeft.getHeight();

        } else if (itemsModel.getType().equals(AppConstants.TOP_LEFT) && itemsModel.getRight().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModel.getTop().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_RIGHT) && itemsModel.getLeft().isStatus()) {
            oldY = (int) itemsModel.getPosY() + bottomRight.getHeight();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_LEFT) && itemsModel.getTop().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.BOTTOM_LEFT) && itemsModel.getRight().isStatus()) {
            oldY = (int) itemsModel.getPosY() + bottomLeft.getHeight();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModel.getTop().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_SMALL) && itemsModel.getBottom().isStatus()) {
            oldY = (int) itemsModel.getPosY() + lineVerticalSmall.getHeight();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModel.getTop().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_MEDIUM) && itemsModel.getBottom().isStatus()) {
            oldY = (int) itemsModel.getPosY() + lineVerticalMedium.getHeight();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModel.getTop().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.VERTICAL_LINE_LARGE) && itemsModel.getBottom().isStatus()) {
            oldY = (int) itemsModel.getPosY() + lineVerticalLarge.getHeight();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModel.getRight().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_SMALL) && itemsModel.getLeft().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModel.getRight().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_MEDIUM) && itemsModel.getLeft().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModel.getRight().isStatus()) {
            oldY = (int) itemsModel.getPosY();

        } else if (itemsModel.getType().equals(AppConstants.HORIZONTAL_LINE_LARGE) && itemsModel.getLeft().isStatus()) {
            oldY = (int) itemsModel.getPosY();
        }
        return oldY;
    }

    public void itemWithEmptyCorner() {
        tempItemPos.clear();
        for (int i = 0; i < itemsModels.size(); i++) {
            if (itemsModels.get(i).getTop().isStatus() || itemsModels.get(i).getBottom().isStatus() || itemsModels.get(i).getLeft().isStatus() || itemsModels.get(i).getRight().isStatus()) {
                Log.e("Temp: " + i, posX(itemsModels.get(i)) + " / " + posY(itemsModels.get(i)));
                tempItemPos.add(i);
            }
        }
    }

    private void checkIfLastItem() {

        itemWithEmptyCorner();

        if (tempItemPos.size() == 2) {
            if (Math.abs(posX(itemsModels.get(tempItemPos.get(0))) - posX(itemsModels.get(tempItemPos.get(1)))) < 20 &&
                    Math.abs(posY(itemsModels.get(tempItemPos.get(0))) - posY(itemsModels.get(tempItemPos.get(1)))) < 20) {
                isConnected = true;
                if (itemsModels.get(tempItemPos.get(0)).getTop().isStatus()) {
                    itemsModels.get(tempItemPos.get(0)).setTop(new NodeModel(false, tempItemPos.get(1), itemsModels.get(tempItemPos.get(1)).getItemId()));

                } else if (itemsModels.get(tempItemPos.get(0)).getBottom().isStatus()) {
                    itemsModels.get(tempItemPos.get(0)).setBottom(new NodeModel(false, tempItemPos.get(1), itemsModels.get(tempItemPos.get(1)).getItemId()));

                } else if (itemsModels.get(tempItemPos.get(0)).getLeft().isStatus()) {
                    itemsModels.get(tempItemPos.get(0)).setLeft(new NodeModel(false, tempItemPos.get(1), itemsModels.get(tempItemPos.get(1)).getItemId()));

                } else if (itemsModels.get(tempItemPos.get(0)).getRight().isStatus()) {
                    itemsModels.get(tempItemPos.get(0)).setRight(new NodeModel(false, tempItemPos.get(1), itemsModels.get(tempItemPos.get(1)).getItemId()));
                }

                if (itemsModels.get(tempItemPos.get(1)).getTop().isStatus()) {
                    itemsModels.get(tempItemPos.get(1)).setTop(new NodeModel(false, tempItemPos.get(0), itemsModels.get(tempItemPos.get(0)).getItemId()));

                } else if (itemsModels.get(tempItemPos.get(1)).getBottom().isStatus()) {
                    itemsModels.get(tempItemPos.get(1)).setBottom(new NodeModel(false, tempItemPos.get(0), itemsModels.get(tempItemPos.get(0)).getItemId()));

                } else if (itemsModels.get(tempItemPos.get(1)).getLeft().isStatus()) {
                    itemsModels.get(tempItemPos.get(1)).setLeft(new NodeModel(false, tempItemPos.get(0), itemsModels.get(tempItemPos.get(0)).getItemId()));

                } else if (itemsModels.get(tempItemPos.get(1)).getRight().isStatus()) {
                    itemsModels.get(tempItemPos.get(1)).setRight(new NodeModel(false, tempItemPos.get(0), itemsModels.get(tempItemPos.get(0)).getItemId()));

                }
            } else {
                isConnected = false;
            }
        } else {
            isConnected = false;
        }
        tempItemPos.clear();

    }


    public void deleteItem(View v, View container) {

        for (int i = itemsModels.size() - 1; i >= 0; i--) {
            if (itemsModels.get(i).getItemId().contains(v.getTag().toString())) {
                ItemsModel itemsModel = itemsModels.get(i);
                if (itemsModel.getTop().getAddress() != -1 && itemsModel.getTop().getAddress() != -2) {
                    int top = itemsModel.getTop().getAddress();
                    itemsModels.get(top).setBottom(trueNode);
                }

                if (itemsModel.getLeft().getAddress() != -1 && itemsModel.getLeft().getAddress() != -2) {
                    int left = itemsModel.getLeft().getAddress();
                    itemsModels.get(left).setRight(trueNode);
                }

                if (itemsModel.getRight().getAddress() != -1 && itemsModel.getRight().getAddress() != -2) {
                    int right = itemsModel.getRight().getAddress();
                    itemsModels.get(right).setLeft(trueNode);
                }

                if (itemsModel.getBottom().getAddress() != -1 && itemsModel.getBottom().getAddress() != -2) {
                    int bottom = itemsModel.getBottom().getAddress();
                    itemsModels.get(bottom).setTop(trueNode);
                }
                itemsModels.remove(i);
                ((ViewGroup) container).removeView(v);
                itemsModel.setTop(falseNode);
                itemsModel.setLeft(falseNode);
                itemsModel.setRight(falseNode);
                itemsModel.setBottom(falseNode);
                break;
            }
        }
    }

    public static Point getTouchPositionFromDragEvent(View item, DragEvent event) {
        Rect rItem = new Rect();
        item.getGlobalVisibleRect(rItem);
        return new Point(rItem.left + Math.round(event.getX()), rItem.top + Math.round(event.getY()));

    }

}
