
package novoda.wallpaper.flickr;

import java.net.ConnectException;

import com.nullwire.trace.ExceptionHandler;

import novoda.net.ErrorReporter;
import novoda.net.FlickrApi;
import novoda.net.GeoNamesAPI;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

/*
 * ===================================
 * Flickr Live Wallpaper 
 * http://github.com/novoda/flickrlivewallpaper 
 * ===================================
 *  
 * Retrieves and displays a photo from Flickr based on your current location.
 * The majority of locations in the world do not have photos specifically 
 * geoTagged on Flickr and so instead a query using the users exact location 
 * is sent to GeoNames establish a good approximation and then queries 
 * Flickr using the place name as a tag.
 * 
 * This code was developed by Novoda (http://www.novoda.com)
 * You are welcome to use this code in however you see fit.
 *
 */
public class FlickrLiveWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new FlickrEngine();
    }

    class FlickrEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            final Display dm = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            ExceptionHandler.register(getApplicationContext());
            
            mPrefs = FlickrLiveWallpaper.this.getSharedPreferences(Constants.Prefs.NAME, MODE_PRIVATE);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);

            if (!mPrefs.contains(Constants.Prefs.TAP_TYPE)) {
                Editor edit = mPrefs.edit();
                edit.putString(Constants.Prefs.TAP_TYPE, Constants.Prefs.TAP_TYPE_VISIT);
                edit.commit();
            }

            if (!mPrefs.contains(Constants.Prefs.DISPLAY)) {
                Editor edit = mPrefs.edit();
                edit.putString(Constants.Prefs.DISPLAY, Constants.Prefs.DISPLAY_FRAME);
                edit.commit();
            }

            DISPLAY_WIDTH = dm.getWidth();
            DISPLAY_HEIGHT = dm.getHeight();
            DISPLAY_X_CENTER = DISPLAY_WIDTH * 0.5f;

            createPainters();
        }

        @Override
        public void onDestroy() {
//            if (cachedBitmap != null) {
//                cachedBitmap.recycle();
//            }
//            cachedBitmap = null;
            mHandler.removeCallbacks(mDrawWallpaper);
            super.onDestroy();
        }
        
        /*
         * A new Wallpaper is requested every time the dashboard becomes visible
         * within a reasonable time period to save queries being made overly
         * often to save battery and bandwith.
         * @see
         * android.service.wallpaper.WallpaperService.Engine#onVisibilityChanged
         * (boolean)
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            boolean reSynchNeeded = (System.currentTimeMillis() - lastSync) > 1000 * 60 * 60;
            currentlyVisibile = visible;
            if (visible) {
                if (reSynchNeeded) {
                    mHandler.post(mDrawWallpaper);
                }
            } else {
                mHandler.removeCallbacks(mDrawWallpaper);
            }
        }

        @Override
        public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
                boolean resultRequested) {
            Intent intent = null;
            Log.i(TAG, "An action going on" + action);
            if (action.equals(WallpaperManager.COMMAND_TAP)) {

                String tappingOpt = mPrefs.getString(Constants.Prefs.TAP_TYPE, Constants.Prefs.TAP_TYPE_VISIT);

                if (tappingOpt.equals(Constants.Prefs.TAP_TYPE_REFRESH) || errorShown) {
                    errorShown = false;
                    mHandler.post(mDrawWallpaper);
                } else {
                    
                    try{
                        Log.i(TAG, "Browsing to image=[" + imgUrl + "]");
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imgUrl));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }catch(NullPointerException e){
                        Log.e(TAG, "Flickr Image URL was null", e);
                    }
                }
            }

            return super.onCommand(action, x, y, z, extras, resultRequested);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key != null) {
                Log.i(TAG, "Shared Preferences changed: " + key);
                mHandler.post(mDrawWallpaper);
            }
        }

        private Bitmap requestImage(final Location location, final String placeName)
                throws IllegalStateException {
            final boolean FRAMED = mPrefs.getString(Constants.Prefs.DISPLAY, Constants.Prefs.DISPLAY_FRAME)
            .equals(Constants.Prefs.DISPLAY_FRAME);

            final Pair<Bitmap, String> flickrResult = flickrApi.retrievePhoto(FRAMED, location,
                    placeName);

//            cachedBitmap = flickrResult.first;
            imgUrl = flickrResult.second;

            final Bitmap img = flickrResult.first;
            
            if (img == null) {
                Log.e(TAG, "I'm not sure what went wrong but image could not be retrieved");
                throw new IllegalStateException(
                        "Whoops! We had problems retrieving an image. Please try again.");
            }
            
            return scaleImage(img, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        }

        private void drawFramedPortrait(final Bitmap img) {
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null && img != null) {
                    Log.i(TAG, "Drawing a Framed Portrait image");
                    c.drawPaint(bgPaint);
                    frame = BitmapFactory.decodeResource(getResources(),
                            R.drawable.bg_frame_portrait);
                    c.drawBitmap(frame, Constants.Frame.PORTRAIT_MARGIN_LEFT, Constants.Frame.PORTRAIT_MARGIN_TOP,
                            new Paint());
                    c.drawBitmap(img, Constants.Frame.PORTRAIT_IMG_MARGIN_LEFT, Constants.Frame.PORTRAIT_IMG_MARGIN_TOP,
                            txtPaint);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        private void drawFramedLandscape(final Bitmap img) {
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null && img != null) {
                    Log.i(TAG, "Drawing a Framed Landscape image");

                    c.drawPaint(bgPaint);
                    frame = BitmapFactory.decodeResource(getResources(),
                            R.drawable.bg_frame_landscape);
                    c.drawBitmap(frame, Constants.Frame.LANDSCAPE_MARGIN_LEFT, Constants.Frame.LANDSCAPE_MARGIN_TOP,
                            new Paint());
                    c.drawBitmap(img, Constants.Frame.LANDSCAPE_IMG_MARGIN_LEFT, Constants.Frame.LANDSCAPE_IMG_MARGIN_TOP,
                            txtPaint);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        /**
         * Scale images to fit the height/width of the drawing canvas.
         * 
         * @param bitmap
         * @param width
         * @param height
         * @return
         */
        private Bitmap scaleImage(final Bitmap bitmap,final int width,final int height) {
            final int bitmapWidth = bitmap.getWidth();
            final int bitmapHeight = bitmap.getHeight();
            final int scaledWidth;
            final int scaledHeight;

            final boolean FRAMED = mPrefs.getString(Constants.Prefs.DISPLAY, Constants.Prefs.DISPLAY_FRAME)
            .equals(Constants.Prefs.DISPLAY_FRAME);

            if (FRAMED) {

                if (bitmapWidth > bitmapHeight) {
                    scaledWidth = 343;
                    scaledHeight = 271;
                } else {
                    scaledWidth = 295;
                    scaledHeight = 372;
                }

            } else {
                double scaledY = 1, scaledX = 1;
                scaledX = width / bitmapWidth;
                scaledY = height / bitmapHeight;
                scaledX = Math.min(scaledX, scaledY);
                scaledY = scaledX;

                scaledWidth = (int)(bitmapWidth * scaledX);
                scaledHeight = (int)(bitmapHeight * scaledY);
            }

            Log.d(TAG, "Scaling Bitmap (height x width): Orginal[" + bitmapHeight + "x"
                    + bitmapWidth + "], New[" + scaledHeight + "x" + scaledWidth + "]");

            return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        }

        private void drawFramePreview() {
            float x = DISPLAY_X_CENTER;
            float y = 180;
        
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                c.drawPaint(bgPaint);
                final Bitmap fullScreenIcon = BitmapFactory.decodeResource(getResources(),
                        R.drawable.preview_frame);
                if (c != null) {
                    c.drawBitmap(fullScreenIcon, (x - fullScreenIcon.getWidth() * 0.5f), y,
                            txtPaint);
                }
            } catch(NullPointerException e){
                Log.e(TAG, "Could not draw frame preview", e);
            }finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        /*
         * Present information designed to inform the user about a behavior
         * which is not erroneous.
         */
        private void drawFullscreenPreview(final String string) {
            Log.i(TAG, string);
            float x = DISPLAY_X_CENTER;
            float y = 180;
        
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                c.drawPaint(bgPaint);
                final Bitmap fullScreenIcon = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_fullscreen);
                if (c != null) {
                    drawTextInRect(c, txtPaint, new Rect((int)x, (int)y, 700, 300), string);
                    c.drawBitmap(fullScreenIcon, (x - fullScreenIcon.getWidth() * 0.5f), y + 208,
                            txtPaint);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        /*
         * Initial loading feedback
         */
        private void drawNotificationLoading() {
            Log.d(TAG, "Displaying loading info");
            final float x = DISPLAY_X_CENTER;
            final float y = 180;
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                c.drawPaint(bgPaint);
                final Bitmap decodeResource = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_logo_flickr);
                if (c != null) {
                    c.drawBitmap(decodeResource, (x - decodeResource.getWidth() * 0.5f), y,
                            txtPaint);
                    c.drawText("Finding your location", x, y + 108, txtPaint);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        /*
         * Loading feedback to assure the user a place has been correctly
         * retrieved. This feedback is intended to help alleviate some of the
         * lag in retrieving and then resizing the image but also informs the
         * user of their presumed location.
         */
        private void drawNotificationScaling(final String placeName) {
            Log.d(TAG, "Displaying loading details for placename=[" + placeName + "]");
            final float x = DISPLAY_X_CENTER;
            final float y = 180;
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                c.drawPaint(bgPaint);
                final Bitmap decodeResource = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_logo_flickr);
                if (c != null) {
                    c.drawBitmap(decodeResource, (x - decodeResource.getWidth() * 0.5f), y,
                            txtPaint);
                    c.drawText("Downloading Image", x, y + 108, txtPaint);
                    drawTextInRect(c, txtPaint, new Rect((int)x, (int)y + 200, 700, 300),
                            "Looking for images around " + placeName);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        /*
         * Provides error feedback for users Also clears the screen of any old
         * artifacts
         */
        private void drawNotificationError(final String error) {
            Log.e(TAG, error);
            float x = DISPLAY_X_CENTER;
            float y = 180;
            errorShown = true;
            createPainters();

            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                c.drawPaint(bgPaint);
                final Bitmap decodeResource = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_smile_sad_48);
                final Bitmap refreshIcon = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_refresh_48);
                if (c != null) {

                    c.drawBitmap(decodeResource, (x - decodeResource.getWidth() * 0.5f), y,
                            txtPaint);
                    drawTextInRect(c, txtPaint, new Rect((int)x, (int)y + 108, 700, 300), error);
                    c.drawBitmap(refreshIcon, (x - refreshIcon.getWidth() * 0.5f), 550, txtPaint);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }
        
        private void notifyDownloadingWallpaper() {
            NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            Intent intent = new Intent();
            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
            Notification notif = new Notification(R.drawable.ic_logo_flickr, getText(R.string.notification_action), System.currentTimeMillis());
            notif.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), getText(R.string.notification_action), contentIntent);
            nm.notify(R.string.app_name, notif);
        }
        
        private void cancelAnyNotifications() {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
            .cancel(R.string.app_name);
        }

        private void createPainters() {
            txtPaint = new Paint();
            txtPaint.setAntiAlias(true);
            txtPaint.setColor(Color.WHITE);
            txtPaint.setTextSize(37);
            txtPaint.setStyle(Paint.Style.STROKE);
            Typeface typeFace = Typeface.createFromAsset(getBaseContext().getAssets(),
                    "fonts/ArnoProRegular10pt.otf");
            txtPaint.setTypeface(typeFace);
            txtPaint.setTextAlign(Paint.Align.CENTER);

            final Bitmap bg = BitmapFactory.decodeResource(getResources(),
                    R.drawable.bg_wallpaper_pattern);

            BitmapShader mShader1 = new BitmapShader(bg, Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT);
            bgPaint = new Paint();
            bgPaint.setShader(mShader1);
//            bg.recycle();

        }

        /*
         * TODO: Possibility of better ways to wrap text using staticLayout
         */
        private void drawTextInRect(Canvas canvas, Paint paint, Rect r, CharSequence text) {

            // initial text range and starting position
            int start = 0;
            int end = text.length() - 1;
            float x = r.left;
            float y = r.top;
            int allowedWidth = r.width();

            if (allowedWidth < 30) {
                return; // too small
            }

            int lineHeight = paint.getFontMetricsInt(null);

            // For each line, with word wrap on whitespace.
            while (start < end) {
                final int charactersRemaining = end - start + 1;
                int charactersToRenderThisPass = charactersRemaining; // optimism!
                int extraSkip = 0;
                // This 'while' is nothing to be proud of.
                // This should probably be a binary search or more googling to
                // find "character index at distance N pixels in string"
                while (charactersToRenderThisPass > 0
                        && paint.measureText(text, start, start + charactersToRenderThisPass) > allowedWidth) {
                    charactersToRenderThisPass--;
                }

                // charactersToRenderThisPass would definitely fit, but could be
                // in the middle of a word
                int thisManyWouldDefinitelyFit = charactersToRenderThisPass;
                if (charactersToRenderThisPass < charactersRemaining) {
                    while (charactersToRenderThisPass > 0
                            && !Character.isWhitespace(text.charAt(start
                                    + charactersToRenderThisPass - 1))) {
                        charactersToRenderThisPass--;
                    }
                }

                // line breaks
                int i;
                for (i = 0; i < charactersToRenderThisPass; i++) {
                    if (text.charAt(start + i) == '\n') {
                        charactersToRenderThisPass = i;
                        extraSkip = 1;
                        break;
                    }
                }

                if (charactersToRenderThisPass < 1 && (extraSkip == 0)) {
                    // no spaces found, must be a really long word.
                    // Panic and show as much as would fit, breaking the word in
                    // the middle
                    charactersToRenderThisPass = thisManyWouldDefinitelyFit;
                }

                // Emit this line of characters and advance our offsets for the
                // next line
                if (charactersToRenderThisPass > 0) {
                    canvas.drawText(text, start, start + charactersToRenderThisPass, x, y, paint);
                }
                start += charactersToRenderThisPass + extraSkip;
                y += lineHeight;

                // start had better advance each time through the while, or
                // we've invented an infinite loop
                if ((charactersToRenderThisPass + extraSkip) < 1) {
                    return;
                }
            }
        }

        /*
         * Main thread of re-execution. Once called, an image will be retrieved
         * and then then drawn. This thread will wait until the canvas is
         * visible for when a a dialog or preference screen is shown. The loop
         * is defensive to ensure requests aren't queued.
         */
        private final Runnable mDrawWallpaper = new Runnable() {
            public void run() {
                if (!drawingWallpaper) {
                    if (currentlyVisibile) {
                        drawingWallpaper = true;
                        if (isPreview()) {
                            drawPreview();
                            drawingWallpaper = false;
                        } else {
                            notifyDownloadingWallpaper();
                            (new Thread() {
                                public void run() {
                                    retrieveNewImage();
                                    drawingWallpaper = false;
                                    cancelAnyNotifications();
                                }
                            }).start();
                        }
                    } else {
                        Log.w(TAG, "Queuing a draw request");
                        mHandler.postDelayed(mDrawWallpaper, 600);
                    }
                }
            }

            private void drawPreview() {
                final boolean FRAMED = mPrefs.getString(Constants.Prefs.DISPLAY, Constants.Prefs.DISPLAY_FRAME)
                .equals(Constants.Prefs.DISPLAY_FRAME);
                if (FRAMED) {
                    drawFramePreview();
                } else {
                    drawFullscreenPreview("Stretching images across dashboards");
                }
            }

            private void retrieveNewImage() {
                Log.i(TAG, "Request to refresh Wallpaper");
                drawNotificationLoading();
                try {
                    final LocationManager locManager = (LocationManager)FlickrLiveWallpaper.this
                    .getBaseContext().getSystemService(Context.LOCATION_SERVICE);
                    location = geoNamesAPI.obtainLocation(locManager);
                } catch (ConnectException e) {
                    location = null;
                    drawNotificationError("Could not connect to the internet to find your location");
                }

                if (location != null) {
                    requestAndDrawImage();
                    lastSync = System.currentTimeMillis();
                }
                Log.i(TAG, "Finished Drawing Wallpaper");
            }

            private void requestAndDrawImage() {
                drawNotificationScaling(location.second);
                final boolean FRAMED = mPrefs.getString(Constants.Prefs.DISPLAY, Constants.Prefs.DISPLAY_FRAME)
                        .equals(Constants.Prefs.DISPLAY_FRAME);
                final Bitmap image;
                try {
                    image = requestImage(location.first, location.second);
                    if (FRAMED) {
                        if (image.getWidth() > image.getHeight()) {
                            drawFramedLandscape(image);
                        } else {
                            drawFramedPortrait(image);
                        }
                    } else {
                        drawFullscreenPreview("Stretching images across dashboards");

                        try {
                            setWallpaper(image);
                        } catch (Exception e) {
                            Log.e(TAG, "Coudn't set wallpaper", e);
                            drawNotificationError("Problems trying to display your Photo");
                        }
                    }

                } catch (IllegalStateException e) {
                    Log.e(TAG, e.getMessage());
                    drawNotificationError("Connection problems when trying to download your photos.");
                }
            }
        };

        private final Handler mHandler = new Handler();

        private SharedPreferences mPrefs;

        private String imgUrl;

        private long lastSync = 0;

        private boolean currentlyVisibile = false;

        private Paint txtPaint;

        private GeoNamesAPI geoNamesAPI = new GeoNamesAPI();

        private final FlickrApi flickrApi = new FlickrApi();

        private Pair<Location, String> location;

        private Paint bgPaint;

        private Bitmap frame;

        private boolean errorShown = false;

    }
    
    private static float DISPLAY_X_CENTER;

    private static int DISPLAY_WIDTH;
    
    private static int DISPLAY_HEIGHT;
    
    private static boolean drawingWallpaper = false;
    
    public static final String TAG = FlickrLiveWallpaper.class.getSimpleName();

}
