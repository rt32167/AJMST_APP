package com.ajmst.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.ajmst.android.R;

/** Shared subtotal styling for the aligned product and order columns. */
public final class ItemRowText {
    private ItemRowText() {
    }

    public static CharSequence subtotal(Context context, String amount) {
        SpannableStringBuilder line = new SpannableStringBuilder("小计 ¥" + amount);
        line.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.ui_muted)),
                0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        line.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.ui_accent_dark)),
                3, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        line.setSpan(new RelativeSizeSpan(1.64f), 3, line.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        line.setSpan(new StyleSpan(Typeface.BOLD), 3, line.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return line;
    }
}
