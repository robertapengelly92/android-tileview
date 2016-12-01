package robertapengelly.support.tileview.path;

import  android.content.Context;
import  android.graphics.Canvas;
import  android.graphics.Matrix;
import  android.graphics.Paint;
import  android.graphics.Path;
import  android.view.View;

import  java.util.HashSet;

public class CompositePathView extends View {

    private static final int DEFAULT_STROKE_COLOR = 0xFF000000;
    private static final int DEFAULT_STROKE_WIDTH = 10;
    
    private boolean mShouldDraw = true;
    private float mScale = 1;
    
    private HashSet<DrawablePath> mDrawablePaths = new HashSet<>();
    private Matrix mMatrix = new Matrix();
    private Paint mDefaultPaint = new Paint();

    private Path mRecyclerPath = new Path();
    
    {
        mDefaultPaint.setAntiAlias(true);
        mDefaultPaint.setColor(DEFAULT_STROKE_COLOR);
        mDefaultPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        mDefaultPaint.setStyle(Paint.Style.STROKE);
    }
    
    public CompositePathView(Context context) {
        super(context);
    }
    
    public DrawablePath addPath(DrawablePath DrawablePath) {
    
        mDrawablePaths.add(DrawablePath);
        invalidate();
        
        return DrawablePath;
    
    }
    
    public DrawablePath addPath(Path path, Paint paint) {
    
        if (paint == null)
            paint = mDefaultPaint;
        
        DrawablePath DrawablePath = new DrawablePath();
        DrawablePath.paint = paint;
        DrawablePath.path = path;
        
        return addPath(DrawablePath);
    
    }
    
    public void clear() {
    
        mDrawablePaths.clear();
        invalidate();
    
    }
    
    public Paint getDefaultPaint() {
        return mDefaultPaint;
    }
    
    public float getScale() {
        return mScale;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    
        if (mShouldDraw) {
        
            for (DrawablePath path : mDrawablePaths) {
            
                mRecyclerPath.set(path.path);
                mRecyclerPath.transform(mMatrix);
                
                canvas.drawPath(mRecyclerPath, path.paint);
            
            }
        
        }
        
        super.onDraw(canvas);
    
    }
    
    public void removePath(DrawablePath DrawablePath) {
    
        mDrawablePaths.remove(DrawablePath);
        invalidate();
    
    }
    
    public void setScale(float scale) {
    
        mMatrix.setScale(scale, scale);
        mScale = scale;
        
        invalidate();
    
    }
    
    public void setShouldDraw(boolean shouldDraw) {
    
        mShouldDraw = shouldDraw;
        invalidate();
    
    }
    
    public static class DrawablePath {

        /** The path that this drawable will follow. */
        public Path path;
        
        /** The paint to be used for this path. */
        public Paint paint;

    }

}