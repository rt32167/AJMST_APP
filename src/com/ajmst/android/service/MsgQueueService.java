package com.ajmst.android.service;

import android.content.Context;
import android.util.Log;
import com.ajmst.android.entity.MsgQueue;
import com.ajmst.android.webservice.WsRequest;
import com.ajmst.android.webservice.WsResponse;
import com.ajmst.common.constants.IWebServiceName;
import com.ajmst.common.exception.ExceptionUtil;
import com.ajmst.common.response.Response;
import com.ajmst.common.xml.XmlUtils;
import com.j256.ormlite.stmt.ArgumentHolder;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class MsgQueueService extends BaseService<MsgQueue> {
    private static final int MAX_SEND_NUM_PER_TIME = 10;
    String TAG;

    public MsgQueueService(Context context) {
        super(context);
        this.TAG = MsgQueueService.class.getSimpleName();
    }

    @Override // com.ajmst.android.service.BaseService
    public Response saveOrUpdate(MsgQueue msg) {
        Response r = new Response();
        MsgQueue msgExist = null;
        try {
            if (msg.getId() != null) {
                msgExist = (MsgQueue) getDao().queryForId(msg.getId());
            }
            if (msgExist != null) {
                getDao().update(msg);
            } else {
                getDao().create(msg);
            }
        } catch (SQLException e) {
            r.setIsOk(false);
            r.setException(e);
        }
        return r;
    }

    public List<MsgQueue> getSendableMsgs() {
        List<MsgQueue> msgs = new ArrayList<>();
        try {
            List<MsgQueue> msgs2 = getDao().queryBuilder().orderBy("createTime", true).where().raw("state in(1,4,2)", new ArgumentHolder[0]).query();
            return msgs2;
        } catch (SQLException e) {
            e.printStackTrace();
            return msgs;
        }
    }

    public void sendMsgs() {
        Set<String> failTypes = new HashSet<>();
        int sendCount = 0;
        List<MsgQueue> msgs = getSendableMsgs();
        for (MsgQueue msg : msgs) {
            String serviceName = msg.getServiceName();
            if (!failTypes.contains(serviceName)) {
                if (sendCount <= 10) {
                    sendCount++;
                    msg.setState(2);
                    msg.setStartSendTime(new Date());
                    saveOrUpdate(msg);
                    String xml = msg.getData();
                    Log.i(this.TAG, "开始发送消息,service name:" + serviceName + ",data:\n" + xml);
                    WsResponse r = WsRequest.call(serviceName, xml);
                    if (r.isOk()) {
                        Log.i(this.TAG, "发送成功");
                        msg.setState(3);
                    } else {
                        String failReason = ExceptionUtil.getStackTrace(r.getException());
                        failTypes.add(serviceName);
                        msg.setFailCount(msg.getFailCount() + 1);
                        msg.setState(4);
                        msg.setLastFailReason(failReason);
                        Log.i(this.TAG, "发送失败,累计失败次数:" + msg.getFailCount() + ",原因:" + msg.getLastFailReason());
                    }
                    msg.setFinishSendTime(new Date());
                    saveOrUpdate(msg);
                } else {
                    return;
                }
            }
        }
    }

    public Response createMsg_Lshj(String spid, BigDecimal lshj) {
        MsgQueue msg = new MsgQueue();
        msg.setServiceName(IWebServiceName.SERVICE_SPKFK_PRICE);
        msg.setCreateTime(new Date());
        msg.setState(1);
        Hashtable data = new Hashtable();
        data.put("Spid", spid);
        data.put("Lshj", lshj);
        msg.setData(XmlUtils.getXmlStr(data));
        return saveOrUpdate(msg);
    }

    public Response createMsg_Sptm(String spid, String sptm) {
        MsgQueue msg = new MsgQueue();
        msg.setServiceName(IWebServiceName.SERVICE_SPKFK_BARCODE);
        msg.setCreateTime(new Date());
        msg.setState(1);
        Hashtable data = new Hashtable();
        data.put("Spid", spid);
        data.put("Sptm", sptm);
        msg.setData(XmlUtils.getXmlStr(data));
        return saveOrUpdate(msg);
    }
}
