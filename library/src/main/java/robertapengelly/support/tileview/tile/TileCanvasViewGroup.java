package robertapengelly.support.tileview.tile;

import  android.content.Context;
import  android.graphics.Canvas;
import  android.graphics.Rect;
import  android.graphics.Region;
import  android.os.Handler;
import  android.os.Looper;
import  android.os.Message;
import  android.view.ViewGroup;

import  java.lang.ref.WeakReference;
import  java.util.HashSet;
import  java.util.Iterator;
import  java.util.Set;

import  robertapengelly.support.tileview.detail.DetailLevel;
import  robertapengelly.support.tileview.graphics.BitmapProvider;
import  robertapengelly.support.tileview.graphics.BitmapProviderAssets;

/**
 * This class extends ViewGroup for legacy reasons, and may be changed to extend View at
 * some future point; consider all ViewGroup methods deprecated.
 */
public class TileCanvasViewGroup extends ViewGroup {

    private static final int DEFAULT_TRANSITION_DURATION = 200;
    private static final int RENDER_FLAG = 1;
    
    public static final int DEFAULT_RENDER_BUFFER = 250;
    public static final int FAST_RENDER_BUFFER = 15;
    
    private boolean mHasInvalidatedOnCleanOnce,
                    mIsRendering = false,
                    mRenderIsCancelled = false,
                    mRenderIsSuppressed = false,
                    mTransitionsEnabled = true;
    
    private float mScale = 1;
    
    private int mRenderBuffer = DEFAULT_RENDER_BUFFER,
                mTransitionDuration = DEFAULT_TRANSITION_DURATION;
    
    private BitmapProvider mBitmapProvider;
    private DetailLevel mDetailLevelToRender, mLastRenderedDetailLevel;
    private Region mDirtyRegion = new Region();
    
    // This runnable is required to run on UI thread
    private Runnable mRenderPostExecuteRunnable = new Runnable() {
    
        @Override
        public void run() {
        
            cleanup();
            
            if (mTileRenderListener != null)
                mTileRenderListener.onRenderComplete();
            
            mLastRenderedDetailLevel = mDetailLevelToRender;
            requestRender();
        
        }
    
    };
    
    private Set<Tile> mDecodedTilesInCurrentViewport = new HashSet<>(),
                      mPreviouslyDrawnTiles = new HashSet<>(),
                      mTilesInCurrentViewport = new HashSet<>();
    
    private TileRenderListener mTileRenderListener;
    private TileRenderPoolExecutor mTileRenderPoolExecutor;
    private TileRenderThrottleHandler mTileRenderThrottleHandler;
    private TileRenderThrowableListener mTileRenderThrowableListener;
    
    public TileCanvasViewGroup(Context context) {
        super(context);
        setWillNotDraw(false);
        
        mTileRenderPoolExecutor = new TileRenderPoolExecutor();
        mTileRenderThrottleHandler = new TileRenderThrottleHandler(this);
    
    }
    
    // this tile has been decoded by the time it gets passed here
    void addTileToCanvas(final Tile tile) {
    
        if (mTilesInCurrentViewport.contains(tile))
            invalidate();
    
    }
    
    private void beginRenderTask() {
    
        // if visible columns and rows are same as previously computed, fast-fail
        boolean changed = mDetailLevelToRender.computeCurrentState();
        
        if (!changed && mDetailLevelToRender.equals(mLastRenderedDetailLevel))
            return;
        
        // determine tiles are mathematically within the current viewport; force re-computation
        mDetailLevelToRender.computeVisibleTilesFromViewport();
        
        // get rid of anything outside, use previously computed intersections
        cleanup();
        
        // are there any new tiles the Executor isn't already aware of?
        boolean wereTilesAdded = mTilesInCurrentViewport.addAll(mDetailLevelToRender
            .getVisibleTilesFromLastViewportComputation());
        
        // if so, start up a new batch
        if (wereTilesAdded)
            mTileRenderPoolExecutor.queue(this, mTilesInCurrentViewport);
    
    }
    
    /** This should seldom be necessary, as it's built into beginRenderTask */
    public void cleanup() {
    
        if ((mDetailLevelToRender == null) || !mDetailLevelToRender.hasComputedState())
            return;
        
        // these tiles are mathematically within the current viewport, and should be already computed
        Set<Tile> recentlyComputedVisibleTileSet = mDetailLevelToRender.getVisibleTilesFromLastViewportComputation();
        
        // use an iterator to avoid concurrent modification
        Iterator<Tile> tilesInCurrentViewportIterator = mTilesInCurrentViewport.iterator();
        
        while (tilesInCurrentViewportIterator.hasNext()) {
        
            Tile tile = tilesInCurrentViewportIterator.next();
            
            // this tile was visible previously, but is no longer, destroy and de-list it
            if (!recentlyComputedVisibleTileSet.contains(tile)) {
            
                tile.reset();
                tilesInCurrentViewportIterator.remove();
            
            }
        
        }
    
    }
    
