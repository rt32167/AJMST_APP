package com.ajmst.android.ui.spkfk;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.List;

import com.ajmst.android.R;
import com.ajmst.android.entity.AdvSpkfk;
import com.ajmst.android.entity.SalesOrder;
import com.ajmst.android.entity.SalesOrderItem;
import com.ajmst.android.service.SpkfkService;
import com.ajmst.android.ui.NumberInputActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SpkfkSelectListAdaper extends BaseAdapter{
	private Activity activity;
	private LayoutInflater inflater;
	private List<AdvSpkfk> spkfks;
	private SalesOrder salesOrder;
	private Hashtable<String,BigDecimal> quantityInOrder;
	
	public SpkfkSelectListAdaper(Activity activity,List<AdvSpkfk> spkfks) {
		super();
		this.activity = activity;
		this.spkfks = spkfks;
		this.inflater = LayoutInflater.from(activity);
		quantityInOrder = new Hashtable<String,BigDecimal>();
	}
	

	public List<AdvSpkfk> getSpkfks() {
		return spkfks;
	}


	public void setSpkfks(List<AdvSpkfk> spkfks) {
		this.spkfks = spkfks;
	}
	
	public void setSpkfk(AdvSpkfk sp){
		for(int i =0; i < getSpkfks().size(); i++){
			AdvSpkfk spTmp = this.spkfks.get(i);
			if(sp.getSpid().equals(spTmp.getSpid())){
				this.spkfks.set(i, sp);
				break;
			}
		}
	}


	@Override
	public int getCount() {
		return spkfks.size();
	}

	@Override
	public Object getItem(int position) {
		return spkfks.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final AdvSpkfk spkfk = spkfks.get(position);
		convertView = inflater.inflate(R.layout.spkfk_list_item, null);
		TextView textViewSeq = (TextView)convertView.findViewById(R.id.textViewSeq);
		textViewSeq.setText("" + (position + 1));
		TextView tvSpmch = (TextView)convertView.findViewById(R.id.tvSpmch);
		tvSpmch.setText(spkfk.getSpmch());
		TextView tvCabinetNo = (TextView)convertView.findViewById(R.id.tvCabinetNo);
		String cabinetNo = spkfk.getGh();
		boolean hasCabinet = cabinetNo != null && cabinetNo.length() > 0;
		tvCabinetNo.setText(hasCabinet ? cabinetNo : "");
		tvCabinetNo.setVisibility(hasCabinet ? View.VISIBLE : View.GONE);
		convertView.findViewById(R.id.textViewCabinetNoSuffix).setVisibility(hasCabinet ? View.VISIBLE : View.GONE);
		TextView tvLshj = (TextView)convertView.findViewById(R.id.tvLshj);
		tvLshj.setText("¥" + spkfk.getLshj().toString());
		convertView.findViewById(R.id.tvPriceUnit).setVisibility(
				spkfk.getSpbh() != null && SpkfkService.isSelfCnSp(spkfk.getSpbh()) ? View.VISIBLE : View.GONE);
		
		Button btnAddToOrder = (Button)convertView.findViewById(R.id.btnAddToOrder);
		//添加到销售单
		btnAddToOrder.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SpkfkSelectActivity spkfkSelectActivity = (SpkfkSelectActivity)activity;
				spkfkSelectActivity.addToOrderBtnClick(position);
/*				//启动商品详情activity
				Intent intent = new Intent(inflater.getContext(), SpkfkDetailActivity.class);
				intent.putExtra(SpkfkDetailActivity.EXTRA_NAME_SPKFK, spkfk);
				intent.putExtra(NumberInputActivity.TAG, "" + position);
				activity.startActivityForResult(intent, SpkfkSelectActivity.REQUEST_CODE_SPKFK_DETAIL);*/
			}
		});
		convertView.setTag(spkfk);
		
		BigDecimal quantity = spkfk.getSpid() == null ? null : quantityInOrder.get(spkfk.getSpid());
		boolean inOrder = quantity != null;
		TextView tvAddedQuantity = (TextView)convertView.findViewById(R.id.tvAddedQuantity);
		if (inOrder) {
			String unit = spkfk.getSpbh() != null && SpkfkService.isSelfCnSp(spkfk.getSpbh()) ? "g" : spkfk.getDw();
			if (unit == null) {
				unit = "";
			}
			tvAddedQuantity.setText("已加" + quantity.stripTrailingZeros().toPlainString() + unit);
			tvAddedQuantity.setVisibility(View.VISIBLE);
		} else {
			tvAddedQuantity.setVisibility(View.GONE);
		}
		View card = convertView.findViewById(R.id.cardContainer);
		card.setBackgroundResource(inOrder ? R.drawable.ui_card_selected : R.drawable.ui_card);
		btnAddToOrder.setBackgroundResource(inOrder ? R.drawable.ui_edit_button : R.drawable.ui_primary_button);
		btnAddToOrder.setTextColor(activity.getResources().getColor(inOrder ? R.color.ui_accent : R.color.ui_surface));
		btnAddToOrder.setText(inOrder ? "✎" : "+");
		btnAddToOrder.setContentDescription(inOrder ? "修改销售单数量" : "加入销售单");
		
		
		return convertView;
	}


	public SalesOrder getSalesOrder() {
		return salesOrder;
	}


	public void setSalesOrder(SalesOrder salesOrder) {
		this.salesOrder = salesOrder;
		quantityInOrder.clear();
		if(this.salesOrder != null && salesOrder.getItems() != null){
			for(int i = 0; i < salesOrder.getItems().size();i++){
				SalesOrderItem item = salesOrder.getItems().get(i);
				if (item.getSpid() != null && item.getShl() != null) {
					BigDecimal quantity = BigDecimal.valueOf(item.getShl());
					BigDecimal previous = quantityInOrder.get(item.getSpid());
					quantityInOrder.put(item.getSpid(), previous == null ? quantity : previous.add(quantity));
				}
			}
		}
		this.notifyDataSetChanged();
	}
	

}
