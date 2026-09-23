package com.ajmst.android.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TabHost;
import com.ajmst.android.R;
import com.ajmst.android.ui.spkfk.SpkfkSelectActivity;

/* JADX INFO: loaded from: classes.dex */
public class MainActivity extends TabActivity {
    private TabHost mTabHost;

    @Override // android.app.ActivityGroup, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.setProperty("http.keepAlive", "false");
        requestWindowFeature(1);
        setContentView(R.layout.tab_main);
        this.mTabHost = getTabHost();
        Resources rs = getResources();
        Intent layoutIntentSpSelect = new Intent();
        layoutIntentSpSelect.setClass(this, SpkfkSelectActivity.class);
        TabHost.TabSpec tabSpSelect = this.mTabHost.newTabSpec("layout5");
        tabSpSelect.setIndicator("商品查询", rs.getDrawable(android.R.drawable.stat_sys_phone_call));
        tabSpSelect.setContent(layoutIntentSpSelect);
        this.mTabHost.addTab(tabSpSelect);
        this.mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() { // from class: com.ajmst.android.ui.MainActivity.1
            @Override // android.widget.TabHost.OnTabChangeListener
            public void onTabChanged(String tabId) {
                System.out.println(tabId);
                if ("layout4".equals(tabId)) {
                    MainActivity.this.setRequestedOrientation(0);
                } else {
                    MainActivity.this.setRequestedOrientation(1);
                }
            }
        });
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tab_main, menu);
        return true;
    }

    @Override // android.app.Activity
    public void finish() {
        Intent stopIntent = new Intent(this, (Class<?>) MsgSendService.class);
        stopService(stopIntent);
        super.finish();
    }
}
