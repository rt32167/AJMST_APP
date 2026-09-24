package com.ajmst.android.salesorder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;

import com.ajmst.android.R;
import com.ajmst.android.entity.SalesOrderItem;
import com.ajmst.android.service.SalesOrderService;
import com.ajmst.android.service.SpkfkService;

import android.app.Activity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

public class OrderItemListAdaper extends BaseAdapter{
	private Activity activity;
	private LayoutInflater inflater;
	private List<SalesOrderItem> orderItems;
	private int openPosition = -1;
	
	public OrderItemListAdaper(Activity activity,List<SalesOrderItem> orderItems) {
		super();
		this.activity = activity;
		this.orderItems = orderItems;
		this.inflater = LayoutInflater.from(activity);
	}
	

	public List<SalesOrderItem> getOrderItems() {
		return orderItems;
	}


	public void setOrderItems(List<SalesOrderItem> orderItems) {
		this.orderItems = orderItems;
	}

	public int getOpenPosition() {
		return openPosition;
	}

	public void setOpenPosition(int position) {
		if (openPosition != position) {
			openPosition = position;
			notifyDataSetChanged();
		}
	}


	@Override
	public int getCount() {
		return orderItems.size();
	}

	@Override
	public Object getItem(int position) {
		return orderItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final SalesOrderItem orderItem = orderItems.get(position);
		convertView = inflater.inflate(R.layout.sales_order_item, null);
		TextView textViewSeq = (TextView)convertView.findViewById(R.id.textViewSeq);
		textViewSeq.setText("" + (position + 1));
		TextView tvSpmch = (TextView)convertView.findViewById(R.id.tvSpmch);
		TextView tvShpgg = (TextView)convertView.findViewById(R.id.tvShpgg);
		tvShpgg.setText(orderItem.getShpgg());
		tvShpgg.setVisibility(TextUtils.isEmpty(orderItem.getShpgg()) ? View.GONE : View.VISIBLE);
/*		TextView tvCabinetNo = (TextView)convertView.findViewById(R.id.tvCabinetNo);
		tvCabinetNo.setText(spkfk.getGh());*/
		TextView tvLshj = (TextView)convertView.findViewById(R.id.tvLshj);
		boolean isChineseMedicine = orderItem.getSpbh() != null && SpkfkService.isSelfCnSp(orderItem.getSpbh());
		((TextView) convertView.findViewById(R.id.tvPriceUnit)).setText(isChineseMedicine ? "每10g" : "单价");
		tvLshj.setText("¥" + orderItem.getLshj().toString());
		NumberFormat amountFormat = NumberFormat.getNumberInstance();
		amountFormat.setMaximumFractionDigits(3);
		TextView tvItemAmount = (TextView)convertView.findViewById(R.id.tvItemAmount);
		tvItemAmount.setText("¥" + amountFormat.format(SalesOrderService.getItemAmount(orderItem)));
		
		String quantity = BigDecimal.valueOf(orderItem.getShl()).stripTrailingZeros().toPlainString();
		String unit = isChineseMedicine ? "g" : orderItem.getDw();
		if (unit == null) {
			unit = "";
		}
		String name = orderItem.getSpmch() == null ? "" : orderItem.getSpmch();
		SpannableString label = new SpannableString(name + " " + quantity + unit);
		int quantityStart = name.length() + 1;
		label.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.ui_accent_dark)),
				quantityStart, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		label.setSpan(new RelativeSizeSpan(0.75f),
				quantityStart, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		tvSpmch.setText(label);
		tvSpmch.setContentDescription(name + "，数量" + quantity + unit);
		Button editButton = (Button) convertView.findViewById(R.id.btnEditQuantity);
		editButton.setContentDescription("修改" + name + "的数量，当前" + quantity + unit);
		editButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((SalesOrderActivity) activity).editOrderItemQuantity(position);
			}
		});
		
		convertView.setTag(orderItem);
		View foreground = convertView.findViewById(R.id.orderRowForeground);
		boolean isOpen = position == openPosition;
		foreground.setTranslationX(isOpen ? -52 * activity.getResources().getDisplayMetrics().density : 0);
		Button deleteButton = (Button) convertView.findViewById(R.id.btnDeleteItem);
		deleteButton.setVisibility(isOpen ? View.VISIBLE : View.INVISIBLE);
		deleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((SalesOrderActivity) activity).deleteOrderItem(orderItem);
			}
		});
		return convertView;
	}

}
