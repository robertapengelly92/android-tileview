package robertapengelly.support.tileview.tile;

import  android.os.Handler;
import  android.os.Looper;
import  android.os.Message;
import  android.support.annotation.IntDef;

import  java.lang.annotation.Retention;
import  java.lang.annotation.RetentionPolicy;
import  java.lang.ref.WeakReference;

class TileRenderHandler extends Handler {

    public static final int RENDER_ERROR      = -1;
    public static final int RENDER_INCOMPLETE = 0;
    public static final int RENDER_COMPLETE   = 1;
    
    private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;
    
    /** @hide **/
    @IntDef({RENDER_ERROR, RENDER_INCOMPLETE, RENDER_COMPLETE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {}
    
    public TileRenderHandler() {
        this(Looper.getMainLooper());
    }

    public TileRenderHandler(Looper looper) {
        super(looper);
    }
    
    public TileCanvasViewGroup getTileCanvasViewGroup() {
    
        if (mTileCanvasViewGroupWeakReference == null)
            return null;
        
        return mTileCanvasViewGroupWeakReference.get();
    
    }
    
    @Override
    public void handleMessage(Message message) {
    
        TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) message.obj;
        TileCanvasViewGroup tileCanvasViewGroup = getTileCanvasViewGroup();
        
        if (tileCanvasViewGroup == null)
            return;
        
        Tile tile = tileRenderRunnable.getTile();
        
        if (tile == null)
            return;
        
        switch(message.what) {
        
            case RENDER_COMPLETE:
                tileCanvasViewGroup.addTileToCanvas(tile);
                break;
            case RENDER_ERROR:
                tileCanvasViewGroup.handleTileRenderException(tileRenderRunnable.getThrowable());
                break;
        
        }
    
    }

    public void setTileCanvasViewGroup(TileCanvasViewGroup tileCanvasViewGroup) {
        mTileCanvasViewGroupWeakReference = new WeakReference<>(tileCanvasViewGroup);
    }

}