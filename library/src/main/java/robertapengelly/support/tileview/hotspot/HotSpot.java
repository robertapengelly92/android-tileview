package robertapengelly.support.tileview.hotspot;

import  android.graphics.Region;

public class HotSpot extends Region {

    private HotSpotTapListener mHotSpotTapListener;
    private Object mTag;
    
    public HotSpotTapListener getHotSpotTapListener() {
        return mHotSpotTapListener;
    }
    
    @Override
    public boolean equals( Object obj ) {
    
        if(obj instanceof HotSpot) {
        
            HotSpot hotSpot = (HotSpot) obj;
            return (super.equals(hotSpot) && (hotSpot.mHotSpotTapListener == mHotSpotTapListener));
        
        }
        
        return false;
    
    }
    
    public Object getTag() {
        return mTag;
    }
    
    public void setHotSpotTapListener(HotSpotTapListener hotSpotTapListener) {
        mHotSpotTapListener = hotSpotTapListener;
    }
    
    public void setTag(Object object) {
        mTag = object;
    }
    
    public interface HotSpotTapListener {
        void onHotSpotTap(HotSpot hotSpot, int x, int y);
    }

}