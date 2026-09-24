package com.ajmst.android.application;

import com.ajmst.android.entity.SalesOrder;

import android.app.Activity;
import android.app.Application;
import android.app.TabActivity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;

public class AjmstApplication extends Application{
	private SalesOrder currSalesOrder;
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (Build.VERSION.SDK_INT >= 35) {
			registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
				@Override
				public void onActivityCreated(Activity activity, Bundle state) {
				// Its embedded activity owns the visible content and receives the insets.
				if (activity instanceof TabActivity) {
					return;
				}
				final View content = activity.findViewById(android.R.id.content);
					if (content == null) {
						return;
					}
					final int left = content.getPaddingLeft();
					final int top = content.getPaddingTop();
					final int right = content.getPaddingRight();
					final int bottom = content.getPaddingBottom();
					content.setOnApplyWindowInsetsListener((view, insets) -> {
						android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
						view.setPadding(left + bars.left, top + bars.top,
								right + bars.right, bottom + bars.bottom);
						return insets;
					});
					content.requestApplyInsets();
				}

				@Override public void onActivityStarted(Activity activity) { }
				@Override public void onActivityResumed(Activity activity) { }
				@Override public void onActivityPaused(Activity activity) { }
				@Override public void onActivityStopped(Activity activity) { }
				@Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
				@Override public void onActivityDestroyed(Activity activity) { }
			});
		}
	}
	public SalesOrder getCurrSalesOrder() {
		return currSalesOrder;
	}
	public void setCurrSalesOrder(SalesOrder currSalesOrder) {
		this.currSalesOrder = currSalesOrder;
	}

	
}
