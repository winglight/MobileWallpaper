package com.omdasoft.mobile.wallpaper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.webkit.WebSettings.TextSize;
import net.yihabits.mobile.wallpaper.R;

/*
 * This animated wallpaper draws a rotating wireframe cube.
 */
public class MyLiveWallpaper extends WallpaperService {

	private Bitmap bitmap;

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	@Override
	public void onCreate() {
		super.onCreate();

	}

	@Override
	public void onStart(Intent intent, int startId) {
		
		super.onStart(intent, startId);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		return new CubeEngine();
	}

	class CubeEngine extends Engine {

		private final Paint mPaint = new Paint();
		private final Paint tPaint = new Paint();

		CubeEngine() {
			// Create a Paint to draw the lines for our cube
			tPaint.setColor(Color.BLACK);
			tPaint.setTextSize(26);
			tPaint.setTextAlign(Align.CENTER);
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);

			// By default we don't get touch events, so enable them.
			// setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (visible) {
				drawFrame();
			} else {
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			// store the center of the surface, so we can draw the cube in the
			// right spot
			drawFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			drawFrame();
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here. This example draws a wireframe cube.
		 */
		void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				
				if (c != null) {
					
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(MyLiveWallpaper.this);
					// judge if jump to release notes
					String sureKey = getString(R.string.pref_wallpaper);
					String path = prefs.getString(sureKey, "");
					if (!"".equals(path)) {
						MyLiveWallpaper.this.bitmap = BitmapFactory
								.decodeFile(path);
					}
					c.save();
					if (bitmap == null) {
						c.drawColor(Color.WHITE);
						c.drawText(getString(R.string.setWallpaperFirstTip), c.getWidth()/2,
								c.getHeight()/2, tPaint);
					} else {
						int width = bitmap.getWidth();
						int height = bitmap.getHeight();
						int lefOffset = 0;
						int topOffset = 0;
						double ratio = (double)width / height;
						double cratio = (double)c.getWidth() / c.getHeight();
						if(ratio > cratio){
							height = c.getHeight();
							width = (int)(height*ratio);
							lefOffset = (int)((double)(c.getWidth() - width) / 2); 
						}else{
							width = c.getWidth();
							height = (int)(width/ratio);
							topOffset = (int)((double)(c.getHeight() - height) / 2); 
						}
						// draw something
						c.drawBitmap(Bitmap.createScaledBitmap(bitmap, width, height, false), lefOffset, topOffset, mPaint);
					}
					c.restore();
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

		}

	}
}
