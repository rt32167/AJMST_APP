package com.ajmst.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.ajmst.android.R;

/** A light gradient header whose lower edge closes in a shallow arc. */
public class HeroHeaderLayout extends LinearLayout {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final float edgeRise;
    private LinearGradient colorGradient;

    public HeroHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        edgeRise = 10f * getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width == 0 || height == 0) {
            return;
        }
        shape.reset();
        shape.moveTo(0f, 0f);
        shape.lineTo(width, 0f);
        shape.lineTo(width, height - edgeRise);
        shape.quadTo(width / 2f, height + edgeRise, 0f, height - edgeRise);
        shape.close();

        colorGradient = new LinearGradient(0f, 0f, width, height,
                new int[] {
                        getContext().getColor(R.color.ui_hero_end),
                        getContext().getColor(R.color.ui_arc_middle),
                        getContext().getColor(R.color.ui_arc_end)
                }, new float[] {0f, 0.48f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (colorGradient == null) {
            return;
        }
        paint.setShader(colorGradient);
        canvas.drawPath(shape, paint);
        paint.setShader(null);
    }
}
