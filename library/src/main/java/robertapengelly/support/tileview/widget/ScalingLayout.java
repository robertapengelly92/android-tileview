package robertapengelly.support.tileview.widget;

import  android.content.Context;
import  android.graphics.Canvas;
import  android.view.View;
import  android.view.ViewGroup;

import  robertapengelly.support.tileview.geom.FloatMathHelper;

public class ScalingLayout extends ViewGroup {

    private float mScale = 1;
    
    public ScalingLayout(Context context) {
        super(context);
        
        setWillNotDraw(false);
    
    }
    
    public float getScale() {
        return mScale;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    
        canvas.scale(mScale, mScale);
        super.onDraw(canvas);
    
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    
        int availableHeight = FloatMathHelper.unscale((b - t), mScale);
        int availableWidth = FloatMathHelper.unscale((r - l), mScale);
        
        for (int i = 0; i < getChildCount(); ++i) {
        
            View child = getChildAt(i);
            
            if (child.getVisibility() != GONE)
                child.layout(0, 0, availableWidth, availableHeight);
        
        }
    
    }
    
    // When scaling a canvas, as happens in onDraw, the clip area will be reduced at a small scale,
    // thus decreasing the drawable surface, but when scaled up, the canvas is still constrained
    // by the original width and height of the backing bitmap, which are not scaled.  Offset those
    // by dividing the measure and layout dimensions by the current scale.
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    
        int availableHeight = FloatMathHelper.unscale(MeasureSpec.getSize(heightMeasureSpec), mScale);
        int availableWidth = FloatMathHelper.unscale(MeasureSpec.getSize(widthMeasureSpec), mScale);
        
        // the container's children should be the size provided by setSize
        // don't use measureChildren because that grabs the child's LayoutParams
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(availableHeight, MeasureSpec.EXACTLY);
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY);
        
        for (int i = 0; i < getChildCount(); ++i) {
        
            View child = getChildAt(i);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        
        }
        
        setMeasuredDimension(availableWidth, availableHeight);
    
    }
    
    public void setScale(float scale) {
    
        mScale = scale;
        invalidate();
    
    }

}