package robertapengelly.support.tileview.tile;

import  android.content.Context;
import  android.graphics.Bitmap;
import  android.graphics.Canvas;
import  android.graphics.Paint;
import  android.graphics.Rect;
import  android.support.annotation.StringDef;
import  android.view.animation.AnimationUtils;

import  java.lang.annotation.Retention;
import  java.lang.annotation.RetentionPolicy;
import  java.lang.ref.WeakReference;

import  robertapengelly.support.tileview.detail.DetailLevel;
import  robertapengelly.support.tileview.geom.FloatMathHelper;
import  robertapengelly.support.tileview.graphics.BitmapProvider;

public class Tile {

    private static final int DEFAULT_TRANSITION_DURATION = 200;
    
    public static final String STATE_DECODED        = "state_decoded";
    public static final String STATE_PENDING_DECODE = "state_pending_decode";
    public static final String STATE_UNASSIGNED     = "state_unassigned";
    
    private boolean mTransitionsEnabled;
    private float mDetailLevelScale, mProgress;
    private int mBottom, mColumn, mHeight, mLeft, mRight, mRow, mTop, mWidth;
    private int mTransitionDuration = DEFAULT_TRANSITION_DURATION;
    
    private Bitmap mBitmap;
    private DetailLevel mDetailLevel;
    private Object mData;
    private Long mRenderTimestamp;
    private Paint mPaint;
    
    private Rect mBaseRect = new Rect(),
                 mIntrinsicRect = new Rect(),
                 mRelativeRect = new Rect(),
                 mScaledRect = new Rect();
    
    private String mState = STATE_UNASSIGNED;
    private WeakReference<TileRenderRunnable> mTileRenderRunnableWeakReference;
    
