package com.starnet.snview.component.liveview;

import com.starnet.snview.R;
import com.starnet.snview.protocol.Connection;
import com.starnet.snview.realplay.PreviewDeviceItem;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LiveViewItemContainer extends RelativeLayout {
	
	private static final String TAG = "LiveViewItemContainer";
	
	private String deviceRecordName;
	private PreviewDeviceItem previewItem;
	
	private WindowLinearLayout mWindowLayout;
	private FrameLayout mPlaywindowFrame;
	private LiveView mSurfaceView;
	private ProgressBar mProgressBar;
	private ImageView mRefresh;
	private TextView mWindowInfoText;
	private ImageView[] mPTZImageViewArray = new ImageView[8];
	
	private RelativeLayout mArrowAddFrame;
	private ImageView[] mAddFocalLengthArray = new ImageView[4];
	private RelativeLayout mArrowSubFrame;
	private ImageView[] mSubFocalLengthArray = new ImageView[4];
	


//	private OnLiveViewContainerClickListener mLvContainerClickListener;
	private OnRefreshButtonClickListener mRefreshButtonClickListener;
	
	private Connection mCurrentConnection;
	
	
	
	public LiveViewItemContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public LiveViewItemContainer(Context context) {
		super(context);
	}
	
	

	public void findSubViews() {
		mWindowLayout = (WindowLinearLayout) findViewById(R.id.liveview_surface_infotext_layout);
		mPlaywindowFrame = (FrameLayout) findViewById(R.id.liveview_playwindow_frame);
		mSurfaceView = (LiveView) findViewById(R.id.liveview_surfaceview);
		mProgressBar = (ProgressBar) findViewById(R.id.liveview_progressbar);
		mRefresh = (ImageView) findViewById(R.id.liveview_refresh_imageview);
		mWindowInfoText = (TextView) findViewById(R.id.liveview_liveinfo_textview);
		
		mPTZImageViewArray[0] = (ImageView) findViewById(R.id.arrow_left);
		mPTZImageViewArray[1] = (ImageView) findViewById(R.id.arrow_left_down);
		mPTZImageViewArray[2] = (ImageView) findViewById(R.id.arrow_down);
		mPTZImageViewArray[3] = (ImageView) findViewById(R.id.arrow_right_down);
		mPTZImageViewArray[4] = (ImageView) findViewById(R.id.arrow_right);
		mPTZImageViewArray[5] = (ImageView) findViewById(R.id.arrow_right_up);
		mPTZImageViewArray[6] = (ImageView) findViewById(R.id.arrow_up);
		mPTZImageViewArray[7] = (ImageView) findViewById(R.id.arrow_left_up);

		mArrowAddFrame = (RelativeLayout) findViewById(R.id.arrow_add_frame);
		mAddFocalLengthArray[0] = (ImageView) findViewById(R.id.arrow_add_left_down);
		mAddFocalLengthArray[1] = (ImageView) findViewById(R.id.arrow_add_right_down);
		mAddFocalLengthArray[2] = (ImageView) findViewById(R.id.arrow_add_right_up);
		mAddFocalLengthArray[3] = (ImageView) findViewById(R.id.arrow_add_left_up);
		
		mArrowSubFrame = (RelativeLayout) findViewById(R.id.arrow_sub_frame);
		mSubFocalLengthArray[0] = (ImageView) findViewById(R.id.arrow_sub_left_down);
		mSubFocalLengthArray[1] = (ImageView) findViewById(R.id.arrow_sub_right_down);
		mSubFocalLengthArray[2] = (ImageView) findViewById(R.id.arrow_sub_right_up);
		mSubFocalLengthArray[3] = (ImageView) findViewById(R.id.arrow_sub_left_up);
	}
	
	public void init() {
//		if (mLvContainerClickListener != null) {
//			this.setOnClickListener(mLvContainerClickListener);
//		}
		
		if (mRefreshButtonClickListener != null) {
			mRefresh.setOnClickListener(mRefreshButtonClickListener);
		}
		
		mWindowInfoText.setText(null);
		
	}	

	public Connection getCurrentConnection() {
		return mCurrentConnection;
	}
	
	public void setCurrentConnection(Connection conn) {
		this.mCurrentConnection = conn;
	}
	
	public String getDeviceRecordName() {
		return deviceRecordName;
	}
	
	public void setDeviceRecordName(String deviceRecordName) {
		this.deviceRecordName = deviceRecordName;
	}
	
	
	
//	public void setLiveViewContainerClickListener(
//			OnLiveViewContainerClickListener lvContainerClickListener) {
//		this.mLvContainerClickListener = lvContainerClickListener;
//	}
	
	
	public PreviewDeviceItem getPreviewItem() {
		return previewItem;
	}
	public void setPreviewItem(PreviewDeviceItem previewItem) {
		this.previewItem = previewItem;
	}
	public void setRefreshButtonClickListener(
			OnRefreshButtonClickListener refreshButtonClickListener) {
		this.mRefreshButtonClickListener = refreshButtonClickListener;
	}
	
	public WindowLinearLayout getWindowLayout() {
		return mWindowLayout;
	}
	
	public FrameLayout getPlaywindowFrame() {
		return mPlaywindowFrame;
	}
	
	public LiveView getSurfaceView() {
		return mSurfaceView;
	}
	
	public ProgressBar getProgressBar() {
		return mProgressBar;
	}
	
	public ImageView getRefreshImageView() {
		return mRefresh;
	}
	
	public TextView getWindowInfoText() {
		return mWindowInfoText;
	}
	
	public void showArrowAnimation(int showPos) {
		int i = 0;
		
		for (i = 0; i < mPTZImageViewArray.length; i++) {
			final ImageView img = mPTZImageViewArray[i];
			
			if (i == showPos) {
				img.setVisibility(View.VISIBLE);
				((AnimationDrawable)img.getBackground()).start();
				
//				// 1秒后停止
//				this.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//						img.setVisibility(View.INVISIBLE);
//						((AnimationDrawable)img.getBackground()).stop();
//					}
//				}, 1000);
				
			} else {
				img.setVisibility(View.INVISIBLE);
				((AnimationDrawable)img.getBackground()).stop();
			}
		}
	}
	
	public void stopArrowAnimation() {
		int i = 0;
		
		for (i = 0; i < mPTZImageViewArray.length; i++) {
			mPTZImageViewArray[i].setVisibility(View.INVISIBLE);
			((AnimationDrawable)mPTZImageViewArray[i].getBackground()).stop();
		}
	}
	
	
	
	public void showFocalLengthAnimation(boolean isAddFocalLength) {
		final ImageView[] imgArray = isAddFocalLength ? mAddFocalLengthArray : mSubFocalLengthArray;
		final boolean _isAddFocalLength = isAddFocalLength;
		int i = 0;
		
		Log.i(TAG, "imgArray.length: " + imgArray.length + "isAddFocalLength: " + isAddFocalLength);
		
		if (_isAddFocalLength){
			mArrowAddFrame.setVisibility(View.VISIBLE);
		} else {
			mArrowSubFrame.setVisibility(View.VISIBLE);
		}
		
		for (i = 0; i < imgArray.length; i++) {
			imgArray[i].setVisibility(View.VISIBLE);
			((AnimationDrawable)imgArray[i].getBackground()).start();
		}
		
		this.postDelayed(new Runnable() {
			@Override
			public void run() {
				int j = 0;
				for (j = 0; j < imgArray.length; j++) {
					imgArray[j].setVisibility(View.INVISIBLE);
					((AnimationDrawable)imgArray[j].getBackground()).stop();
				}
				
				if (_isAddFocalLength){
					mArrowAddFrame.setVisibility(View.GONE);
				} else {
					mArrowSubFrame.setVisibility(View.GONE);
				}
			}
			
		}, 500);
		
	}

	public void setWindowInfoContent(String info) {
		final StringBuffer s;
		
		if (deviceRecordName != null && info != null) {
			s = new StringBuffer(deviceRecordName);
			s.append("[");
			s.append(info);
			s.append("]");
		} else {
			s = new StringBuffer("");
		}
		
		mWindowInfoText.post(new Runnable() {
			@Override
			public void run() {
				mWindowInfoText.setText(s.toString());
			}
		});
	}
	
	public void resetView() {
		mSurfaceView.setValid(true);
		mProgressBar.setVisibility(View.INVISIBLE);
		mRefresh.setVisibility(View.GONE);
	}
	
	
//	public static interface OnLiveViewContainerClickListener extends View.OnClickListener {}
	public static interface OnRefreshButtonClickListener extends View.OnClickListener {}
}
