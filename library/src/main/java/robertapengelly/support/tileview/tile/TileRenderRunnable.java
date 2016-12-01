package robertapengelly.support.tileview.tile;

import  android.os.Handler;
import  android.os.Message;
import  android.os.Process;

import  java.lang.ref.WeakReference;

class TileRenderRunnable implements Runnable {

    private volatile Thread mThread;
    
    private boolean mCancelled = false, mComplete = false;
    
    private Throwable mThrowable;
    private WeakReference<Tile> mTileWeakReference;
    private WeakReference<TileRenderPoolExecutor> mTileRenderPoolExecutorWeakReference;
    
    public boolean cancel(boolean interrupt) {
    
        if (interrupt && (mThread != null))
            mThread.interrupt();
        
        boolean cancelled = mCancelled;
        mCancelled = true;
        
        if (mTileRenderPoolExecutorWeakReference  != null) {
        
            TileRenderPoolExecutor tileRenderPoolExecutor = mTileRenderPoolExecutorWeakReference.get();
            
            if (tileRenderPoolExecutor != null)
                tileRenderPoolExecutor.remove(this);
        
        }
        
        return !cancelled;
    
    }
    
    public Throwable getThrowable() {
        return mThrowable;
    }
    
    public Tile getTile() {
    
        if (mTileWeakReference != null)
            return mTileWeakReference.get();
        
        return null;
    
    }
    
    public boolean isCancelled() {
        return mCancelled;
    }
    
    public boolean isDone() {
        return mComplete;
    }
    
    public @TileRenderHandler.Status int renderTile() {
    
        if (mCancelled)
            return TileRenderHandler.RENDER_INCOMPLETE;
        
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        
        if (mThread.isInterrupted())
            return TileRenderHandler.RENDER_INCOMPLETE;
        
        Tile tile = getTile();
        
        if (tile == null)
            return TileRenderHandler.RENDER_INCOMPLETE;
        
        TileRenderPoolExecutor tileRenderPoolExecutor = mTileRenderPoolExecutorWeakReference.get();
        
        if (tileRenderPoolExecutor == null)
            return TileRenderHandler.RENDER_INCOMPLETE;
        
        TileCanvasViewGroup tileCanvasViewGroup = tileRenderPoolExecutor.getTileCanvasViewGroup();
        
        if (tileCanvasViewGroup == null)
            return TileRenderHandler.RENDER_INCOMPLETE;
        
        try {
            tile.generateBitmap(tileCanvasViewGroup.getContext(), tileCanvasViewGroup.getBitmapProvider());
        } catch (Throwable throwable) {
        
            mThrowable = throwable;
            return TileRenderHandler.RENDER_ERROR;
        
        }
        
        if (mCancelled || mThread.isInterrupted() || (tile.getBitmap() == null)) {
        
            tile.reset();
            return TileRenderHandler.RENDER_ERROR;
        
        }
        
        return TileRenderHandler.RENDER_COMPLETE;
    
    }
    
    @Override
    public void run() {
    
        mThread = Thread.currentThread();
        
        int status = renderTile();
        
        if (status == TileRenderHandler.RENDER_INCOMPLETE)
            return;
        
        if (status == TileRenderHandler.RENDER_COMPLETE)
            mComplete = true;
        
        TileRenderPoolExecutor tileRenderPoolExecutor = mTileRenderPoolExecutorWeakReference.get();
        
        if (tileRenderPoolExecutor == null)
            return;
        
        TileCanvasViewGroup tileCanvasViewGroup = tileRenderPoolExecutor.getTileCanvasViewGroup();
        
        if (tileCanvasViewGroup == null)
            return;
        
        Tile tile = getTile();
        
        if (tile == null)
            return;
        
        Handler handler = tileRenderPoolExecutor.getHandler();
        
        if (handler == null)
            return;
        
        // need to stamp time now, since it'll be drawn before the handler posts
        tile.setTransitionDuration(tileCanvasViewGroup.getTransitionDuration());
        tile.setTransitionsEnabled(tileCanvasViewGroup.getTransitionsEnabled());
        
        Message message = handler.obtainMessage(status, this);
        message.sendToTarget();
    
    }
    
    public void setTile(Tile tile) {
        mTileWeakReference = new WeakReference<>(tile);
    }
    
    public void setTileRenderPoolExecutor(TileRenderPoolExecutor tileRenderPoolExecutor) {
        mTileRenderPoolExecutorWeakReference = new WeakReference<>(tileRenderPoolExecutor);
    }

}