    /**
     * Prevent new render tasks from starting, attempts to interrupt ongoing tasks, and will
     * prevent queued tiles from begin decoded or rendered.
     */
    public void cancelRender() {
    
        mRenderIsCancelled = true;

        if (mTileRenderPoolExecutor != null)
            mTileRenderPoolExecutor.cancel();
    
    }
    
    /** Clears existing tiles and cancels any existing render tasks. */
    public void clear() {
    
        cancelRender();
        
        mPreviouslyDrawnTiles.clear();
        mTilesInCurrentViewport.clear();
        
        invalidate();
    
    }
    
    public void destroy() {
    
        mTileRenderPoolExecutor.shutdownNow();
        clear();
        
        if (mTileRenderThrottleHandler.hasMessages(RENDER_FLAG))
            mTileRenderThrottleHandler.removeMessages(RENDER_FLAG);
    
    }
    
    private boolean drawAndClearCurrentDecodedTiles(Canvas canvas) {
    
        boolean shouldInvalidate = false;
        
        for (Tile tile : mDecodedTilesInCurrentViewport) {
        
            // these tiles should already have progress computed by the time they get here
            tile.draw(canvas);
            shouldInvalidate |= tile.getIsDirty();
        
        }
        
        mDecodedTilesInCurrentViewport.clear();
        
        return shouldInvalidate;
    
    }
    
    private boolean drawPreviousTiles(Canvas canvas) {
    
        boolean shouldInvalidate = false;
        Iterator<Tile> tilesFromLastDetailLevelIterator = mPreviouslyDrawnTiles.iterator();
        
        while (tilesFromLastDetailLevelIterator.hasNext()) {
        
            Tile tile = tilesFromLastDetailLevelIterator.next();
            Rect rect = tile.getRelativeRect();
            
            if (mDirtyRegion.quickReject(rect))
                tilesFromLastDetailLevelIterator.remove();
            else {
            
                tile.computeProgress();
                tile.draw(canvas);
                
                shouldInvalidate |= tile.getIsDirty();
            
            }
        
        }
        
        return shouldInvalidate;
    
    }
    
    /**
     * Draw tile bitmaps into the surface canvas displayed by this View.
     *
     * @param canvas The Canvas instance to draw tile bitmaps into.
     */
    private void drawTiles(Canvas canvas) {
    
        if (mPreviouslyDrawnTiles.size() > 0)
            drawTilesConsideringPreviouslyDrawnLevel(canvas);
        else
            drawTilesWithoutConsideringPreviouslyDrawnLevel(canvas);
    
    }
    
    private void drawTilesConsideringPreviouslyDrawnLevel(Canvas canvas) {
    
        // compute states, populate opaque region
        boolean shouldInvalidate = establishDirtyRegion();
        
        // draw any previous tiles that are in viewport and not under full opaque current tiles
        shouldInvalidate |= drawPreviousTiles(canvas);
        
        // draw the current tile set
        shouldInvalidate |= drawAndClearCurrentDecodedTiles(canvas);
        
        // depending on transition states and previous tile draw ops, add'l invalidation might be needed
        handleInvalidation(shouldInvalidate);
    
    }
    
    private void drawTilesWithoutConsideringPreviouslyDrawnLevel(Canvas canvas) {
    
        boolean shouldInvalidate = false;
        
        for (Tile tile : mTilesInCurrentViewport) {
        
            if (!tile.getState().equals(Tile.STATE_DECODED))
                continue;
            
            tile.computeProgress();
            tile.draw(canvas);
            shouldInvalidate |= tile.getIsDirty();
        
        }
        
        handleInvalidation(shouldInvalidate);
    
    }
    
    private boolean establishDirtyRegion() {
    
        boolean shouldInvalidate = false;
        mDirtyRegion.set(getComputedViewport());
        
        for (Tile tile : mTilesInCurrentViewport) {
        
            if (!tile.getState().equals(Tile.STATE_DECODED))
                continue;
            
            tile.computeProgress();
            mDecodedTilesInCurrentViewport.add(tile);
            
            if (tile.getIsDirty())
                shouldInvalidate = true;
            else
                mDirtyRegion.op(tile.getRelativeRect(), Region.Op.DIFFERENCE);
        
        }
        
        return shouldInvalidate;
    
    }
    
    public BitmapProvider getBitmapProvider() {
    
        if (mBitmapProvider == null)
            mBitmapProvider = new BitmapProviderAssets();
        
        return mBitmapProvider;
    
    }
    
    private Rect getComputedViewport() {
    
        if (mDetailLevelToRender == null)
            return null;
        
        return mDetailLevelToRender.getDetailLevelManager().getComputedScaledViewport(getInvertedScale());
    
    }
    
    public float getInvertedScale() {
        return (1f / mScale);
    }
    
    /**
     * Returns true if the TileView has threads currently decoding tile Bitmaps.
     *
     * @return True if the TileView has threads currently decoding tile Bitmaps.
     */
    public boolean getIsRendering() {
        return mIsRendering;
    }
    
    public int getRenderBuffer() {
        return mRenderBuffer;
    }
    
    public boolean getIsRenderCancelled() {
        return mRenderIsCancelled;
    }
    
