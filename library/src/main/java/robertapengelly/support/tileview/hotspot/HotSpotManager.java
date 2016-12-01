package robertapengelly.support.tileview.hotspot;

import  java.util.Iterator;
import  java.util.LinkedList;

import  robertapengelly.support.tileview.geom.FloatMathHelper;

public class HotSpotManager {

    private float mScale = 1;
    
    private HotSpot.HotSpotTapListener mHotSpotTapListener;
    private LinkedList<HotSpot> mHotSpots = new LinkedList<HotSpot>();
    
    public void addHotSpot(HotSpot hotSpot) {
        mHotSpots.add(hotSpot);
    }
    
    public void clear() {
        mHotSpots.clear();
    }
    
    private HotSpot getMatch( int x, int y ) {
    
        int scaledX = FloatMathHelper.unscale(x, mScale);
        int scaledY = FloatMathHelper.unscale(y, mScale);
        
        Iterator<HotSpot> iterator = mHotSpots.descendingIterator();
        
        while(iterator.hasNext()) {
        
            HotSpot hotSpot = iterator.next();
            
            if(hotSpot.contains(scaledX, scaledY))
                return hotSpot;
        
        }
        
        return null;
    
    }
    
    public float getScale() {
        return mScale;
    }
    
    public void processHit(int x, int y) {
    
        HotSpot hotSpot = getMatch(x, y);
        
        if (hotSpot == null)
            return;
        
        HotSpot.HotSpotTapListener spotListener = hotSpot.getHotSpotTapListener();
        
        if(spotListener != null)
            spotListener.onHotSpotTap(hotSpot, x, y);
        
        if(mHotSpotTapListener != null)
            mHotSpotTapListener.onHotSpotTap(hotSpot, x, y);
    
    }
    
    public void removeHotSpot(HotSpot hotSpot) {
        mHotSpots.remove(hotSpot);
    }
    
    public void setHotSpotTapListener(HotSpot.HotSpotTapListener hotSpotTapListener) {
        mHotSpotTapListener = hotSpotTapListener;
    }
    
    public void setScale(float scale) {
        mScale = scale;
    }

}