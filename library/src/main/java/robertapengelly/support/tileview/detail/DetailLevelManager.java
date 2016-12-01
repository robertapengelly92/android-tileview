package robertapengelly.support.tileview.detail;

import  android.graphics.Rect;

import  java.util.Collections;
import  java.util.LinkedList;

import  robertapengelly.support.tileview.geom.FloatMathHelper;

public class DetailLevelManager {

    private boolean mDetailLevelLocked;
    
    protected float mScale = 1;
    
    private int mBaseHeight, mBaseWidth, mPadding, mScaledHeight, mScaledWidth;
    
    private DetailLevel mCurrentDetailLevel;
    private DetailLevelChangeListener mDetailLevelChangeListener;
    protected LinkedList<DetailLevel> mDetailLevelLinkedList  = new LinkedList<>();
    
    private Rect mComputedScaledViewport = new Rect();
    private Rect mComputedViewport = new Rect();
    private Rect mViewport = new Rect();
    
    public DetailLevelManager() {
        update();
    }
    
    public void addDetailLevel(float scale, Object data, int tileWidth, int tileHeight) {
    
        DetailLevel detailLevel = new DetailLevel(this, scale, data, tileWidth, tileHeight);
        
        if (mDetailLevelLinkedList.contains(detailLevel))            return;
        
        mDetailLevelLinkedList.add(detailLevel);
        Collections.sort(mDetailLevelLinkedList);
        
        update();
    
    }
    
    public int getBaseHeight() {
        return mBaseHeight;
    }
    
    public int getBaseWidth() {
        return mBaseWidth;
    }
    
    public Rect getComputedScaledViewport(float scale){
    
        mComputedScaledViewport.set((int) (mComputedViewport.left * scale), (int) (mComputedViewport.top * scale),
            (int) (mComputedViewport.right * scale), (int) (mComputedViewport.bottom * scale));
        
        return mComputedScaledViewport;
    
    }
    
    public Rect getComputedViewport() {
        return mComputedViewport;
    }
    
    public DetailLevel getCurrentDetailLevel() {
        return mCurrentDetailLevel;
    }
    
    public DetailLevel getDetailLevelForScale() {
    
        if (mDetailLevelLinkedList.size() == 0)
            return null;
        
        if (mDetailLevelLinkedList.size() == 1)
            return mDetailLevelLinkedList.get(0);
        
        DetailLevel match = null;
        int index = (mDetailLevelLinkedList.size() - 1);
        
        for (int i = index; i >= 0; --i) {
        
            match = mDetailLevelLinkedList.get(index);
            
            if (match.getScale() < mScale) {
            
                if (i < index)
                    match = mDetailLevelLinkedList.get(i + 1);
                
                break;
            
            }
        
        }
        
        return match;
    
    }
    
    public boolean getIsLocked() {
        return mDetailLevelLocked;
    }
    
    public float getScale() {
        return mScale;
    }
    
    public int getScaledHeight() {
        return mScaledHeight;
    }
    
    public int getScaledWidth() {
        return mScaledWidth;
    }
    
    public Rect getViewport() {
        return mViewport;
    }
    
    public void invalidateAll() {
    
        for (DetailLevel level : mDetailLevelLinkedList)
            level.invalidate();
    
    }
    
    /**
     * While the detail level is locked (after this method is invoked, and before unlockDetailLevel is invoked),
     * the DetailLevel will not change, and the current DetailLevel will be scaled beyond the normal
     * bounds.  Normally, during any scale change the DetailLevelManager searches for the DetailLevel with
     * a registered scale closest to the defined mScale.  While locked, this does not occur.
     */
    public void lockDetailLevel() {
        mDetailLevelLocked = true;
    }
    
    public void resetDetailLevels() {
    
        mDetailLevelLinkedList.clear();
        update();
    
    }
    
    public void setDetailLevelChangeListener(DetailLevelChangeListener detailLevelChangeListener) {
        mDetailLevelChangeListener = detailLevelChangeListener;
    }
    
    public void setScale(float scale) {
    
        mScale = scale;
        update();
    
    }
    
    public void setSize(int width, int height) {
    
        mBaseHeight = height;
        mBaseWidth = width;
        
        update();
    
    }
    
    /**
     * "pads" the viewport by the number of pixels passed.  e.g., setViewportPadding( 100 ) instructs the
     * DetailManager to interpret it's actual viewport offset by 100 pixels in each direction (top, left,
     * right, bottom), so more tiles will qualify for "visible" status when intersections are calculated.
     *
     * @param pixels The number of pixels to pad the viewport by.
     */
    public void setViewportPadding(int pixels) {
    
        mPadding = pixels;
        updateComputedViewport();
    
    }
    
    /** Unlocks a DetailLevel locked with {@link #lockDetailLevel()}. */
    public void unlockDetailLevel() {
        mDetailLevelLocked = false;
    }
    
    protected void update() {
    
        boolean detailLevelChanged = false;
        
        if (!mDetailLevelLocked) {
        
            DetailLevel matchingLevel = getDetailLevelForScale();
            
            if (matchingLevel != null) {
            
                detailLevelChanged = !matchingLevel.equals(mCurrentDetailLevel);
                mCurrentDetailLevel = matchingLevel;
            
            }
        
        }
        
        mScaledHeight = FloatMathHelper.scale(mBaseHeight, mScale);
        mScaledWidth = FloatMathHelper.scale(mBaseWidth, mScale);
        
        if (detailLevelChanged && (mDetailLevelChangeListener != null))
            mDetailLevelChangeListener.onDetailLevelChanged(mCurrentDetailLevel);
    
    }
    
    private void updateComputedViewport() {
    
        mComputedViewport.set(mViewport);
        
        mComputedViewport.bottom += mPadding;
        mComputedViewport.left -= mPadding;
        mComputedViewport.right += mPadding;
        mComputedViewport.top -= mPadding;
    
    }
    
    public void updateViewport(int left, int top, int right, int bottom) {
    
        mViewport.set(left, top, right, bottom);
        updateComputedViewport();
    
    }
    
    public interface DetailLevelChangeListener {
        void onDetailLevelChanged(DetailLevel level);
    }

}