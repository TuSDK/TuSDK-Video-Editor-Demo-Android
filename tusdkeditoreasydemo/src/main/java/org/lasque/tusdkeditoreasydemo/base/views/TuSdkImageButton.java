/** 
 * TuSDKCore
 * TuSdkImageButton.java
 *
 * @author 		Clear
 * @Date 		2014-11-29 下午8:39:03 
 * @Copyright 	(c) 2014 tusdk.com. All rights reserved.
 * 
 */
package org.lasque.tusdkeditoreasydemo.base.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import org.lasque.tusdkpulse.core.listener.TuSdkTouchColorChangeListener;
import org.lasque.tusdkpulse.core.view.TuSdkImageView;

/**
 * 图片按钮
 * 
 * @author Clear
 */
public class TuSdkImageButton extends TuSdkImageView
{

	public TuSdkImageButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public TuSdkImageButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public TuSdkImageButton(Context context)
	{
		super(context);
	}

	/**
	 * 颜色改变事件监听器
	 */
	private TuSdkTouchColorChangeListener colorChangeListener;

	/**
	 * 索引
	 */
	public int index;

	/**
	 * 初始化视图
	 */
	protected void initView()
	{

	}

	/**
	 * 绑定颜色变化
	 */
	protected void bindColorChangeListener()
	{
		if (this.colorChangeListener != null) return;
		this.colorChangeListener = TuSdkTouchColorChangeListener
				.bindTouchDark(this);
	}

	/**
	 * 删除颜色变化
	 */
	@SuppressLint("ClickableViewAccessibility")
	protected void removeColorChangeListener()
	{
		this.colorChangeListener = null;
		this.setOnTouchListener(null);
	}

	@Override
	public void setOnClickListener(OnClickListener l)
	{
		if (l != null)
		{
			this.bindColorChangeListener();
		}
		else
		{
			this.removeColorChangeListener();
		}
		super.setOnClickListener(l);
	}

	/**
	 * 设置状态
	 */
	@Override
	public void setEnabled(boolean enabled)
	{
		if (this.colorChangeListener != null && this.isEnabled() != enabled)
		{
			this.colorChangeListener.enabledChanged(this, enabled);
		}
		super.setEnabled(enabled);
	}

	/**
	 * 设置选中状态
	 */
	@Override
	public void setSelected(boolean selected)
	{
		if (this.colorChangeListener != null && this.isSelected() != selected)
		{
			this.colorChangeListener.selectedChanged(this, selected);
		}
		super.setSelected(selected);
	}
}
