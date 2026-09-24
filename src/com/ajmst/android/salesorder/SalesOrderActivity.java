package com.ajmst.android.salesorder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.ajmst.android.R;
import com.ajmst.android.application.AjmstApplication;
import com.ajmst.android.entity.SalesOrder;
import com.ajmst.android.entity.SalesOrderItem;
import com.ajmst.android.service.SalesOrderService;
import com.ajmst.android.service.SpkfkService;
import com.ajmst.android.ui.NumberInputActivity;
import com.ajmst.common.response.Response;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SalesOrderActivity extends Activity {
	private static final int REQUEST_CODE_EDIT_QUANTITY = 6;
	private AjmstApplication app;
	private SalesOrder salesOrder;
	private SalesOrderService salesOrderService;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_sales_order);
		
		app = (AjmstApplication)getApplication();
		salesOrder = app.getCurrSalesOrder();
		this.salesOrderService = new SalesOrderService(SalesOrderActivity.this);
		final ListView lvOrderItem = (ListView) findViewById(R.id.lvOrderItem);
		
		lvOrderItem.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				final SalesOrderItem item = (SalesOrderItem) parent.getItemAtPosition(position);
				((OrderSwipeListView) lvOrderItem).closeOpenRow();
				new AlertDialog.Builder(SalesOrderActivity.this)
						.setTitle(item.getSpmch())
						.setItems(new String[] {"删除"}, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								deleteOrderItem(item);
							}
						})
						.show();
				return true;
			}
		});

		//结算按钮
		Button btnFinish = (Button) findViewById(R.id.btnFinish);
		Button btnBack = (Button) findViewById(R.id.btnBack);
		btnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		btnFinish.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(salesOrder != null){
					EditText edtCustomer = (EditText)findViewById(R.id.edtCustomer);
					salesOrder.setCustomer(edtCustomer.getText().toString().trim());
					salesOrderService.finishOrder(salesOrder);
					app.setCurrSalesOrder(null);
					salesOrder = app.getCurrSalesOrder();
					displaySalesOrder();
					setReturnResult();
				}else{
					Toast.makeText(SalesOrderActivity.this, "无单据可以结算", Toast.LENGTH_LONG).show();
				}

			}
		});
		
		displaySalesOrder();
	}

	public void deleteOrderItem(SalesOrderItem item) {
		if (salesOrder == null || item == null) {
			return;
		}
		salesOrder.deleteItem(item);
		salesOrderService.saveOrUpdate(salesOrder);
		OrderItemListAdaper adapter = (OrderItemListAdaper) ((ListView) findViewById(R.id.lvOrderItem)).getAdapter();
		if (adapter != null) {
			adapter.setOpenPosition(-1);
		}
		setReturnResult();
		refreshSalesOrder();
	}

	public void editOrderItemQuantity(int position) {
		if (salesOrder == null || salesOrder.getItems() == null
				|| position < 0 || position >= salesOrder.getItems().size()) {
			return;
		}
		SalesOrderItem item = salesOrder.getItems().get(position);
		boolean chineseMedicine = item.getSpbh() != null && SpkfkService.isSelfCnSp(item.getSpbh());
		String unit = chineseMedicine ? "g" : item.getDw();
		Intent intent = new Intent(this, NumberInputActivity.class);
		intent.putExtra(NumberInputActivity.TITLE,
				unit == null || unit.length() == 0 ? "修改数量" : "修改数量（" + unit + "）");
		intent.putExtra(NumberInputActivity.NUMBER,
				BigDecimal.valueOf(item.getShl()).stripTrailingZeros().toPlainString());
		intent.putExtra(NumberInputActivity.TAG, String.valueOf(position));
		intent.putExtra(NumberInputActivity.DECIMAL_COUT, 2);
		startActivityForResult(intent, REQUEST_CODE_EDIT_QUANTITY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQUEST_CODE_EDIT_QUANTITY ||
				resultCode != NumberInputActivity.RESULT_CODE_GET_INPUT || data == null || salesOrder == null) {
			return;
		}
		try {
			int position = Integer.parseInt(data.getStringExtra(NumberInputActivity.TAG));
			if (position < 0 || position >= salesOrder.getItems().size()) {
				return;
			}
			String rawQuantity = data.getStringExtra(NumberInputActivity.NUMBER);
			if (rawQuantity == null || rawQuantity.length() == 0) {
				Toast.makeText(this, "请输入有效数量", Toast.LENGTH_SHORT).show();
				return;
			}
			BigDecimal quantity = new BigDecimal(rawQuantity);
			double value = quantity.doubleValue();
			if (quantity.compareTo(BigDecimal.ZERO) <= 0 || quantity.stripTrailingZeros().scale() > 2 ||
					Double.isInfinite(value)) {
				Toast.makeText(this, "请输入大于0、最多两位小数的数量", Toast.LENGTH_SHORT).show();
				return;
			}
			SalesOrderItem item = salesOrder.getItems().get(position);
			Double oldQuantity = item.getShl();
			item.setShl(value);
			EditText edtCustomer = (EditText) findViewById(R.id.edtCustomer);
			salesOrder.setCustomer(edtCustomer.getText().toString().trim());
			Response result = salesOrderService.saveOrUpdate(salesOrder);
			if (!result.isOk()) {
				item.setShl(oldQuantity);
				Toast.makeText(this, "数量保存失败", Toast.LENGTH_SHORT).show();
				return;
			}
			setReturnResult();
			refreshSalesOrder();
		} catch (NumberFormatException e) {
			Toast.makeText(this, "请输入有效数量", Toast.LENGTH_SHORT).show();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sales_order, menu);
		return true;
	}

	private void displaySalesOrder() {
		ListView lvOrderItem = (ListView) findViewById(R.id.lvOrderItem);
		List<SalesOrderItem> items = new ArrayList<SalesOrderItem>();
		if (this.salesOrder != null) {
			items = this.salesOrder.getItems();
		}
		OrderItemListAdaper adapter = new OrderItemListAdaper(
				SalesOrderActivity.this, items);
		lvOrderItem.setAdapter(adapter);
		showOrderInfo();
/*		if (this.salesOrder != null) {
			List<SalesOrderItem> items = this.salesOrder.getItems();
			ListView lvOrderItem = (ListView) findViewById(R.id.lvOrderItem);
			OrderItemListAdaper adapter = new OrderItemListAdaper(
					SalesOrderActivity.this, items);
			lvOrderItem.setAdapter(adapter);
			showOrderInfo();
		}*/
	}

	private void refreshSalesOrder() {
		ListView lvOrderItem = (ListView) findViewById(R.id.lvOrderItem);
		OrderItemListAdaper lvAdapter = ((OrderItemListAdaper) lvOrderItem
				.getAdapter());
		lvAdapter.notifyDataSetChanged();
		showOrderInfo();
	}

	/**
	 * 显示单据主信息
	 * 
	 * @author caijun 2014-1-6
	 */
	private void showOrderInfo() {
		int count = 0;
		Double amount = 0.0;

		TextView tvOrderNo = (TextView) findViewById(R.id.tvOrderNo);
		EditText edtCustomer = (EditText) findViewById(R.id.edtCustomer);
		TextView tvAmount = (TextView) findViewById(R.id.tvAmount);
		TextView tvCount = (TextView) findViewById(R.id.tvCount);

		
		if (this.salesOrder != null) {
			tvOrderNo.setText(this.salesOrder.getOrderNo());
			edtCustomer.setText(this.salesOrder.getCustomer());
			count = this.salesOrder.getItems().size();
			amount = SalesOrderService.getOrderAmount(this.salesOrder);
		}
		tvCount.setText(String.valueOf(count));
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(3);
		tvAmount.setText(nf.format(amount));//显示时保留3位小数
	}

	private void setReturnResult() {
		// 设置返回商品列表的结果
		Intent resultIntent = new Intent();
		setResult(Activity.RESULT_OK, resultIntent);
	}
	
}
