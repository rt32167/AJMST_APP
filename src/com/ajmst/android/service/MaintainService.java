package com.ajmst.android.service;

import android.content.Context;
import com.ajmst.android.util.DateTimeUtils;
import com.ajmst.android.util.ExcelUtils;
import com.ajmst.android.util.StringUtils;
import com.ajmst.commmon.entity.AjmstMaintain;
import com.ajmst.common.response.Response;
import com.j256.ormlite.stmt.QueryBuilder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jxl.Workbook;

/* JADX INFO: loaded from: classes.dex */
public class MaintainService extends BaseService<AjmstMaintain> {
    private static final int DEFAULT_SHEET_INDEX = 0;

    public MaintainService(Context context) {
        super(context);
    }

    @Override // com.ajmst.android.service.BaseService
    public Response saveOrUpdate(AjmstMaintain obj) {
        Response r = new Response();
        try {
            getDao().createOrUpdate(obj);
        } catch (SQLException e) {
            r.setIsOk(false);
            r.setException(e);
        }
        return r;
    }

    public List<AjmstMaintain> getMaintainItems() {
        return getMaintainItemsByGH("全部");
    }

    public List<AjmstMaintain> getMaintainItemsByGH(String gh) {
        List<AjmstMaintain> maintainItems = new ArrayList<>();
        try {
            String gh2 = gh.replace("柜", "").trim();
            QueryBuilder queryBuilder = getDao().queryBuilder();
            queryBuilder.orderBy("cabinetNo", true).orderBy("spbh", true);
            if (!gh2.equals("全部")) {
                if (gh2.equals("未完成")) {
                    queryBuilder.where().isNull("shl");
                } else {
                    queryBuilder.where().eq("cabinetNo", gh2);
                }
            }
            maintainItems = queryBuilder.query();
            return maintainItems;
        } catch (SQLException e) {
            e.printStackTrace();
            return maintainItems;
        }
    }

    public boolean create(AjmstMaintain maintainItem) {
        Response r = new Response();
        try {
            getDao().create(maintainItem);
        } catch (SQLException e) {
            r.setException(e);
            r.setIsOk(false);
            r.setResult(maintainItem);
            e.printStackTrace();
        }
        return r.isOk();
    }

    public boolean initData(String path) {
        boolean result = false;
        Workbook wb = null;
        try {
            try {
                InputStream is = new FileInputStream(path);
                wb = Workbook.getWorkbook(is);
                List<List<String>> data = ExcelUtils.getData(wb, 0);
                clearData();
                for (int r = 1; r < data.size(); r++) {
                    List<String> rowData = data.get(r);
                    AjmstMaintain maintainItem = new AjmstMaintain();
                    String spID = rowData.get(0);
                    if (spID == null || "".equals(spID)) {
                        break;
                    }
                    String desc = rowData.get(1);
                    String batchcode = rowData.get(2);
                    Double suggestQuantity = Double.valueOf(StringUtils.stringtodouble(rowData.get(3)));
                    Double quantity = null;
                    if (rowData.get(4) != null && !"".equals(rowData.get(4).trim())) {
                        quantity = Double.valueOf(StringUtils.stringtodouble(rowData.get(4)));
                    }
                    String cabinetNo = rowData.get(5);
                    String factory = rowData.get(6);
                    String specification = rowData.get(7);
                    String unit = rowData.get(8);
                    String spid = rowData.get(13);
                    Date maintainDate = DateTimeUtils.parseDate(rowData.get(14), "yyyy-MM-dd");
                    String zjm = rowData.get(15);
                    if (spID != null && !"".equals(spID)) {
                        String id = String.valueOf(spid) + batchcode + DateTimeUtils.formatDate(maintainDate, "yyyyMMdd");
                        maintainItem.setId(id);
                        maintainItem.setSpid(spid);
                        maintainItem.setPihao(batchcode);
                        maintainItem.setMaintainDate(maintainDate);
                        maintainItem.setSpbh(spID);
                        maintainItem.setSpmch(desc);
                        maintainItem.setShengccj(factory);
                        maintainItem.setShpgg(specification);
                        maintainItem.setDw(unit);
                        maintainItem.setCabinetNo(cabinetNo);
                        maintainItem.setSuggestQuantity(suggestQuantity);
                        maintainItem.setShl(quantity);
                        maintainItem.setZjm(zjm);
                        maintainItem.setCreateTime(new Date());
                        create(maintainItem);
                    }
                }
                result = true;
                if (wb != null) {
                    try {
                        wb.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                if (wb != null) {
                    try {
                        wb.close();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
            }
            return result;
        } catch (Throwable th) {
            if (wb != null) {
                try {
                    wb.close();
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
            }
            throw th;
        }
    }

    public int clearData() {
        return deleteAll();
    }

    public List<AjmstMaintain> getNoQuantityItems() {
        return getMaintainItemsByGH("未完成");
    }
}
