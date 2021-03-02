package org.lasque.tusdkeditoreasydemo.base.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import org.lasque.tusdkpulse.core.listener.TuSdkTouchColorChangeListener;
import org.lasque.tusdkpulse.core.view.TuSdkViewInterface;

/**
 * 文字按钮
 * 
 * @author Clear
 */
public class TuSdkTextButton extends TextView implements TuSdkViewInterface
{

	public TuSdkTextButton(Context context)
	{
		super(context);
		this.initView();
	}

	public TuSdkTextButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.initView();
	}

	public TuSdkTextButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		this.initView();
	}

	/**
	 * 颜色改变事件监听器
	 */
	private TuSdkTouchColorChangeListener colorChangeListener;

	/**
	 * 索引
	 */
	public int index;

	// 选中状态颜色
	private int mSelectedColor = Integer.MAX_VALUE;
	// 默认颜色
	private int mDefaultColor = Integer.MAX_VALUE;

	/**
	 * 初始化视图
	 */
	protected void initView()
	{
		this.colorChangeListener = TuSdkTouchColorChangeListener
				.bindTouchDark(this);
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

		changeColor(selected ? mSelectedColor : mDefaultColor);
	}

	/**
	 * 改变文字与图标颜色
	 * 
	 * @param color
	 */
	public void changeColor(int color)
	{
		if (mSelectedColor == Integer.MAX_VALUE) return;
		this.setTextColor(color);
		if (color == mDefaultColor)
		{
			clearColorFilter(this.getCompoundDrawables());
		}
		else
		{
			setColorFilter(this.getCompoundDrawables(), color);
		}
	}

	/**
	 * 清楚颜色
	 * 
	 * @param compoundDrawables
	 */
	private void clearColorFilter(Drawable[] compoundDrawables)
	{
		for (Drawable drawable : compoundDrawables)
		{
			if (drawable != null)
			{
				drawable.clearColorFilter();
			}
		}
	}

	private void setColorFilter(Drawable[] compoundDrawables, int color)
	{
		for (Drawable drawable : compoundDrawables)
		{
			if (drawable != null)
			{
				drawable.clearColorFilter();
				 // SRC_IN 取两层绘制交集。显示上层。
				drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
			}
		}
	}

	/**
	 * 设置默认颜色
	 *
	 * @param color
	 */
	public void setDefaultColor(int color) {
		 mDefaultColor = color;
	}

	/**
	 * 设置选中颜色
	 * 
	 * @param color
	 */
	public void setSelectedColor(int color)
	{
		mSelectedColor = color;
	}

	/**
	 * 绑定视图
	 */
	@Override
	public void loadView()
	{
		// 默认颜色
		mDefaultColor = this.getTextColors().getDefaultColor();
	}

	/**
	 * 视图加载完成
	 */
	@Override
	public void viewDidLoad()
	{

	}

	/**
	 * 视图即将销毁
	 */
	@Override
	public void viewWillDestory()
	{

	}

	/**
	 * 视图需要重置
	 */
	@Override
	public void viewNeedRest()
	{

	}
}
