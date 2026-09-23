package com.ajmst.android.service;

import android.annotation.SuppressLint;
import android.content.Context;
import com.ajmst.android.entity.AdvSpkfk;
import com.ajmst.commmon.entity.AjmstGh;
import com.ajmst.commmon.entity.Spkfk;
import com.ajmst.commmon.util.BeanUtils;
import com.ajmst.commmon.util.StringUtils;
import com.ajmst.common.response.Response;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class SpkfkService extends BaseService<AdvSpkfk> {
    private static final int MAX_SELECT_ROW_NUM = 1000;
    public static final String SELF_CN_SP_SPBH_PRE = "8";
    public static final int SELF_CN_SP_UNIT_QUANTITY = 10;

    public SpkfkService(Context context) {
        super(context);
    }

    public AdvSpkfk toAdvSpkfk(Spkfk sp) throws Exception {
        AdvSpkfk advSp = new AdvSpkfk();
        BeanUtils.fatherToChild(sp, advSp);
        return advSp;
    }

    public List<AdvSpkfk> toAdvSpkfk(List<Spkfk> sps) throws Exception {
        List<AdvSpkfk> advSps = new ArrayList<>();
        for (Spkfk sp : sps) {
            advSps.add(toAdvSpkfk(sp));
        }
        return advSps;
    }

    @Override // com.ajmst.android.service.BaseService
    public Response saveOrUpdate(AdvSpkfk sp) {
        Response r = new Response();
        try {
            AdvSpkfk spExist = getById(sp.getSpid());
            if (spExist != null) {
                getDao().update(sp);
            } else {
                getDao().create(sp);
            }
        } catch (SQLException e) {
            r.setIsOk(false);
            r.setException(e);
        }
        return r;
    }

    public AdvSpkfk getByBarcode(String barcode) {
        if (barcode != null) {
            barcode = barcode.trim();
        }
        try {
            AdvSpkfk sp = (AdvSpkfk) getDao().queryBuilder().where().raw("trim(sptm)='" + barcode + "'", new ArgumentHolder[0]).queryForFirst();
            return sp;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<AdvSpkfk> getSelfCnSp() {
        List<AdvSpkfk> sps = new ArrayList<>();
        try {
            List<AdvSpkfk> sps2 = getDao().queryBuilder().orderBy("spbh", true).where().like("spbh", "8%").query();
            return sps2;
        } catch (SQLException e) {
            e.printStackTrace();
            return sps;
        }
    }

    public Response updateCabinet(List<AjmstGh> ghs) {
        Response r = new Response();
        for (AjmstGh gh : ghs) {
            r = updateCabinet(gh);
            if (!r.isOk()) {
                break;
            }
        }
        return r;
    }

    public Response updateCabinet(AjmstGh gh) {
        AdvSpkfk sp;
        Response r = new Response();
        String spid = gh.getSpid();
        String spbh = gh.getSpbh();
        String cabinet = gh.getGh();
        if (spid != null && !"".equals(spid.trim())) {
            AdvSpkfk sp2 = getById(spid);
            sp = sp2;
        } else {
            sp = getBySpbh(spbh);
        }
        if (sp != null) {
            sp.setGh(cabinet);
            try {
                getDao().update(sp);
            } catch (SQLException e) {
                r.setException(e);
            }
        } else {
            r.setException(new Exception("No such spkfk,spid:" + spid + ",spbh:" + spbh));
        }
        return r;
    }

    public Response updateSptm(AdvSpkfk sp, String sptm) {
        Response r = new Response();
        if (sp != null) {
            sp.setSptm(sptm);
            sp.setIsBarcodeUpdate(1);
            try {
                getDao().update(sp);
            } catch (SQLException e) {
                r.setException(e);
            }
        } else {
            r.setException(new Exception("No such spkfk,spid:" + sp.getSpid() + ",spbh:" + sp.getSpbh()));
        }
        return r;
    }

    public AdvSpkfk getBySpbh(String spbh) {
        if (spbh != null) {
            spbh = spbh.trim();
        }
        try {
            AdvSpkfk sp = (AdvSpkfk) getDao().queryBuilder().where().raw("trim(spbh)='" + spbh + "'", new ArgumentHolder[0]).queryForFirst();
            return sp;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override // com.ajmst.android.service.BaseService
    public String toString(AdvSpkfk sp) {
        StringBuilder sb = new StringBuilder();
        sb.append(sp.getSpbh().trim()).append("\n").append(sp.getZjm()).append("\n").append(sp.getSpmch().trim()).append("\n").append("规格:").append(sp.getShpgg()).append(", 单位:").append(sp.getDw()).append("\n").append(sp.getShengccj()).append("\n").append("售价:").append(sp.getLshj());
        return sb.toString();
    }

    @SuppressLint({"DefaultLocale"})
    public List<AdvSpkfk> query(String zjm, String cabinet, BigDecimal priceFrom, BigDecimal priceEnd, Boolean isSelfCnSp) {
        List<AdvSpkfk> sps = new ArrayList<>();
        try {
            QueryBuilder queryBuilder = getDao().queryBuilder();
            Where where = queryBuilder.where().isNotNull("spid");
            if (zjm != null && !"".equals(zjm)) {
                where.and().like("zjm", "%" + zjm.toUpperCase() + "%");
            }
            if (cabinet != null) {
                if ("NULL".equalsIgnoreCase(cabinet)) {
                    where.and().isNull("gh");
                } else {
                    String cabinet2 = cabinet.trim();
                    if (StringUtils.isInt(cabinet2) && Integer.valueOf(cabinet2).intValue() < 10) {
                        cabinet2 = "0" + cabinet2;
                    }
                    where = where.and().raw("trim(gh)='" + cabinet2 + "'", new ArgumentHolder[0]);
                }
            }
            if (priceFrom != null) {
                where.and().raw("CAST(lshj as double) >=" + priceFrom, new ArgumentHolder[0]);
            }
            if (priceEnd != null) {
                where.and().raw("CAST(lshj as double) <=" + priceEnd, new ArgumentHolder[0]);
            }
            if (isSelfCnSp != null) {
                if (isSelfCnSp.booleanValue()) {
                    where.and().raw("trim(spbh) like'8%'", new ArgumentHolder[0]);
                } else {
                    where.and().raw("trim(spbh) not like'8%'", new ArgumentHolder[0]);
                }
            }
            String sql = where.getStatement();
            System.out.println(sql);
            sps = queryBuilder.orderBy("gh", false).limit(MAX_SELECT_ROW_NUM).query();
            return sps;
        } catch (Exception e) {
            e.printStackTrace();
            return sps;
        }
    }

    public static boolean isSelfCnSp(String spbh) {
        if (!spbh.startsWith(SELF_CN_SP_SPBH_PRE)) {
            return false;
        }
        return true;
    }

    public static boolean isSelfCnSp(AdvSpkfk sp) {
        return isSelfCnSp(sp.getSpbh());
    }
}
