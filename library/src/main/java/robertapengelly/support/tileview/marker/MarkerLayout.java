package robertapengelly.support.tileview.marker;

import  android.content.Context;
import  android.graphics.Rect;
import  android.view.View;
import  android.view.ViewGroup;

import  robertapengelly.support.tileview.geom.FloatMathHelper;

public class MarkerLayout extends ViewGroup {

    private float mAnchorX, mAnchorY, mScale = 1;
    
    private MarkerTapListener mMarkerTapListener;
    
    public MarkerLayout(Context context) {
        super(context);
        
        setClipChildren(false);
    
    }
    
    public View addMarker(View view, LayoutParams params) {
    
        addView(view, params);
        return view;
    
    }
    
    public View addMarker(View view, int x, int y, Float aX, Float aY) {
    
        ViewGroup.LayoutParams defaultLayoutParams = view.getLayoutParams();
        
        LayoutParams markerLayoutParams = ((defaultLayoutParams != null)
            ? generateLayoutParams(defaultLayoutParams) : generateDefaultLayoutParams());
        
        markerLayoutParams.anchorX = aX;
        markerLayoutParams.anchorY = aY;
        markerLayoutParams.x = x;
        markerLayoutParams.y = y;
        
        return addMarker(view, markerLayoutParams);
    
    }
    
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return (layoutParams instanceof LayoutParams);
    }
    
    @Override
    protected MarkerLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
    }
    
    @Override
    protected MarkerLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }
    
    /**
     * Retrieves the current scale of the MarkerLayout.
     *
     * @return The current scale of the MarkerLayout.
     */
    public float getScale() {
        return mScale;
    }
    
    private View getViewFromTap( int x, int y ) {
    
        for (int i = (getChildCount() - 1); i >= 0; --i) {
        
            View child = getChildAt(i);
            
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            Rect hitRect = layoutParams.getHitRect();
            
            if (hitRect.contains(x, y))
                return child;
        
        }
        
        return null;
    
    }
    
    public void moveMarker(View view, LayoutParams params) {
    
        if (indexOfChild(view) == -1)
            return;
        
        view.setLayoutParams(params);
        requestLayout();
    
    }
    
    public void moveMarker(View view, int x, int y) {
    
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.x = x;
        layoutParams.y = y;
        
        moveMarker(view, layoutParams);
    
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    
        for (int i = 0; i < getChildCount(); ++i) {
        
            View child = getChildAt(i);
            
            if (child.getVisibility() != GONE) {
            
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                child.layout(layoutParams.mLeft, layoutParams.mTop, layoutParams.mRight, layoutParams.mBottom);
            
            }
        
        }
    
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        
        for (int i = 0; i < getChildCount(); ++i) {
        
            View child = getChildAt(i);
            
            if ( child.getVisibility() == GONE )
                continue;
            
            MarkerLayout.LayoutParams params = (MarkerLayout.LayoutParams) child.getLayoutParams();
            
            // get anchor offsets
            float heightMultiplier = ((params.anchorY == null) ? mAnchorY : params.anchorY);
            float widthMultiplier = ((params.anchorX == null) ? mAnchorX : params.anchorX);
            
            // actual sizes of children
            int actualHeight = child.getMeasuredHeight();
            int actualWidth = child.getMeasuredWidth();
            
            // offset dimensions by anchor values
            float heightOffset = (actualHeight * heightMultiplier);
            float widthOffset = (actualWidth * widthMultiplier);
            
            // get offset position
            int scaledX = FloatMathHelper.scale(params.x, mScale);
            int scaledY = FloatMathHelper.scale(params.y, mScale);
            
            // save computed values
            params.mLeft = (int) (scaledX + widthOffset);
            params.mTop = (int) (scaledY + heightOffset);
            
            params.mBottom = (params.mTop + actualHeight);
            params.mRight = (params.mLeft + actualWidth);
        
        }
        
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        
        setMeasuredDimension(availableWidth, availableHeight);
    
    }
    
    public void processHit(int x, int y) {
    
        if (mMarkerTapListener == null )
            return;
        
        View view = getViewFromTap(x, y);
        
        if (view != null)
            mMarkerTapListener.onMarkerTap(view, x, y);
    
    }
    
    public void removeMarker(View view) {
        removeView(view);
    }
    
    /**
     * Sets the anchor values used by this ViewGroup if it's children do not
     * have anchor values supplied directly (via individual LayoutParams).
     *
     * @param aX x-axis anchor value (offset computed by multiplying this value by the child's width).
     * @param aY y-axis anchor value (offset computed by multiplying this value by the child's height).
     */
    public void setAnchors(float aX, float aY) {
    
        mAnchorX = aX;
        mAnchorY = aY;
        
        requestLayout();
    
    }
    
    public void setMarkerTapListener(MarkerTapListener markerTapListener) {
        mMarkerTapListener = markerTapListener;
    }
    
    /**
     * Sets the scale (0-1) of the MarkerLayout.
     *
     * @param scale The new value of the MarkerLayout scale.
     */
    public void setScale(float scale) {
    
        mScale = scale;
        requestLayout();
    
    }
    
    /** Per-child layout information associated with AnchorLayout. */
    public static class LayoutParams extends ViewGroup.LayoutParams {
    
        /**
         * Float value to determine the child's horizontal offset.
         * This float is multiplied by the child's width.
         * If null, the containing AnchorLayout's anchor values will be used.
         */
        public Float anchorX = null;
        
        /**
         * Float value to determine the child's vertical offset.
         * This float is multiplied by the child's height.
         * If null, the containing AnchorLayout's anchor values will be used.
         */
        public Float anchorY = null;
        
        /** The absolute left position of the child in pixels. */
        public int x = 0;
        
        /** The absolute right position of the child in pixels. */
        public int y = 0;
        
        private int mBottom, mLeft, mRight, mTop;
        private Rect mHitRect;
        
        /**
         * Copy constructor.
         *
         * @param source LayoutParams instance to copy properties from.
         */
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
        
        /**
         * Creates a new set of layout parameters with the specified values.
         *
         * @param width  Information about how wide the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
         * @param height Information about how tall the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }
        
        /**
         * Creates a new set of layout parameters with the specified values.
         *
         * @param width  Information about how wide the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
         * @param height Information about how tall the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
         * @param left   Sets the absolute x value of the view's position in pixels.
         * @param top    Sets the absolute y value of the view's position in pixels.
         */
        public LayoutParams(int width, int height, int left, int top) {
            super(width, height);
            
            x = left;
            y = top;
        
        }
        
        /**
         * Creates a new set of layout parameters with the specified values.
         *
         * @param width      Information about how wide the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
         * @param height     Information about how tall the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
         * @param left       Sets the absolute x value of the view's position in pixels.
         * @param top        Sets the absolute y value of the view's position in pixels.
         * @param anchorLeft Sets the relative horizontal offset of the view (multiplied by the view's width).
         * @param anchorTop  Sets the relative vertical offset of the view (multiplied by the view's height).
         */
        public LayoutParams(int width, int height, int left, int top, Float anchorLeft, Float anchorTop) {
            super(width, height);
            
            anchorX = anchorLeft;
            anchorY = anchorTop;
            
            x = left;
            y = top;
        
        }
        
        private Rect getHitRect() {
        
            if (mHitRect == null)
                mHitRect = new Rect();
            
            mHitRect.bottom = mBottom;
            mHitRect.left = mLeft;
            mHitRect.right = mRight;
            mHitRect.top = mTop;
            
            return mHitRect;
        
        }
    
    }
    
    public interface MarkerTapListener {
        void onMarkerTap(View view, int x, int y);
    }

}