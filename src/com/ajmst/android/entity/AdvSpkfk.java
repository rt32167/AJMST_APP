package com.ajmst.android.entity;

import com.ajmst.commmon.entity.Spkfk;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/* JADX INFO: loaded from: classes.dex */
@DatabaseTable
public class AdvSpkfk extends Spkfk {
    private static final long serialVersionUID = -3926164119706133607L;

    @DatabaseField
    private String gh;

    @DatabaseField
    private Integer isBarcodeUpdate;

    @DatabaseField
    private Integer maxQuantity;

    public String getGh() {
        return this.gh;
    }

    public void setGh(String gh) {
        this.gh = gh;
    }

    public Integer getMaxQuantity() {
        return this.maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public Integer getIsBarcodeUpdate() {
        return this.isBarcodeUpdate;
    }

    public void setIsBarcodeUpdate(Integer isBarcodeUpdate) {
        this.isBarcodeUpdate = isBarcodeUpdate;
    }
}