    public float getScale() {
        return mScale;
    }
    
    public int getTransitionDuration() {
        return mTransitionDuration;
    }
    
    public boolean getTransitionsEnabled() {
        return mTransitionsEnabled;
    }
    
    private void handleInvalidation(boolean shouldInvalidate) {
    
        if (shouldInvalidate) {
        
            // there's more work to do, partially opaque tiles were drawn
            mHasInvalidatedOnCleanOnce = false;
            invalidate();
        
        } else {
        
            // if all tiles were fully opaque, we need another pass to clear our tiles from last level
            if (!mHasInvalidatedOnCleanOnce) {
            
                mHasInvalidatedOnCleanOnce = true;
                invalidate();
            
            }
        
        }
    
    }
    
    void handleTileRenderException(Throwable throwable) {
    
        if (mTileRenderThrowableListener != null)
            mTileRenderThrowableListener.onRenderThrow(throwable);
    
    }
    
    private void markTilesAsPrevious() {
    
        for (Tile tile : mTilesInCurrentViewport)
            if (tile.getState().equals(Tile.STATE_DECODED))
                mPreviouslyDrawnTiles.add(tile);
        
        mTilesInCurrentViewport.clear();
    
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        canvas.save();
        canvas.scale(mScale, mScale);
        
        drawTiles(canvas);
        canvas.restore();
    
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {}
    
    void onRenderTaskCancelled() {
    
        if (mTileRenderListener != null)
            mTileRenderListener.onRenderCancelled();
        
        mIsRendering = false;
    
    }
    
    void onRenderTaskPostExecute() {
    
        mIsRendering = false;
        mTileRenderThrottleHandler.post(mRenderPostExecuteRunnable);
    
    }
    
    void onRenderTaskPreExecute() {
    
        mIsRendering = true;
        
        if (mTileRenderListener != null)
            mTileRenderListener.onRenderStart();
    
    }
    
    void renderTiles() {
    
        if (!mRenderIsCancelled && !mRenderIsSuppressed && (mDetailLevelToRender != null))
            beginRenderTask();
    
    }
    
    /** Enables new render tasks to start */
    public void resumeRender() {
        mRenderIsSuppressed = false;
    }
    
    /**
     * The layout dimensions supplied to this ViewGroup will be exactly as large as the scaled
     * width and height of the containing ZoomPanLayout (or TileView).  However, when the canvas
     * is scaled, it's clip area is also scaled - offset this by providing dimensions scaled as
     * large as the smallest size the TileCanvasView might be.
     */
    public void requestRender() {
    
        mRenderIsCancelled = false;
        
        if (mDetailLevelToRender == null)
            return;
        
        if (!mTileRenderThrottleHandler.hasMessages(RENDER_FLAG))
            mTileRenderThrottleHandler.sendEmptyMessageDelayed(RENDER_FLAG, mRenderBuffer);
    
    }
    
    public void setBitmapProvider(BitmapProvider bitmapProvider) {
        mBitmapProvider = bitmapProvider;
    }
    
    public void setRenderBuffer(int buffer) {
        mRenderBuffer = buffer;
    }
    
    public void setScale(float factor) {
    
        mScale = factor;
        invalidate();
    
    }
    
    public void setTileRenderListener(TileRenderListener tileRenderListener) {
        mTileRenderListener = tileRenderListener;
    }
    
    public void setTileRenderThrowableListener(TileRenderThrowableListener tileRenderThrowableListener) {
        mTileRenderThrowableListener = tileRenderThrowableListener;
    }
    
    public void setTransitionDuration(int duration) {
        mTransitionDuration = duration;
    }
    
    public void setTransitionsEnabled(boolean enabled) {
        mTransitionsEnabled = enabled;
    }
    
    /** Prevent new render tasks from starting, but does not cancel any ongoing operations. */
    public void suppressRender() {
        mRenderIsSuppressed = true;
    }
    
    public void updateTileSet(DetailLevel level) {
    
        if ((level == null) || level.equals(mDetailLevelToRender))
            return;
        
        cancelRender();
        markTilesAsPrevious();
        
        mDetailLevelToRender = level;
        requestRender();
    
    }
    
    private static class TileRenderThrottleHandler extends Handler {
    
        private final WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;
        
        public TileRenderThrottleHandler(TileCanvasViewGroup tileCanvasViewGroup) {
            super(Looper.getMainLooper());
            
            mTileCanvasViewGroupWeakReference = new WeakReference<>(tileCanvasViewGroup);
        
        }
        
        @Override
        public final void handleMessage(Message message) {
        
            final TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
            
            if (tileCanvasViewGroup != null)
                tileCanvasViewGroup.renderTiles();
        
        }
    
    }
    
    /** Interface definition for callbacks to be invoked after render operations. */
    public interface TileRenderListener {
    
        void onRenderCancelled();
        void onRenderComplete();
        void onRenderStart();
    
    }
    
    // ideally this would be part of TileRenderListener, but that's a breaking change
    public interface TileRenderThrowableListener {
        void onRenderThrow(Throwable throwable);
    }

}