    /** @hide **/
    @StringDef({STATE_DECODED, STATE_PENDING_DECODE, STATE_UNASSIGNED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}
    
    public Tile(int column, int row, int width, int height, Object data, DetailLevel detailLevel) {
    
        mColumn = column;
        mData = data;
        mDetailLevel = detailLevel;
        mDetailLevelScale = mDetailLevel.getScale();
        mHeight = height;
        mRow = row;
        mWidth = width;
        
        mLeft = (column * width);
        mTop = (row * height);
        
        mBottom = (mTop + height);
        mRight = (mLeft + width);
        
        updateRects();
    
    }
    
    public void computeProgress(){
    
        if (!mTransitionsEnabled)
            return;
        
        if (mRenderTimestamp == null) {
        
            mProgress = 0;
            mRenderTimestamp = AnimationUtils.currentAnimationTimeMillis();
            
            return;
        
        }
        
        double elapsed = (AnimationUtils.currentAnimationTimeMillis() - mRenderTimestamp);
        mProgress = (float) Math.min(1, (elapsed / mTransitionDuration));
        
        if (mProgress == 1f) {
        
            mRenderTimestamp = null;
            mTransitionsEnabled = false;
        
        }
    
    }
    
    /** @param canvas The canvas the tile's bitmap should be drawn into. */
    public void draw(Canvas canvas) {
    
        if ((mBitmap != null) && !mBitmap.isRecycled())
            canvas.drawBitmap(mBitmap, mIntrinsicRect, mRelativeRect, getPaint());
    
    }
    
    @Override
    public boolean equals(Object obj) {
    
        if (this == obj)
            return true;
        
        if (obj instanceof Tile) {
        
            Tile tile = (Tile) obj;
            return ((tile.getRow() == getRow()) && (tile.getColumn() == getColumn()) &&
                (tile.getDetailLevel().getScale() == getDetailLevel().getScale()));
        
        }
        
        return false;
    
    }
    
    public void execute(TileRenderPoolExecutor tileRenderPoolExecutor) {
    
        if (!mState.equals(STATE_UNASSIGNED))
            return;
        
        mState = STATE_PENDING_DECODE;
        
        TileRenderRunnable runnable = new TileRenderRunnable();
        mTileRenderRunnableWeakReference = new WeakReference<>(runnable);
        
        runnable.setTile(this);
        runnable.setTileRenderPoolExecutor(tileRenderPoolExecutor);
        
        tileRenderPoolExecutor.execute(runnable);
    
    }
    
    void generateBitmap(Context context, BitmapProvider provider) {
    
        if (mBitmap != null)
            return;
        
        mBitmap = provider.getBitmap(this, context);
        mHeight = mBitmap.getHeight();
        mWidth = mBitmap.getWidth();
        
        mBottom = (mTop + mHeight);
        mRight = (mLeft + mWidth);
        
        updateRects();
        mState = STATE_DECODED;
    
    }
    
    public Rect getBaseRect() {
        return mBaseRect;
    }
    
    public Bitmap getBitmap() {
        return mBitmap;
    }
    
    public int getColumn() {
        return mColumn;
    }
    
    public Object getData() {
        return mData;
    }
    
    public DetailLevel getDetailLevel() {
        return mDetailLevel;
    }
    
    public int getHeight() {
        return mHeight;
    }
    
    public boolean getIsDirty() {
        return (mTransitionsEnabled && (mProgress < 1f));
    }
    
    public int getLeft() {
        return mLeft;
    }
    
    public Paint getPaint() {
    
        if (!mTransitionsEnabled)
            return (mPaint = null);
        
        if (mPaint == null)
            mPaint = new Paint();
        
        mPaint.setAlpha((int) (255 * mProgress));
        return mPaint;
    
    }
    
    public Rect getRelativeRect() {
        return mRelativeRect;
    }
    
    public float getRendered() {
        return mProgress;
    }
    
    public int getRow() {
        return mRow;
    }
    
    public Rect getScaledRect(float scale) {
    
        mScaledRect.set((int) (mRelativeRect.left * scale), (int) (mRelativeRect.top * scale),
            (int) (mRelativeRect.right * scale), (int) (mRelativeRect.bottom * scale));
        
        return mScaledRect;
    
    }
    
    public @State String getState() {
        return mState;
    }
    
    public int getTop() {
        return mTop;
    }
    
    public int getWidth() {
        return mWidth;
    }
    
    public boolean hasBitmap() {
        return (mBitmap != null);
    }
    
    @Override
    public int hashCode() {
    
        int hash = 17;
        hash = ((hash * 31) + getColumn());
        hash = ((hash * 31) + getRow());
        hash = ((hash * 31) + (int) (1000 * getDetailLevel().getScale()));
        
        return hash;
    
    }
    
    void reset() {
    
        if (mState.equals(STATE_PENDING_DECODE)) {
        
            if (mTileRenderRunnableWeakReference != null) {
            
                TileRenderRunnable runnable = mTileRenderRunnableWeakReference.get();
                
                if (runnable != null)
                    runnable.cancel(true);
            
            }
        
        }
        
        mRenderTimestamp = null;
        mState = STATE_UNASSIGNED;
        
        if ((mBitmap != null) && !mBitmap.isRecycled())
            mBitmap.recycle();
        
        mBitmap = null;
    
    }
    
    public void setState(@State String state) {
        mState = state;
    }
    
    public void setTransitionDuration(int transitionDuration) {
        mTransitionDuration = transitionDuration;
    }
    
    public void setTransitionsEnabled(boolean enabled) {
    
        mTransitionsEnabled = enabled;
        
        if (enabled)
            mProgress = 0f;
    
    }
    
    public String toShortString() {
        return (mColumn + ":" + mRow);
    }
    
    private void updateRects() {
    
        mBaseRect.set(mLeft, mTop, mRight, mBottom);
        mIntrinsicRect.set(0, 0, mWidth, mHeight);
        mRelativeRect.set(FloatMathHelper.unscale(mLeft, mDetailLevelScale), FloatMathHelper.unscale(mTop, mDetailLevelScale),
            FloatMathHelper.unscale(mRight, mDetailLevelScale), FloatMathHelper.unscale(mBottom, mDetailLevelScale));
        
        mScaledRect.set(mRelativeRect);
    
    }

}