package robertapengelly.support.tileview.detail;

import  android.graphics.Rect;
import  android.support.annotation.NonNull;

import  java.util.HashSet;
import  java.util.Set;

import  robertapengelly.support.tileview.tile.Tile;

public class DetailLevel implements Comparable<DetailLevel> {

    private float mScale;
    private int mTileHeight, mTileWidth;
    
    private Object mData;
    
    private DetailLevelManager mDetailLevelManager;
    private StateSnapshot mLastStateSnapshot;
    private Set<Tile> mTilesVisibleInViewport = new HashSet<>();
    
    public DetailLevel(DetailLevelManager manager, float scale, Object data, int tileWidth, int tileHeight) {
    
        mData = data;
        mDetailLevelManager = manager;
        mScale = scale;
        mTileHeight = tileHeight;
        mTileWidth = tileWidth;
    
    }
    
    @Override
    public int compareTo(@NonNull DetailLevel level) {
        return (int) Math.signum(getScale() - level.getScale());
    }
    
    /**
     * Returns true if there has been a change, false otherwise.
     *
     * @return True if there has been a change, false otherwise.
     */
    public boolean computeCurrentState() {
    
        float relativeScale = getRelativeScale();
        
        int drawableHeight = mDetailLevelManager.getScaledHeight();
        int drawableWidth = mDetailLevelManager.getScaledWidth();
        
        float offsetHeight = (mTileHeight * relativeScale);
        float offsetWidth = (mTileWidth * relativeScale);
        
        Rect viewport = new Rect(mDetailLevelManager.getComputedViewport());
        viewport.bottom = Math.min(viewport.bottom, drawableHeight);
        viewport.left = Math.max(viewport.left, 0);
        viewport.right = Math.min(viewport.right, drawableWidth);
        viewport.top = Math.max(viewport.top, 0);
        
        int columnEnd = (int) Math.ceil(viewport.right / offsetWidth);
        int columnStart = (int) Math.floor(viewport.left / offsetWidth);
        
        int rowEnd = (int) Math.ceil(viewport.bottom / offsetHeight);
        int rowStart = (int) Math.floor(viewport.top / offsetHeight);
        
        StateSnapshot stateSnapshot = new StateSnapshot(this, rowStart, rowEnd, columnStart, columnEnd);
        
        boolean sameState = stateSnapshot.equals(mLastStateSnapshot);
        mLastStateSnapshot = stateSnapshot;
        
        return !sameState;
    
    }
    
    public void computeVisibleTilesFromViewport() {
    
        mTilesVisibleInViewport.clear();
        
        for (int rowCurrent = mLastStateSnapshot.rowStart; rowCurrent < mLastStateSnapshot.rowEnd; ++rowCurrent) {
        
            for (int columnCurrent = mLastStateSnapshot.columnStart; columnCurrent < mLastStateSnapshot.columnEnd; ++columnCurrent) {
            
                Tile tile = new Tile(columnCurrent, rowCurrent, mTileWidth, mTileHeight, mData, this);
                mTilesVisibleInViewport.add(tile);
            
            }
        
        }
    
    }
    
    @Override
    public boolean equals(Object obj) {
    
        if (this == obj)
            return true;
        
        if (obj instanceof DetailLevel) {
        
            DetailLevel level = (DetailLevel) obj;
            return ((mData != null) && mData.equals(level.getData()) && (mScale == level.getScale()));
        
        }
        
        return false;
    
    }
    
    public Object getData() {
        return mData;
    }
    
    public DetailLevelManager getDetailLevelManager() {
        return mDetailLevelManager;
    }
    
    public float getRelativeScale() {
        return (mDetailLevelManager.getScale() / mScale);
    }
    
    public float getScale() {
        return mScale;
    }
    
    public int getTileHeight() {
        return mTileHeight;
    }
    
    public int getTileWidth() {
        return mTileWidth;
    }
    
    /**
     * Returns a list of Tile instances describing the currently visible viewport.
     *
     * @return List of Tile instances describing the currently visible viewport.
     */
    public Set<Tile> getVisibleTilesFromLastViewportComputation() {
    
        if (mLastStateSnapshot == null)
            throw new StateNotComputedException();
        
        return mTilesVisibleInViewport;
    
    }
    
    public boolean hasComputedState() {
        return (mLastStateSnapshot != null);
    }
    
    @Override
    public int hashCode() {
    
        long bits = (Double.doubleToLongBits( getScale() ) * 43);
        return (((int) bits) ^ ((int) (bits >> 32)));
    
    }
    
    /** Ensures that computeCurrentState will return true, indicating a change has occurred. */
    public void invalidate() {
        mLastStateSnapshot = null;
    }
    
    public static class StateNotComputedException extends IllegalStateException {
    
        public StateNotComputedException() {
            super("Grid has not been computed; " +
                "you must call computeCurrentState at some point prior to calling " +
                    "getVisibleTilesFromLastViewportComputation." );
        }
    
    }
    
    private static class StateSnapshot {
    
        int columnEnd, columnStart, rowEnd, rowStart;
        DetailLevel level;
        
        public StateSnapshot(DetailLevel level, int rowStart, int rowEnd, int columnStart, int columnEnd) {
        
            this.columnEnd = columnEnd;
            this.columnStart = columnStart;
            this.level = level;
            this.rowEnd = rowEnd;
            this.rowStart = rowStart;
        
        }
        
        public boolean equals(Object obj) {
        
            if (obj == this)
                return true;
            
            if (obj instanceof StateSnapshot) {
            
                StateSnapshot snapshot = (StateSnapshot) obj;
                return ((columnEnd == snapshot.columnEnd) && (columnStart == snapshot.columnStart) &&
                    level.equals(snapshot.level) && (rowEnd == snapshot.rowEnd) && (rowStart == snapshot.rowStart));
            
            }
            
            return false;
        
        }
    
    }

}