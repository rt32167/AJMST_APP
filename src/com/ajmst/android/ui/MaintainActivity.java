package com.ajmst.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.ajmst.android.R;
import com.ajmst.android.service.MaintainService;
import com.ajmst.android.ui.maintain.MaintainItemListAdapter;
import com.ajmst.android.util.StringUtils;
import com.ajmst.commmon.entity.AjmstMaintain;
import com.ajmst.common.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class MaintainActivity extends Activity {
    ArrayList<Map<String, Object>> items = new ArrayList<>();
    private String lastImportTime;
    private MaintainItemListAdapter listAdapter;
    Context mContext;
    List<AjmstMaintain> maintainItemList;
    private MaintainService maintainService;
    private SharedPreferences preferences;

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maintain);
        this.mContext = this;
        this.maintainService = new MaintainService(this.mContext);
        String preferencesName = getString(R.string.preferences_of_maintain);
        this.preferences = getSharedPreferences(preferencesName, 0);
        Button buttonGH = (Button) findViewById(R.id.buttonGH);
        String lastGH = this.preferences.getString("lastGH", "全部");
        buttonGH.setText(lastGH);
        buttonGH.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.MaintainActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                final CharSequence[] cabinets = {"全部", "未完成", "1柜", "2柜", "3柜", "4柜", "5柜", "6柜", "7柜", "8柜", "9柜", "10柜", "11柜", "12柜", "13柜", "14柜", "A柜", "B柜", "C柜", "D柜", "E柜", "F柜", "冰箱", "里面"};
                new AlertDialog.Builder(MaintainActivity.this).setTitle((CharSequence) null).setItems(cabinets, new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.MaintainActivity.1.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        String cabinet = cabinets[which].toString();
                        Button buttonGH2 = (Button) MaintainActivity.this.findViewById(R.id.buttonGH);
                        buttonGH2.setText(cabinet);
                        MaintainActivity.this.showData(cabinet);
                        dialog.dismiss();
                    }
                }).show();
            }
        });
        ListView maintainList = (ListView) findViewById(R.id.listView1);
        maintainList.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.ajmst.android.ui.MaintainActivity.2
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long arg3) {
                Intent intent = new Intent(MaintainActivity.this, (Class<?>) NumberInputActivity.class);
                TextView tvShl = (TextView) v.findViewById(R.id.tvShl);
                String number = tvShl.getText().toString();
                intent.putExtra(NumberInputActivity.NUMBER, number);
                intent.putExtra(NumberInputActivity.TAG, new StringBuilder().append(position).toString());
                intent.putExtra(NumberInputActivity.DECIMAL_COUT, 2);
                MaintainActivity.this.startActivityForResult(intent, 0);
            }
        });
        this.lastImportTime = this.preferences.getString("lastImportTime", null);
        showData(lastGH);
    }

    @Override // android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1 && data != null) {
            String number = data.getStringExtra(NumberInputActivity.NUMBER);
            int position = Integer.valueOf(data.getStringExtra(NumberInputActivity.TAG)).intValue();
            if (number != null && !"".equals(number)) {
                ListView lvMaintain = (ListView) findViewById(R.id.listView1);
                AjmstMaintain mt = (AjmstMaintain) lvMaintain.getItemAtPosition(position);
                mt.setShl(Double.valueOf(StringUtils.stringtodouble(number)));
                Response r = this.maintainService.saveOrUpdate(mt);
                if (r.isOk()) {
                    int idx = this.maintainItemList.indexOf(mt);
                    SharedPreferences.Editor editor = this.preferences.edit();
                    editor.putInt("lastInx", idx);
                    Button buttonGH = (Button) findViewById(R.id.buttonGH);
                    editor.putString("lastGH", buttonGH.getText().toString());
                    editor.commit();
                    this.listAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this.mContext, "更新失败:" + r.getExceptionStr(), 0).show();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int showData(String gh) {
        String lastGH = this.preferences.getString("lastGH", "全部");
        int selectIdx = this.preferences.getInt("lastInx", 0);
        this.maintainItemList = this.maintainService.getMaintainItemsByGH(gh);
        System.out.println("size:" + this.maintainItemList.size());
        this.listAdapter = new MaintainItemListAdapter(this, this.maintainItemList);
        ListView maintainListView = (ListView) findViewById(R.id.listView1);
        maintainListView.setAdapter((ListAdapter) this.listAdapter);
        if (lastGH.endsWith(gh)) {
            maintainListView.setSelection(selectIdx);
        }
        Toast.makeText(this.mContext, String.valueOf(gh) + ":" + this.maintainItemList.size() + " 个", 0).show();
        return this.maintainItemList.size();
    }

    @Override // android.app.Activity
    protected void onResume() {
        String newLastImportTime = this.preferences.getString("lastImportTime", null);
        if (this.lastImportTime != newLastImportTime) {
            Toast.makeText(this, "注意导入了新数据,系统自动刷新", 1).show();
            Button buttonGH = (Button) findViewById(R.id.buttonGH);
            buttonGH.setText("全部");
            showData("全部");
            this.lastImportTime = newLastImportTime;
        }
        super.onResume();
    }
}
