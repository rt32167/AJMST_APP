package com.ajmst.android.salesorder;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.ListView;

import com.ajmst.android.R;

/** Handles row reveal while keeping the return gesture at the left screen edge. */
public class OrderSwipeListView extends ListView {
    private final int touchSlop;
    private final float revealWidth;
    private final float returnEdge;
    private final float returnDistance;
    private float downX;
    private float downY;
    private float startingTranslation;
    private int downPosition = INVALID_POSITION;
    private View activeRow;
    private View activeForeground;
    private boolean horizontalGesture;
    private boolean returning;
    private boolean needsCancel;

    public OrderSwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        revealWidth = 56f * density;
        returnEdge = 40f * density;
        returnDistance = 72f * density;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                beginTouch(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (startHorizontalGesture(event)) {
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !horizontalGesture) {
            startHorizontalGesture(event);
        }
        if (needsCancel) {
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.onTouchEvent(cancel);
            cancel.recycle();
            needsCancel = false;
        }
        if (!horizontalGesture) {
            return super.onTouchEvent(event);
        }
        float dx = event.getX() - downX;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                if (!returning && activeForeground != null) {
                    activeForeground.setTranslationX(
                            Math.max(-revealWidth, Math.min(0f, startingTranslation + dx)));
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean completed = event.getActionMasked() == MotionEvent.ACTION_UP;
                if (returning) {
                    if (completed && dx >= returnDistance
                            && Math.abs(dx) > Math.abs(event.getY() - downY)) {
                        ((Activity) getContext()).finish();
                    }
                } else if (activeForeground != null) {
                    if (completed) {
                        activeForeground.setTranslationX(
                                Math.max(-revealWidth, Math.min(0f, startingTranslation + dx)));
                    }
                    boolean open = completed && activeForeground.getTranslationX() < -revealWidth / 2f;
                    settleRow(open);
                }
                horizontalGesture = false;
                returning = false;
                return true;
            default:
                return true;
        }
    }

    private void beginTouch(MotionEvent event) {
        downX = event.getX();
        downY = event.getY();
        downPosition = pointToPosition((int) downX, (int) downY);
        activeRow = getVisibleRow(downPosition);
        activeForeground = activeRow == null ? null : activeRow.findViewById(R.id.orderRowForeground);
        startingTranslation = activeForeground == null ? 0f : activeForeground.getTranslationX();
        horizontalGesture = false;
        returning = false;
        needsCancel = false;
    }

    private boolean startHorizontalGesture(MotionEvent event) {
        if (horizontalGesture) {
            return true;
        }
        float dx = event.getX() - downX;
        float dy = event.getY() - downY;
        if (Math.abs(dx) <= touchSlop || Math.abs(dx) <= Math.abs(dy) * 1.2f) {
            return false;
        }
        if (downX <= returnEdge && dx > 0f) {
            returning = true;
            horizontalGesture = true;
        } else if (activeForeground != null && (dx < 0f || startingTranslation < 0f)) {
            horizontalGesture = true;
            activeForeground.animate().cancel();
            Button deleteButton = (Button) activeRow.findViewById(R.id.btnDeleteItem);
            deleteButton.setVisibility(VISIBLE);
            closeOtherRow();
        }
        if (horizontalGesture) {
            needsCancel = true;
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return horizontalGesture;
    }

    private View getVisibleRow(int position) {
        int childIndex = position - getFirstVisiblePosition();
        return childIndex < 0 || childIndex >= getChildCount() ? null : getChildAt(childIndex);
    }

    private void closeOtherRow() {
        OrderItemListAdaper adapter = (OrderItemListAdaper) getAdapter();
        if (adapter == null || adapter.getOpenPosition() == downPosition) {
            return;
        }
        closeOpenRow();
    }

    public void closeOpenRow() {
        OrderItemListAdaper adapter = (OrderItemListAdaper) getAdapter();
        if (adapter == null || adapter.getOpenPosition() < 0) {
            return;
        }
        View oldRow = getVisibleRow(adapter.getOpenPosition());
        if (oldRow != null) {
            animateRow(oldRow, 0f);
        }
        adapter.setOpenPosition(-1);
    }

    private void settleRow(boolean open) {
        OrderItemListAdaper adapter = (OrderItemListAdaper) getAdapter();
        if (adapter != null) {
            adapter.setOpenPosition(open ? downPosition : -1);
        }
        animateRow(activeRow, open ? -revealWidth : 0f);
    }

    private void animateRow(View row, final float destination) {
        final View foreground = row.findViewById(R.id.orderRowForeground);
        final Button deleteButton = (Button) row.findViewById(R.id.btnDeleteItem);
        foreground.animate().cancel();
        if (destination < 0f) {
            deleteButton.setVisibility(VISIBLE);
        }
        foreground.animate().translationX(destination).setDuration(150).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (destination == 0f) {
                    deleteButton.setVisibility(INVISIBLE);
                }
            }
        }).start();
    }
}
