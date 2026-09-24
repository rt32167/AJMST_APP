package com.ajmst.android.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import com.ajmst.android.R;

/** Rounds the page inside the system-bar insets without reducing its usable area. */
public class RoundedPageLayout extends FrameLayout {
    public RoundedPageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.BLACK);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final float radius = getResources().getDimension(R.dimen.ui_page_corner_radius);
        View page = getChildAt(0);
        page.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        page.setClipToOutline(true);
    }
}
