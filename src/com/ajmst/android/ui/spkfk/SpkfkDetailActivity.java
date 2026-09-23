package com.ajmst.android.ui.spkfk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.ajmst.android.R;
import com.ajmst.android.barcode.client.CaptureActivity;
import com.ajmst.android.barcode.client.Intents;
import com.ajmst.android.entity.AdvSpkfk;
import com.ajmst.android.service.MsgQueueService;
import com.ajmst.android.service.SpkfkService;
import com.ajmst.android.ui.NumberInputActivity;
import com.ajmst.common.response.Response;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.math.BigDecimal;

/* JADX INFO: loaded from: classes.dex */
public class SpkfkDetailActivity extends Activity {
    public static final String EXTRA_NAME_SPKFK = "Spkfk";
    private static final int REQUEST_CODE_GET_LSHJ = 2;
    public static final int RESULT_CODE_SUCCESS = 1;
    private MsgQueueService msgQueueService;
    private AdvSpkfk sp;
    private SpkfkService spService;

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        setContentView(R.layout.activity_spkfk_detail);
        this.spService = new SpkfkService(this);
        this.msgQueueService = new MsgQueueService(this);
        this.sp = (AdvSpkfk) getIntent().getSerializableExtra(EXTRA_NAME_SPKFK);
        display();
        Button btnReturn = (Button) findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkDetailActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                SpkfkDetailActivity.this.finish();
            }
        });
        Button btnCabinet = (Button) findViewById(R.id.btnCabinet);
        btnCabinet.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkDetailActivity.2
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                final CharSequence[] cabinets = {"", "1柜", "2柜", "3柜", "4柜", "5柜", "6柜", "7柜", "8柜", "9柜", "10柜", "11柜", "12柜", "13柜", "14柜", "A柜", "B柜", "C柜", "D柜", "E柜", "F柜", "冰箱", "里面", "其他"};
                new AlertDialog.Builder(SpkfkDetailActivity.this).setTitle((CharSequence) null).setItems(cabinets, new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkDetailActivity.2.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        String cabinet = cabinets[which].toString().replace("柜", "");
                        if ("".equals(cabinet)) {
                            cabinet = null;
                        }
                        SpkfkDetailActivity.this.sp.setGh(cabinet);
                        Response r = SpkfkDetailActivity.this.spService.saveOrUpdate(SpkfkDetailActivity.this.sp);
                        if (r.isOk()) {
                            Toast.makeText(SpkfkDetailActivity.this, "修改柜号成功", 1).show();
                            SpkfkDetailActivity.this.notifyDataChanged();
                        } else {
                            Toast.makeText(SpkfkDetailActivity.this, "修改柜号失败", 1).show();
                        }
                        dialog.dismiss();
                    }
                }).show();
            }
        });
        Button btnLshj = (Button) findViewById(R.id.btnLshj);
        btnLshj.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkDetailActivity.3
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intent intent = new Intent(SpkfkDetailActivity.this, (Class<?>) NumberInputActivity.class);
                String number = new StringBuilder().append(SpkfkDetailActivity.this.sp.getLshj().doubleValue()).toString();
                intent.putExtra(NumberInputActivity.NUMBER, number);
                intent.putExtra(NumberInputActivity.TITLE, "修改零售价");
                intent.putExtra(NumberInputActivity.DECIMAL_COUT, 2);
                SpkfkDetailActivity.this.startActivityForResult(intent, 2);
            }
        });
        Button btnSptm = (Button) findViewById(R.id.btnSptm);
        btnSptm.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkDetailActivity.4
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intent intent = new Intent(SpkfkDetailActivity.this, (Class<?>) CaptureActivity.class);
                intent.setAction(Intents.Scan.ACTION);
                SpkfkDetailActivity.this.startActivityForResult(intent, IntentIntegrator.REQUEST_CODE);
            }
        });
        Button btnSptmByHand = (Button) findViewById(R.id.btnSptmByHand);
        btnSptmByHand.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkDetailActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                EditText etSptm = (EditText) SpkfkDetailActivity.this.findViewById(R.id.etSptm);
                String sptm = etSptm.getText().toString().trim();
                if (sptm.length() != 13) {
                    Toast.makeText(SpkfkDetailActivity.this, "注意商品条码一般为13位,当前条码位数为" + sptm.length() + "位", 1).show();
                }
                Response r = SpkfkDetailActivity.this.spService.updateSptm(SpkfkDetailActivity.this.sp, sptm);
                if (r.isOk()) {
                    SpkfkDetailActivity.this.msgQueueService.createMsg_Sptm(SpkfkDetailActivity.this.sp.getSpid(), sptm);
                    Toast.makeText(SpkfkDetailActivity.this, "修改条码成功", 1).show();
                    SpkfkDetailActivity.this.notifyDataChanged();
                    return;
                }
                Toast.makeText(SpkfkDetailActivity.this, "修改条码失败", 0).show();
            }
        });
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.spkfk_detail, menu);
        return true;
    }

    @Override // android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result;
        if (requestCode == 2) {
            if (data != null) {
                String number = data.getStringExtra(NumberInputActivity.NUMBER);
                this.sp.setLshj(new BigDecimal(number));
                Response r = this.spService.saveOrUpdate(this.sp);
                if (r.isOk()) {
                    this.msgQueueService.createMsg_Lshj(this.sp.getSpid(), this.sp.getLshj());
                    Toast.makeText(this, "修改零售价成功", 1).show();
                    notifyDataChanged();
                    return;
                }
                Toast.makeText(this, "修改零售价失败", 1).show();
                return;
            }
            return;
        }
        if (requestCode == 49374 && (result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)) != null) {
            String sptm = result.getContents();
            if (sptm != null) {
                EditText etSptm = (EditText) findViewById(R.id.etSptm);
                etSptm.setText(sptm.trim());
            } else {
                Toast.makeText(this, "未解析出任何条码", 1).show();
            }
        }
    }

    private void display() {
        TextView tvSpid = (TextView) findViewById(R.id.tvSpid);
        tvSpid.setText(this.sp.getSpid());
        TextView tvSpbh = (TextView) findViewById(R.id.tvSpbh);
        tvSpbh.setText(this.sp.getSpbh());
        TextView tvSpmch = (TextView) findViewById(R.id.tvSpmch);
        tvSpmch.setText(this.sp.getSpmch());
        TextView tvDw = (TextView) findViewById(R.id.tvDw);
        tvDw.setText(this.sp.getDw());
        TextView tvShpgg = (TextView) findViewById(R.id.tvShpgg);
        tvShpgg.setText(this.sp.getShpgg());
        findViewById(R.id.rowSpecification).setVisibility(isBlank(this.sp.getShpgg()) ? View.GONE : View.VISIBLE);
        TextView tvShengccj = (TextView) findViewById(R.id.tvShengccj);
        tvShengccj.setText(this.sp.getShengccj());
        tvShengccj.setVisibility(isBlank(this.sp.getShengccj()) ? View.GONE : View.VISIBLE);
        findViewById(R.id.rowUnit).setVisibility(isBlank(this.sp.getDw()) ? View.GONE : View.VISIBLE);
        TextView tvCabinetNo = (TextView) findViewById(R.id.tvCabinetNo);
        tvCabinetNo.setText(this.sp.getGh());
        TextView tvLshj = (TextView) findViewById(R.id.tvLshj);
        tvLshj.setText(this.sp.getLshj().toString());
        findViewById(R.id.tvPriceUnit).setVisibility(
                this.sp.getSpbh() != null && SpkfkService.isSelfCnSp(this.sp.getSpbh()) ? View.VISIBLE : View.GONE);
        EditText etSptm = (EditText) findViewById(R.id.etSptm);
        etSptm.setText(this.sp.getSptm());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDataChanged() {
        display();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_NAME_SPKFK, this.sp);
        setResult(1, resultIntent);
    }
}
