package com.starnet.snview.component.liveview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.starnet.snview.global.Constants;
import com.starnet.snview.global.GlobalApplication;
import com.starnet.snview.images.LocalFileUtils;
import com.starnet.snview.util.BitmapUtils;
import com.starnet.snview.util.SDCardUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PlaybackLiveView extends SurfaceView implements OnLiveViewChangedListener {
	public static final String TAG = "LiveView";
	
	private PlaybackLiveViewItemContainer parent;
	private SurfaceHolder mHolder = null;
	
	private int width = 352;
	private int height = 288;
	
	private byte [] mPixel = new byte[width * height * 2];
    private ByteBuffer mBuffer;
	private Bitmap mVideoBit;  
	
	private Matrix mScale;
	
	private boolean canTakePicture = false;
	private boolean canStartRecord = false;
	private boolean canTakeVideoSnapshot = false;
//	private boolean canVideoUpdate = false;
	private String mVideoSnapshotName;
	
	private Paint mPaint = new Paint();
	
	private int mOldWidthMeasureSpec;
	private int mOldHeightMeasureSpec;
	
	
	public PlaybackLiveView(Context context) {
		super(context);
		init(width, height);
	}

	public PlaybackLiveView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(width, height);
	}

	public PlaybackLiveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(width, height);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		contentUpdate();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		setBackgroundColor(Color.TRANSPARENT);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}
	
	public void init(int w, int h) {
		if ( mHolder == null) {
			mHolder = getHolder();
			mHolder.addCallback(this);
		}
		
		if (mPixel != null) {
			mPixel = null;
			mPixel = new byte[w * h * 2];
			width = w;
			height = h;
		}
		
		mBuffer = null;
		
		if (mVideoBit != null) {
			mVideoBit.recycle();
			mVideoBit = null;
		}
		

		mBuffer = ByteBuffer.wrap(mPixel);
		mVideoBit = Bitmap.createBitmap(w, h, Config.RGB_565);
		mPaint.setColor(Color.RED);
		// this.setScaleType(ImageView.ScaleType.FIT_XY);
		
		mScale = null;
		mScale = new Matrix();
		mScale.setScale(1.0F * getWidth() / mVideoBit.getWidth() , 1.0F * getHeight() / mVideoBit.getHeight());
		
		mOldWidthMeasureSpec = -1;
		mOldHeightMeasureSpec = -1;
		
//		canVideoUpdate = true;
	}
	
	public void setParent(PlaybackLiveViewItemContainer c) {
		this.parent = c;
	} 

	
	public void makeVideoSnapshot(String fileNameExpceptSuffix) {
		canTakeVideoSnapshot = true;
		mVideoSnapshotName = fileNameExpceptSuffix;
	}

	public void takePicture() {
		canTakePicture = true;
		contentUpdate();
	}
	
	public boolean isStartRecord() {
		return canStartRecord;
	}
	
	public void setStartRecord(boolean b) {
		this.canStartRecord = b;
	}
	
