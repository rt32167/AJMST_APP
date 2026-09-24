package com.ajmst.android.ui.spkfk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.ajmst.android.R;
import com.ajmst.android.application.AjmstApplication;
import com.ajmst.android.barcode.client.CaptureActivity;
import com.ajmst.android.entity.AdvSpkfk;
import com.ajmst.android.entity.SalesOrder;
import com.ajmst.android.entity.SalesOrderItem;
import com.ajmst.android.salesorder.SalesOrderActivity;
import com.ajmst.android.service.MsgQueueService;
import com.ajmst.android.service.SalesOrderService;
import com.ajmst.android.service.SpkfkService;
import com.ajmst.android.ui.NumberInputActivity;
import com.ajmst.common.response.Response;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class SpkfkSelectActivity extends Activity implements GestureDetector.OnGestureListener {
    public static final String EXTRA_NAME_SELECTED_SPKFK = "Selected_Spkfk";
    public static final int FLAG_FIND_SP = 4;
    public static final int REQUEST_CODE_GET_QUANTITY = 4;
    public static final int REQUEST_CODE_SPKFK_DETAIL = 3;
    public static final int REQUEST_CODE_VIEW_ORDER = 5;
    private AjmstApplication app;
    private GestureDetector gestureDetector = null;
    private MsgQueueService msgQueueService;
    private SalesOrder salesOrder;
    private SalesOrderService salesOrderService;
    private SpkfkService spService;
    private List<AdvSpkfk> spkfks;

    @Override // android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spkfk_select);
        this.gestureDetector = new GestureDetector(this, this);
        final ListView listViewSpkfk = (ListView) findViewById(R.id.listViewSpkfk);
        View.OnTouchListener onTouchListener = new View.OnTouchListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.1
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View v, MotionEvent event) {
                SpkfkSelectActivity.this.gestureDetector.onTouchEvent(event);
                return false;
            }
        };
        listViewSpkfk.setOnTouchListener(onTouchListener);
        this.app = (AjmstApplication) getApplication();
        this.spService = new SpkfkService(this);
        this.salesOrderService = new SalesOrderService(this);
        this.msgQueueService = new MsgQueueService(this);
        this.salesOrder = this.salesOrderService.getUnFinishedOrder();
        this.app.setCurrSalesOrder(this.salesOrder);
        if (this.salesOrder != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("是否继续上次未完成的单据?");
            builder.setTitle("提示");
            builder.setCancelable(false);
            builder.setPositiveButton("是", new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.2
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("否", new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.3
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialog, int which) {
                    SpkfkSelectActivity.this.salesOrderService.finishOrder(SpkfkSelectActivity.this.salesOrder);
                    SpkfkSelectActivity.this.app.setCurrSalesOrder(null);
                    SpkfkSelectActivity.this.refreshSpkfk();
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
        EditText etZjm = (EditText) findViewById(R.id.etZjm);
        etZjm.addTextChangedListener(new TextWatcher() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.4
            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 1) {
                    SpkfkSelectActivity.this.searchSpkfk();
                }
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        final Button btnCabinet = (Button) findViewById(R.id.btnCabinet);
        btnCabinet.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                final CharSequence[] cabinets = {"全柜", "1柜", "2柜", "3柜", "4柜", "5柜", "6柜", "7柜", "8柜", "9柜", "10柜", "11柜", "12柜", "13柜", "14柜", "A柜", "B柜", "C柜", "D柜", "E柜", "F柜", "冰箱", "里面", "其他"};
                AlertDialog.Builder title = new AlertDialog.Builder(SpkfkSelectActivity.this).setTitle((CharSequence) null);
                final Button button = btnCabinet;
                title.setItems(cabinets, new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.5.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        String tag;
                        String cabinet = cabinets[which].toString();
                        button.setText(cabinet);
                        if ("全柜".equals(cabinet)) {
                            tag = null;
                        } else if ("其他".equals(cabinet)) {
                            tag = "null";
                        } else {
                            tag = cabinet.replace("柜", "");
                        }
                        button.setTag(tag);
                        dialog.dismiss();
                    }
                }).show();
            }
        });
        final Button btnPrice = (Button) findViewById(R.id.btnPrice);
        btnPrice.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.6
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                final CharSequence[] prices = {"全价", "0-5", "6-10", "11-15", "16-20", "21-25", "26-30", "31-35", "36-40", "41-50", "51-60", "61-70", "71-80", "81-90", "91-99", "100以上"};
                AlertDialog.Builder title = new AlertDialog.Builder(SpkfkSelectActivity.this).setTitle((CharSequence) null);
                final Button button = btnPrice;
                title.setItems(prices, new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.6.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        BigDecimal from;
                        BigDecimal end;
                        String price = prices[which].toString();
                        button.setText(price);
                        if ("全价".equals(price)) {
                            button.setTag(null);
                        } else {
                            if ("100以上".equals(price)) {
                                from = BigDecimal.valueOf(100L);
                                end = BigDecimal.valueOf(2147483647L);
                            } else {
                                from = new BigDecimal(price.split("-")[0]);
                                end = new BigDecimal(price.split("-")[1]);
                            }
                            BigDecimal[] priceArray = {from, end};
                            button.setTag(priceArray);
                        }
                        dialog.dismiss();
                    }
                }).show();
            }
        });
        final Button btnType = (Button) findViewById(R.id.btnType);
        btnType.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.7
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                final CharSequence[] types = {"全类", "中药", "西药"};
                AlertDialog.Builder title = new AlertDialog.Builder(SpkfkSelectActivity.this).setTitle((CharSequence) null);
                final Button button = btnType;
                title.setItems(types, new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.7.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        String type = types[which].toString();
                        button.setText(type);
                        dialog.dismiss();
                    }
                }).show();
            }
        });
        btnType.setText("中药");
        Button btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.8
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                SpkfkSelectActivity.this.searchSpkfk();
            }
        });
        Button btnViewOrder = (Button) findViewById(R.id.btnViewOrder);
        btnViewOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpkfkSelectActivity.this.showOrderDetail();
            }
        });
        updateOrderButton();
        Intent externalIntent = getIntent();
        if (externalIntent != null && externalIntent.getFlags() == 4) {
            final String sptm = externalIntent.getStringExtra(CaptureActivity.EXTRA_NAME_SPTM);
            listViewSpkfk.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.9
                @Override // android.widget.AdapterView.OnItemClickListener
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    AdvSpkfk sp = (AdvSpkfk) listViewSpkfk.getItemAtPosition(position);
                    Response r = SpkfkSelectActivity.this.spService.updateSptm(sp, sptm);
                    if (r.isOk()) {
                        SpkfkSelectActivity.this.msgQueueService.createMsg_Sptm(sp.getSpid(), sptm);
                        Toast.makeText(SpkfkSelectActivity.this, "条码修改成功", 1).show();
                    } else {
                        Toast.makeText(SpkfkSelectActivity.this, "条码修改失败:" + r.getExceptionStr(), 1).show();
                    }
                    SpkfkSelectActivity.this.finish();
                }
            });
        } else {
            listViewSpkfk.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AdvSpkfk sp = (AdvSpkfk) listViewSpkfk.getItemAtPosition(position);
                    Intent detail = new Intent(SpkfkSelectActivity.this, SpkfkDetailActivity.class);
                    detail.putExtra(SpkfkDetailActivity.EXTRA_NAME_SPKFK, sp);
                    startActivityForResult(detail, REQUEST_CODE_SPKFK_DETAIL);
                }
            });
        }
        listViewSpkfk.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.10
            @Override // android.widget.AdapterView.OnItemLongClickListener
            public boolean onItemLongClick(AdapterView<?> arg0, View v, final int position, long id) {
                final CharSequence[] choices = {"查看详情", "查看销售单"};
                ListView lvSpkfk = (ListView) SpkfkSelectActivity.this.findViewById(R.id.listViewSpkfk);
                final AdvSpkfk sp = (AdvSpkfk) lvSpkfk.getItemAtPosition(position);
                new AlertDialog.Builder(SpkfkSelectActivity.this).setTitle((CharSequence) null).setItems(choices, new DialogInterface.OnClickListener() { // from class: com.ajmst.android.ui.spkfk.SpkfkSelectActivity.10.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        String choice = choices[which].toString();
                        switch (which) {
                            case 0:
                                Intent intentViewDetail = new Intent(SpkfkSelectActivity.this, (Class<?>) SpkfkDetailActivity.class);
                                intentViewDetail.putExtra(SpkfkDetailActivity.EXTRA_NAME_SPKFK, sp);
                                intentViewDetail.putExtra(NumberInputActivity.TAG, new StringBuilder().append(position).toString());
                                SpkfkSelectActivity.this.startActivityForResult(intentViewDetail, 3);
                                break;
                            case 1:
                                SpkfkSelectActivity.this.showOrderDetail();
                                break;
                        }
                        Toast.makeText(SpkfkSelectActivity.this, "选择了 " + choice, 1).show();
                        dialog.dismiss();
                    }
                }).show();
                return true;
            }
        });
        searchSpkfk();
    }

    public void addToOrderBtnClick(final int position) {
        ListView lvSpkfk = (ListView) findViewById(R.id.listViewSpkfk);
        final AdvSpkfk sp = (AdvSpkfk) lvSpkfk.getItemAtPosition(position);
        final BigDecimal currentQuantity = getQuantityInOrder(sp.getSpid());
        final String[] quantities = {"4", "5", "6", "8", "9", "10", "12", "15", "20", "25", "30", "60"};
        final Dialog dialog = new Dialog(this, R.style.NumberInputStyle);
        dialog.setContentView(R.layout.dialog_quantity);
        boolean chineseMedicine = sp.getSpbh() != null && SpkfkService.isSelfCnSp(sp.getSpbh());
        final String unit = chineseMedicine ? "g" : sp.getDw();
        ((TextView) dialog.findViewById(R.id.tvQuantityTitle)).setText(
                currentQuantity == null ? "选择数量" : "修改数量");
        TextView hint = (TextView) dialog.findViewById(R.id.tvQuantityHint);
        if (currentQuantity == null) {
            hint.setText(unit == null || unit.length() == 0 ? "点击常用数量，或输入其他数量" : "数量单位：" + unit);
        } else {
            hint.setText("当前 " + currentQuantity.stripTrailingZeros().toPlainString()
                    + (unit == null ? "" : unit) + " · 选择新数量");
        }
        LinearLayout rows = (LinearLayout) dialog.findViewById(R.id.quantityPresetRows);
        int gap = (int) (4 * getResources().getDisplayMetrics().density + 0.5f);
        for (int row = 0; row < 4; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (row > 0) {
                lineParams.topMargin = gap;
            }
            rows.addView(line, lineParams);
            for (int column = 0; column < 3; column++) {
                final int quantity = Integer.parseInt(quantities[row * 3 + column]);
                Button button = new Button(this);
                button.setText(String.valueOf(quantity));
                button.setTextSize(18);
                button.setTextColor(getResources().getColor(R.color.ui_accent));
                button.setBackgroundResource(R.drawable.ui_secondary_button);
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0,
                        (int) (46 * getResources().getDisplayMetrics().density + 0.5f), 1);
                buttonParams.leftMargin = gap;
                buttonParams.rightMargin = gap;
                line.addView(button, buttonParams);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        saveSelectedQuantity(sp, BigDecimal.valueOf(quantity));
                    }
                });
            }
        }
        dialog.findViewById(R.id.btnQuantityCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.btnQuantityOther).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(SpkfkSelectActivity.this, NumberInputActivity.class);
                intent.putExtra(NumberInputActivity.NUMBER,
                        currentQuantity == null ? "" : currentQuantity.stripTrailingZeros().toPlainString());
                intent.putExtra(NumberInputActivity.TITLE,
                        (currentQuantity == null ? "输入数量" : "修改数量")
                                + (unit == null || unit.length() == 0 ? "" : "（" + unit + "）"));
                intent.putExtra(NumberInputActivity.TAG, String.valueOf(position));
                intent.putExtra(NumberInputActivity.DECIMAL_COUT, 2);
                startActivityForResult(intent, REQUEST_CODE_GET_QUANTITY);
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setLayout(getResources().getDisplayMetrics().widthPixels -
                (int) (32 * getResources().getDisplayMetrics().density + 0.5f),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private BigDecimal getQuantityInOrder(String spid) {
        if (spid == null || salesOrder == null || salesOrder.getItems() == null) {
            return null;
        }
        BigDecimal total = null;
        for (SalesOrderItem item : salesOrder.getItems()) {
            if (spid.equals(item.getSpid()) && item.getShl() != null) {
                BigDecimal quantity = BigDecimal.valueOf(item.getShl());
                total = total == null ? quantity : total.add(quantity);
            }
        }
        return total;
    }

    private void saveSelectedQuantity(AdvSpkfk sp, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
                || quantity.stripTrailingZeros().scale() > 2
                || Double.isInfinite(quantity.doubleValue())) {
            Toast.makeText(this, "请输入大于0、最多两位小数的数量", Toast.LENGTH_SHORT).show();
            return;
        }
        BigDecimal currentQuantity = getQuantityInOrder(sp.getSpid());
        if (currentQuantity == null) {
            addItem(sp, null, quantity.doubleValue());
            return;
        }
        if (currentQuantity.compareTo(quantity) == 0) {
            return;
        }
        List<SalesOrderItem> items = salesOrder.getItems();
        List<SalesOrderItem> previousItems = new ArrayList<SalesOrderItem>(items);
        List<Double> previousQuantities = new ArrayList<Double>();
        for (SalesOrderItem item : previousItems) {
            previousQuantities.add(item.getShl());
        }
        BigDecimal change = quantity.subtract(currentQuantity);
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            for (int i = items.size() - 1; i >= 0; i--) {
                SalesOrderItem item = items.get(i);
                if (sp.getSpid().equals(item.getSpid()) && item.getShl() != null) {
                    item.setShl(BigDecimal.valueOf(item.getShl()).add(change).doubleValue());
                    break;
                }
            }
        } else {
            BigDecimal toRemove = change.negate();
            for (int i = items.size() - 1; i >= 0 && toRemove.compareTo(BigDecimal.ZERO) > 0; i--) {
                SalesOrderItem item = items.get(i);
                if (sp.getSpid().equals(item.getSpid()) && item.getShl() != null) {
                    BigDecimal itemQuantity = BigDecimal.valueOf(item.getShl());
                    if (itemQuantity.compareTo(toRemove) > 0) {
                        item.setShl(itemQuantity.subtract(toRemove).doubleValue());
                        toRemove = BigDecimal.ZERO;
                    } else {
                        items.remove(i);
                        toRemove = toRemove.subtract(itemQuantity);
                    }
                }
            }
        }
        Response result = salesOrderService.saveOrUpdate(salesOrder);
        if (!result.isOk()) {
            salesOrder.setItems(previousItems);
            for (int i = 0; i < previousItems.size(); i++) {
                previousItems.get(i).setShl(previousQuantities.get(i));
            }
            Toast.makeText(this, "数量保存失败", Toast.LENGTH_SHORT).show();
            return;
        }
        refreshSpkfk();
        Toast.makeText(this, "数量已修改为 " + quantity.stripTrailingZeros().toPlainString(), Toast.LENGTH_SHORT).show();
    }

    public void addItem(AdvSpkfk sp, String pihao, Integer shl) {
        addItem(sp, pihao, Double.valueOf(shl.intValue()));
    }

    public void addItem(AdvSpkfk sp, String pihao, Double shl) {
        if (this.salesOrder == null) {
            String orderNo = this.salesOrderService.generateOrderNo();
            this.salesOrder = new SalesOrder(orderNo);
            this.app.setCurrSalesOrder(this.salesOrder);
        }
        this.salesOrder.addItem(sp, (String) null, shl);
        this.salesOrderService.saveOrUpdate(this.salesOrder);
        refreshSpkfk();
        Toast.makeText(this, "添加成功,数量:" + shl, 0).show();
        EditText etZjm = (EditText) findViewById(R.id.etZjm);
        etZjm.setText((CharSequence) null);
    }

    private void displaySpkfk() {
        ListView listViewSpkfk = (ListView) findViewById(R.id.listViewSpkfk);
        TextView resultCount = (TextView) findViewById(R.id.tvResultCount);
        resultCount.setText("搜索结果 · " + (this.spkfks == null ? 0 : this.spkfks.size()) + " 条");
        SpkfkSelectListAdaper lvAdapter = new SpkfkSelectListAdaper(this, this.spkfks);
        lvAdapter.setSalesOrder(this.salesOrder);
        listViewSpkfk.setAdapter((ListAdapter) lvAdapter);
        updateOrderButton();
    }

    private void updateOrderButton() {
        int count = salesOrder == null || salesOrder.getItems() == null
                ? 0 : salesOrder.getItems().size();
        Button btnViewOrder = (Button) findViewById(R.id.btnViewOrder);
        btnViewOrder.setText("销售单 " + count + "项");
        NumberFormat amountFormat = NumberFormat.getNumberInstance();
        amountFormat.setMaximumFractionDigits(3);
        double amount = count == 0 ? 0 : SalesOrderService.getOrderAmount(salesOrder);
        TextView tvOrderAmount = (TextView) findViewById(R.id.tvOrderAmount);
        tvOrderAmount.setText("合计 ¥" + amountFormat.format(amount));
        tvOrderAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOrderDetail();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshSpkfk() {
        ListView lvSpkfk = (ListView) findViewById(R.id.listViewSpkfk);
        SpkfkSelectListAdaper lvAdapter = (SpkfkSelectListAdaper) lvSpkfk.getAdapter();
        this.salesOrder = this.app.getCurrSalesOrder();
        if (lvAdapter != null) {
            lvAdapter.setSalesOrder(this.salesOrder);
            lvAdapter.notifyDataSetChanged();
        }
        updateOrderButton();
    }

    public void searchSpkfk() {
        EditText etZjm = (EditText) findViewById(R.id.etZjm);
        Button btnCabinet = (Button) findViewById(R.id.btnCabinet);
        Button btnPrice = (Button) findViewById(R.id.btnPrice);
        Button btnType = (Button) findViewById(R.id.btnType);
        String zjm = etZjm.getText().toString();
        String cabinet = null;
        BigDecimal priceFrom = null;
        BigDecimal priceEnd = null;
        if (btnCabinet.getTag() != null) {
            cabinet = btnCabinet.getTag().toString();
        }
        if (btnPrice.getTag() != null) {
            BigDecimal[] priceSection = (BigDecimal[]) btnPrice.getTag();
            priceFrom = priceSection[0];
            priceEnd = priceSection[1];
        }
        Boolean isSelfCnSp = null;
        String type = btnType.getText().toString();
        if ("中药".equals(type)) {
            isSelfCnSp = true;
        } else if ("西药".equals(type)) {
            isSelfCnSp = false;
        }
        this.spkfks = this.spService.query(zjm, cabinet, priceFrom, priceEnd, isSelfCnSp);
        displaySpkfk();
    }

    @Override // android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_VIEW_ORDER) {
            refreshSpkfk();
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (data != null) {
            ListView lvSpkfk = (ListView) findViewById(R.id.listViewSpkfk);
            SpkfkSelectListAdaper lvAdapter = (SpkfkSelectListAdaper) lvSpkfk.getAdapter();
            if (requestCode == 4 && resultCode == 1) {
                String number = data.getStringExtra(NumberInputActivity.NUMBER);
                int position = Integer.valueOf(data.getStringExtra(NumberInputActivity.TAG)).intValue();
                if (number != null && !"".equals(number)) {
                    ListView lvSpkfk2 = (ListView) findViewById(R.id.listViewSpkfk);
                    AdvSpkfk sp = (AdvSpkfk) lvSpkfk2.getItemAtPosition(position);
                    try {
                        saveSelectedQuantity(sp, new BigDecimal(number));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "请输入有效数量", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == 3 && resultCode == 1) {
                AdvSpkfk sp2 = (AdvSpkfk) data.getSerializableExtra(SpkfkDetailActivity.EXTRA_NAME_SPKFK);
                if (sp2 != null) {
                    lvAdapter.setSpkfk(sp2);
                    refreshSpkfk();
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override // android.app.Activity
    public boolean onTouchEvent(MotionEvent event) {
        return this.gestureDetector.onTouchEvent(event);
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e2.getX() - e1.getX() <= 250.0f && e2.getX() - e1.getX() < -250.0f) {
            showOrderDetail();
            return true;
        }
        return true;
    }

    public void showOrderDetail() {
        Intent intentViewOrder = new Intent(this, (Class<?>) SalesOrderActivity.class);
        startActivityForResult(intentViewOrder, 5);
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent e) {
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent e) {
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
}
