package robertapengelly.support.tileview.view;

import  android.view.MotionEvent;

public class TouchUpGestureDetector {

    private OnTouchUpListener mOnTouchUpListener;
    
    public TouchUpGestureDetector(OnTouchUpListener listener) {
        mOnTouchUpListener = listener;
    }
    
    public boolean onTouchEvent(MotionEvent event) {
    
        if ((event.getActionMasked() == MotionEvent.ACTION_UP) && (mOnTouchUpListener != null))
            return mOnTouchUpListener.onTouchUp(event);
        
        return true;
    
    }
    
    public interface OnTouchUpListener {
        boolean onTouchUp(MotionEvent event);
    }

}