//	public void setVideoUpdate(boolean canVideoUpdate) {
//		this.canVideoUpdate = canVideoUpdate;
//	}
	
	public int[] getResolution() {
		int[] r = new int[2];
		
		r[0] = width;
		r[1] = height;
		
		return r;
	}

	public byte[] retrievetDisplayBuffer() { 
		return mPixel;
	}
	
	public void copyPixelsFromBuffer(byte[] srcBuf) {
		if (mPixel.length != srcBuf.length) {
			throw new IllegalStateException("The length of source buffer differs from destination buffer mPixel");
		}
		
		System.arraycopy(srcBuf, 0, mPixel, 0, mPixel.length);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		int width = MeasureSpec.getSize(widthMeasureSpec);
//		int height = MeasureSpec.getSize(heightMeasureSpec);
//		Log.d(TAG, "onMeasure(), measuredWidth:" + width + ", measuredHeight:" + height);
		
		if (mOldWidthMeasureSpec != widthMeasureSpec ||
				mOldHeightMeasureSpec != heightMeasureSpec) {
			mOldWidthMeasureSpec = widthMeasureSpec;
			mOldHeightMeasureSpec = heightMeasureSpec;
		}		 
		
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (mScale != null && mVideoBit != null) {
			mScale.setScale(1.0F * (right-left) / mVideoBit.getWidth(), 
					1.0F * (bottom-top) / mVideoBit.getHeight());
		}
		
		contentUpdate();  // 横竖屏切换时，图片先自动扩展，防止因网络原因部分Liveview未及时刷新
		
		super.onLayout(changed, left, top, right, bottom);
	}

	private synchronized void contentUpdate() {
		Canvas canvas = mHolder.lockCanvas();
		// 刷屏
		if (mVideoBit != null && canvas != null) {
//			if (!canVideoUpdate) {
//				return;
//			}
			
        	/* 此处rewind用意
         	 * 4.2中对copyPixelsFromBuffer( )执行的缓冲区进行了调整，每次拷贝结束后，将下次拷贝
        	 * 的起始位置置为前一次拷贝结束时的位置。这样，如果对同一个ByteBuffer执行多次连续拷贝，
        	 * 就要注意每次起始位置。
        	 */
        	mBuffer.rewind();	
        	if ((mVideoBit.getWidth() * mVideoBit.getHeight() * 2) 
        			!= (mBuffer.position() + mBuffer.remaining())) {
        		return;
        	}
        	
        	mVideoBit.copyPixelsFromBuffer(mBuffer);	
        	canvas.drawBitmap(mVideoBit, mScale, null);
        	
        	//Log.i(TAG, "refreshDisplay, width: " + getWidth() + ", height: " + getHeight());
        	
        	if (canTakePicture) {
        		savePictureAndThumbnail(mVideoBit);
        		canTakePicture = false;
        	}
        	if (canStartRecord) {
        		if (parent.canStartRecord()) {
        			canvas.drawCircle(20, 20, 10, mPaint);
        		}
        	}
        	if (canTakeVideoSnapshot && mVideoSnapshotName != null) {
        		saveVideoSnapshotAndThumbnail(mVideoBit, mVideoSnapshotName);
        		canTakeVideoSnapshot = false;
        	}
        	
        	mHolder.unlockCanvasAndPost(canvas);         	
        }
	}
	
	private int THUMBNAIL_HEIGHT = 100;
	private void saveVideoSnapshotAndThumbnail(Bitmap bmp, String fileName) {
		PlaybackLiveViewItemContainer c = parent;
		if (c == null) {
			return;
		}
		
		Log.i(TAG, "Has Sdcard: " + SDCardUtils.IS_MOUNTED);
		if (SDCardUtils.IS_MOUNTED) { // SDcard可用
			// 获取快照及其缩略图完整路径
			String fullImgPath = LocalFileUtils.getCaptureFileFullPath(fileName, true);
			String fullThumbImgPath = LocalFileUtils.getThumbnailsFileFullPath(fileName, true);;
			
			// 取得缩略图
			int thumbnailHeight = THUMBNAIL_HEIGHT;
			int thumbnailWidth = THUMBNAIL_HEIGHT * bmp.getWidth() / bmp.getHeight();
			Bitmap thumbnail = BitmapUtils.extractMiniThumb(bmp, thumbnailWidth, thumbnailHeight, false); 
			
			// 保存拍照截图
			if (saveBmpFile(bmp, fullImgPath)
					&& saveBmpFile(thumbnail, fullThumbImgPath)) {
				//result = true;
				Log.i(TAG, "Save pictures successfully !");
			}
		} else { // 不存在SDCard的情况（分有/无内置内存情况）
			
		}
	}
	
	private void savePictureAndThumbnail(Bitmap bmp) {
		PlaybackLiveViewItemContainer c = parent;
		if (c == null) {
			return;
		}

		boolean result = false;
		String imgPath =  null;
		
		Log.i(TAG, "Has Sdcard: " + SDCardUtils.IS_MOUNTED);
		
		if (SDCardUtils.IS_MOUNTED) { // SDcard可用
			// 获取拍照截图及其缩略图完整路径
			String fileName = LocalFileUtils.getFormatedFileName(c.getPlaybackItem()
					.getDeviceRecordName(), c.getPlaybackItem().getChannel());
			String fullImgPath = LocalFileUtils.getCaptureFileFullPath(fileName, true);
			String fullThumbImgPath = LocalFileUtils.getThumbnailsFileFullPath(fileName, true);;
			
			Log.i(TAG, "fileName: " + fileName);
			Log.i(TAG, "fileImgPath: " + fullImgPath);
			Log.i(TAG, "fullThumbImgPath: " + fullThumbImgPath);
			
			imgPath = fullImgPath;
			
			// 取得缩略图
			int thumbnailHeight = THUMBNAIL_HEIGHT;
			int thumbnailWidth = THUMBNAIL_HEIGHT * bmp.getWidth() / bmp.getHeight();
			Bitmap thumbnail = BitmapUtils.extractMiniThumb(bmp, thumbnailWidth, thumbnailHeight, false); 

			Log.i(TAG, "tW: " + thumbnailWidth + ", tH: " + thumbnailHeight);
			Log.i(TAG, "Bitmap thumbnail: " + thumbnail);
			
			// 保存拍照截图
			if (saveBmpFile(bmp, fullImgPath)
					&& saveBmpFile(thumbnail, fullThumbImgPath)) {
				result = true;
				Log.i(TAG, "Save pictures successfully !");
			}
		} else { // 不存在SDCard的情况（分有/无内置内存情况）
			
		}
		
		// 通知主界面
		Handler h = GlobalApplication.getInstance().getPlaybackHandler();
		if (h != null && result) {
			Bundle b = new Bundle();
			b.putString("PICTURE_FULL_PATH", imgPath);
			
			Message m = h.obtainMessage();
			m.what = Constants.TAKE_PICTURE;
			m.setData(b);
			m.sendToTarget();
			
			Log.i(TAG, "Send msg notification for TAKE_PICTURE");
			Log.i(TAG, "Image path: " + imgPath);
		}
		
	}
	
	
	private boolean saveBmpFile(Bitmap b, String fullImgPath) {
		File f = new File(fullImgPath);
		FileOutputStream fout =  null;
		
		try {
			fout =  new FileOutputStream(f);
			b.compress(Bitmap.CompressFormat.JPEG, 100, fout);
			fout.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public void onResulotionChanged(int width, int height) {
		if (this.width != width || this.height != height) {
			init(width, height);
		}
	}
	
	@Override
	public void onContentUpdated() {
		contentUpdate();
	}

	@Override
	public void onContentReset() {	
//		canVideoUpdate = false;
		Canvas canvas = mHolder.lockCanvas();
		if (canvas != null) {
			canvas.drawColor(Color.BLACK);
			mHolder.unlockCanvasAndPost(canvas); 
		}
	}
	
}
