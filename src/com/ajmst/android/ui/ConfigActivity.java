package com.ajmst.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.ajmst.android.R;
import com.ajmst.android.entity.AdvSpkfk;
import com.ajmst.android.service.MaintainService;
import com.ajmst.android.service.SpkfkService;
import com.ajmst.android.util.DateTimeUtils;
import com.ajmst.android.webservice.WsGhService;
import com.ajmst.android.webservice.WsMaintainService;
import com.ajmst.android.webservice.WsResponse;
import com.ajmst.android.webservice.WsSpkfkService;
import com.ajmst.commmon.entity.AjmstGh;
import com.ajmst.commmon.entity.AjmstMaintain;
import com.ajmst.commmon.entity.Spkfk;
import com.ajmst.common.response.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/* JADX INFO: loaded from: classes.dex */
@SuppressLint({"HandlerLeak"})
public class ConfigActivity extends Activity {
    private static final int GET_AJMST_GH_RETRY_GAP_SECONDS = 10;
    private static final int GET_SPKFK_RETRY_GAP_SECONDS = 10;
    private static final String LOG_TAG = ConfigActivity.class.getSimpleName();
    private static final int MAX_COUNT_GET_AJMST_GH = 200;
    private static final int MAX_COUNT_GET_SPKFK = 200;
    private static final int MAX_TRY_COUNT_GET_AJMST_GH = 10;
    private static final int MAX_TRY_COUNT_GET_SPKFK = 3;
    private static final int MSG_TYPE_GET_MAINTAIN = 1;
    private static final int MSG_TYPE_GET_SPKFK = 3;
    private static final int MSG_TYPE_UPLOAD_MAINTAIN = 2;
    private static final int REQUEST_CODE_CHOSE_FILE = 0;
    private MaintainService maintainService;
    private SharedPreferences preferencesOfMaintain;
    private SpkfkService spkfkService;
    Handler progressMsgHandler = new Handler() { // from class: com.ajmst.android.ui.ConfigActivity.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            ProgressBar prb = (ProgressBar) ConfigActivity.this.findViewById(R.id.prbRate);
            EditText etPrbInfo = (EditText) ConfigActivity.this.findViewById(R.id.etPrbInfo);
            String info = msg.obj.toString();
            int rate = msg.arg1;
            prb.setProgress(rate);
            etPrbInfo.setText(info);
        }
    };
    Handler consoleMsgHandler = new Handler() { // from class: com.ajmst.android.ui.ConfigActivity.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            String info;
            EditText etConsole = (EditText) ConfigActivity.this.findViewById(R.id.etConsole);
            String info2 = etConsole.getText().toString();
            if (msg.arg1 == 0) {
                info = msg.obj.toString();
            } else {
                info = String.valueOf(info2) + "\n" + msg.obj.toString();
            }
            etConsole.setText(info);
        }
    };
    final Handler wsMsgHandler = new Handler() { // from class: com.ajmst.android.ui.ConfigActivity.3
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            WsResponse r = (WsResponse) msg.obj;
            int msgType = msg.arg1;
            if (msgType == 1) {
                if (r.isOk()) {
                    List<AjmstMaintain> maintains = (List) r.getResult();
                    Toast.makeText(ConfigActivity.this, "获取养护检查表成功,共 " + maintains.size() + " 条", 1).show();
                    Toast.makeText(ConfigActivity.this, "准备创建本地养护检查表...", 1).show();
                    MaintainService maintainService = new MaintainService(ConfigActivity.this);
                    Toast.makeText(ConfigActivity.this, "删除本地旧数据...", 1).show();
                    int delCount = maintainService.clearData();
                    Toast.makeText(ConfigActivity.this, "删除本地旧数据,共 " + delCount + " 条", 1).show();
                    Toast.makeText(ConfigActivity.this, "开始创建本地养护检查表...", 1).show();
                    int failCount = 0;
                    for (int i = 0; i < maintains.size(); i++) {
                        boolean succeed = maintainService.create(maintains.get(i));
                        if (!succeed) {
                            failCount++;
                        }
                    }
                    if (failCount > 0) {
                        Toast.makeText(ConfigActivity.this, "本地养护检查表成功 " + (maintains.size() - failCount) + " 条,失败 " + failCount + " 条", 1).show();
                    } else {
                        Toast.makeText(ConfigActivity.this, "本地养护检查表全部创建成功 ,共" + maintains.size() + " 条", 1).show();
                    }
                    SharedPreferences.Editor editor = ConfigActivity.this.preferencesOfMaintain.edit();
                    editor.putString("lastImportTime", DateTimeUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
                    editor.putInt("lastInx", 0);
                    editor.putString("lastGH", "全部");
                    editor.commit();
                    return;
                }
                Toast.makeText(ConfigActivity.this, "获取养护检查表失败:" + r.getException().getMessage(), 1).show();
                return;
            }
            if (msgType == 2) {
                if (r.isOk()) {
                    Toast.makeText(ConfigActivity.this, "上传养护资料成功 ", 1).show();
                } else {
                    Toast.makeText(ConfigActivity.this, "上传养护资料失败:" + r.getException().getMessage(), 1).show();
                }
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public void showProgress(String info, int rate) {
        Message msg = new Message();
        msg.obj = info;
        msg.arg1 = rate;
        this.progressMsgHandler.sendMessage(msg);
        Log.i(LOG_TAG, info);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showConsole(String info) {
        showConsole(info, 1);
    }

    private void showConsole(String info, int addUp) {
        Message msg = new Message();
        msg.obj = info;
        msg.arg1 = addUp;
        this.consoleMsgHandler.sendMessage(msg);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        this.maintainService = new MaintainService(this);
        this.spkfkService = new SpkfkService(this);
        String preferencesName = getString(R.string.preferences_of_maintain);
        this.preferencesOfMaintain = getSharedPreferences(preferencesName, 0);
        Button buttonImportMaintain = (Button) findViewById(R.id.buttonImportMaintain);
        buttonImportMaintain.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.ConfigActivity.4
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("*/*");
                intent.addCategory("android.intent.category.OPENABLE");
                try {
                    Intent fileChoser = Intent.createChooser(intent, "请选择Excel文件");
                    ConfigActivity.this.startActivityForResult(fileChoser, 0);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ConfigActivity.this, "请先安装文件管理器", 0).show();
                }
            }
        });
        Button buttonGetMaintainFromServer = (Button) findViewById(R.id.buttonGetMaintainFromServer);
        buttonGetMaintainFromServer.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.ConfigActivity.5
            /* JADX WARN: Type inference failed for: r0v2, types: [com.ajmst.android.ui.ConfigActivity$5$1] */
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Toast.makeText(ConfigActivity.this, "正在从服务器下载数据,请稍后", 0).show();
                new Thread() { // from class: com.ajmst.android.ui.ConfigActivity.5.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        Message msg = new Message();
                        msg.obj = WsMaintainService.getMaintain(new Date());
                        msg.arg1 = 1;
                        ConfigActivity.this.wsMsgHandler.sendMessage(msg);
                    }
                }.start();
            }
        });
        Button buttonUploadMaintainToServer = (Button) findViewById(R.id.buttonUploadMaintainToServer);
        buttonUploadMaintainToServer.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.ConfigActivity.6
            /* JADX WARN: Type inference failed for: r0v2, types: [com.ajmst.android.ui.ConfigActivity$6$1] */
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Toast.makeText(ConfigActivity.this, "正在上传养护资料到服务器,请稍后", 0).show();
                new Thread() { // from class: com.ajmst.android.ui.ConfigActivity.6.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        WsResponse r;
                        Message msg = new Message();
                        MaintainService maintainService = new MaintainService(ConfigActivity.this);
                        List<AjmstMaintain> maintains = maintainService.getMaintainItems();
                        if (maintains.size() <= 0) {
                            r = new WsResponse();
                            r.setIsOk(false);
                            r.setException(new Exception("无数据需要上传"));
                        } else {
                            r = WsMaintainService.uploadMaintain(maintains);
                        }
                        msg.obj = r;
                        msg.arg1 = 2;
                        ConfigActivity.this.wsMsgHandler.sendMessage(msg);
                    }
                }.start();
            }
        });
        Button btnGetSpkfk = (Button) findViewById(R.id.btnGetSpkfk);
        btnGetSpkfk.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.ConfigActivity.7
            /* JADX WARN: Type inference failed for: r0v2, types: [com.ajmst.android.ui.ConfigActivity$7$1] */
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Toast.makeText(ConfigActivity.this, "正在下载商品资料,请稍后", 0).show();
                new Thread() { // from class: com.ajmst.android.ui.ConfigActivity.7.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        int sIdx = 0;
                        int rate = 0;
                        int partCount = 1;
                        int total = 0;
                        SpkfkService spService = new SpkfkService(ConfigActivity.this);
                        int reTryTime = 0;
                        while (true) {
                            String partInfo = "第 " + partCount + " 部分商品资料";
                            ConfigActivity.this.showProgress("正在获取" + partInfo + "...", rate);
                            WsResponse r = WsSpkfkService.getSpkfks(sIdx, 200);
                            if (!r.isOk()) {
                                ConfigActivity.this.showProgress("获取" + partInfo + "失败:" + r.getException().getMessage(), rate);
                                if (reTryTime >= 3) {
                                    ConfigActivity.this.showProgress(String.valueOf(reTryTime) + "次重试获取" + partInfo + "失败,已中止下载资料:" + r.getException().getMessage(), rate);
                                    return;
                                }
                                reTryTime++;
                                try {
                                    ConfigActivity.this.showProgress("10 秒后开始第" + reTryTime + "次重试获取" + partInfo, rate);
                                    Thread.sleep(10000L);
                                    ConfigActivity.this.showProgress("开始第 " + reTryTime + " 次重试获取" + partInfo, rate);
                                } catch (InterruptedException e) {
                                }
                            } else {
                                reTryTime = 0;
                                sIdx += 200;
                                int rate2 = rate + 1;
                                ConfigActivity.this.showProgress("获取" + partInfo + "成功", rate2);
                                List<Spkfk> sps = (List) r.getResult();
                                int rate3 = rate2 + 1;
                                ConfigActivity.this.showProgress("开始保存" + partInfo + "...", rate3);
                                try {
                                    List<AdvSpkfk> advSps = spService.toAdvSpkfk(sps);
                                    Response rSave = spService.saveOrUpdate((List) advSps);
                                    if (!rSave.isOk()) {
                                        ConfigActivity.this.showProgress("保存" + partInfo + "失败,停止下载,原因:" + r.getException().getMessage(), rate3);
                                        return;
                                    }
                                    rate = rate3 + 1;
                                    ConfigActivity.this.showProgress("保存" + partInfo + "成功", rate);
                                    total += sps.size();
                                    if (sps.size() < 200) {
                                        ConfigActivity.this.showProgress("商品资料全部下载成功,共 " + total + " 个", rate + 1);
                                        return;
                                    }
                                    partCount++;
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    ConfigActivity.this.showProgress("内部父类子类转换失败", rate3 + 1);
                                    return;
                                }
                            }
                        }
                    }
                }.start();
            }
        });
        Button btnGetSpkfkMultThread = (Button) findViewById(R.id.btnGetSpkfkMultThread);
        btnGetSpkfkMultThread.setOnClickListener(new AnonymousClass8());
        Button btnGetGh = (Button) findViewById(R.id.btnGetGh);
        btnGetGh.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.ConfigActivity.9
            /* JADX WARN: Type inference failed for: r0v2, types: [com.ajmst.android.ui.ConfigActivity$9$1] */
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Toast.makeText(ConfigActivity.this, "正在下载位置资料,请稍后", 0).show();
                new Thread() { // from class: com.ajmst.android.ui.ConfigActivity.9.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        int sIdx = 0;
                        int rate = 0;
                        int partCount = 1;
                        int total = 0;
                        int reTryTime = 0;
                        while (true) {
                            String partInfo = "第 " + partCount + " 部分位置资料";
                            ConfigActivity.this.showProgress("正在获取" + partInfo + "...", rate);
                            WsResponse r = WsGhService.getGhs(sIdx, 200);
                            if (!r.isOk()) {
                                ConfigActivity.this.showProgress("获取" + partInfo + "失败:" + r.getException().getMessage(), rate);
                                if (reTryTime >= 10) {
                                    ConfigActivity.this.showProgress(String.valueOf(reTryTime) + "次重试获取" + partInfo + "失败,已中止下载资料:" + r.getException().getMessage(), rate);
                                    return;
                                }
                                reTryTime++;
                                try {
                                    ConfigActivity.this.showProgress("10 秒后开始第" + reTryTime + "次重试获取" + partInfo, rate);
                                    Thread.sleep(10000L);
                                    ConfigActivity.this.showProgress("开始第 " + reTryTime + " 次重试获取" + partInfo, rate);
                                } catch (InterruptedException e) {
                                }
                            } else {
                                reTryTime = 0;
                                sIdx += 200;
                                int rate2 = rate + 1;
                                ConfigActivity.this.showProgress("获取" + partInfo + "成功", rate2);
                                List<AjmstGh> ghs = (List) r.getResult();
                                int rate3 = rate2 + 1;
                                ConfigActivity.this.showProgress("开始保存" + partInfo + "...", rate3);
                                Response rSave = ConfigActivity.this.spkfkService.updateCabinet(ghs);
                                if (!rSave.isOk()) {
                                    ConfigActivity.this.showProgress("保存" + partInfo + "失败,停止下载,原因:" + rSave.getException().getMessage(), rate3);
                                    return;
                                }
                                rate = rate3 + 1;
                                ConfigActivity.this.showProgress("保存" + partInfo + "成功", rate);
                                total += ghs.size();
                                if (ghs.size() < 200) {
                                    ConfigActivity.this.showProgress("位置资料全部下载成功,共 " + total + " 个", 100);
                                    return;
                                }
                                partCount++;
                            }
                        }
                    }
                }.start();
            }
        });
        Button buttonTest = (Button) findViewById(R.id.buttonTest);
        buttonTest.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.ConfigActivity.10
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
            }
        });
    }

    /* JADX INFO: renamed from: com.ajmst.android.ui.ConfigActivity$8, reason: invalid class name */
    class AnonymousClass8 implements View.OnClickListener {
        AnonymousClass8() {
        }

        /* JADX WARN: Type inference failed for: r0v0, types: [com.ajmst.android.ui.ConfigActivity$8$1] */
        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            new Thread() { // from class: com.ajmst.android.ui.ConfigActivity.8.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    WsResponse r = WsSpkfkService.getSpkfkSizeOfActive();
                    if (!r.isOk()) {
                        ConfigActivity.this.showProgress("获取总个数失败,无法下载资料,请检查网络", 0);
                        ConfigActivity.this.showConsole("获取总个数失败,无法下载资料,请检查网络");
                        return;
                    }
                    final Integer maxSize = (Integer) r.getResult();
                    final List<DownloadThread> threads = new ArrayList<>();
                    for (int currPos = 0; currPos < maxSize.intValue(); currPos += 4000) {
                        int delaySec = 5 * threads.size();
                        DownloadThread thread = new DownloadThread(ConfigActivity.this, currPos, 4000, delaySec);
                        thread.start();
                        threads.add(thread);
                    }
                    String info = "总共 " + maxSize + " 个,启动 " + threads.size() + " 个线程下载,每个线程下载 4000 个";
                    ConfigActivity.this.showProgress(info, 0);
                    ConfigActivity.this.showConsole(info);
                    final Timer timer = new Timer();
                    final Date startTime = new Date();
                    timer.schedule(new TimerTask() { // from class: com.ajmst.android.ui.ConfigActivity.8.1.1
                        @Override // java.util.TimerTask, java.lang.Runnable
                        public void run() {
                            int finishSize = 0;
                            int downLoadSize = 0;
                            boolean isAllThreadStop = true;
                            for (int i = 0; i < threads.size(); i++) {
                                Thread thread2 = (Thread) threads.get(i);
                                if (thread2.isAlive()) {
                                    isAllThreadStop = false;
                                }
                                finishSize += ((DownloadThread) threads.get(i)).getFinishSize();
                                downLoadSize += ((DownloadThread) threads.get(i)).getDownLoadSize();
                            }
                            String info2 = "进度 :已下载 " + downLoadSize + " ,已保存 " + finishSize + " ,总数 " + maxSize + " ,耗时 " + DateTimeUtils.getSeconds(startTime, new Date()) + " 秒";
                            ConfigActivity.this.showProgress(info2, finishSize / maxSize.intValue());
                            if (isAllThreadStop) {
                                timer.cancel();
                            }
                        }
                    }, 0L, 5000L);
                }
            }.start();
        }
    }

    @Override // android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1 && requestCode == 0) {
            Toast.makeText(this, "call back from file choser", 0).show();
            String path = data.getData().getPath();
            if (path != null && !"".equals(path)) {
                Toast.makeText(this, "正在导入数据,请稍后", 0).show();
                boolean result = this.maintainService.initData(path);
                if (result) {
                    SharedPreferences.Editor editor = this.preferencesOfMaintain.edit();
                    editor.putString("lastPath", path);
                    editor.putString("lastImportTime", DateTimeUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
                    editor.putInt("lastInx", 0);
                    editor.putString("lastGH", "全部");
                    editor.commit();
                    Toast.makeText(this, "导入成功", 0).show();
                    Log.i(getClass().getName(), "导入文件(" + path + ")到数据库成功");
                } else {
                    Toast.makeText(this, "导入数据失败,请检查文件是否正确", 0).show();
                    Log.e(getClass().getName(), "导入文件(" + path + ")到数据库失败");
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config, menu);
        return true;
    }
}
