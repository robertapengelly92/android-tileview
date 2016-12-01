package robertapengelly.support.tileview.widget;

import  android.content.Context;
import  android.support.annotation.StringDef;
import  android.util.AttributeSet;
import  android.view.GestureDetector;
import  android.view.MotionEvent;
import  android.view.ScaleGestureDetector;
import  android.view.View;
import  android.view.ViewGroup;
import  android.view.animation.Interpolator;
import  android.widget.Scroller;

import  java.lang.annotation.Retention;
import  java.lang.annotation.RetentionPolicy;
import  java.lang.ref.WeakReference;
import  java.util.HashSet;

import  robertapengelly.support.animation.Animator;
import  robertapengelly.support.animation.ValueAnimator;
import  robertapengelly.support.tileview.geom.FloatMathHelper;
import  robertapengelly.support.tileview.view.TouchUpGestureDetector;

/**
 * ZoomPanLayout extends ViewGroup to provide support for scrolling and zooming.
 * Fling, drag, pinch and double-tap events are supported natively.
 *
 * Children of ZoomPanLayout are laid out to the sizes provided by setSize,
 * and will always be positioned at 0, 0.
 */
public class ZoomPanLayout extends ViewGroup implements GestureDetector.OnDoubleTapListener,
    GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, TouchUpGestureDetector.OnTouchUpListener {
    
    private static final int DEFAULT_ZOOM_PAN_ANIMATION_DURATION = 400;
    
    public static final String MINIMUM_SCALE_MODE_FILL  = "minimum_scale_mode_fill";
    public static final String MINIMUM_SCALE_MODE_FIT   = "minimum_scale_mode_fit";
    public static final String MINIMUM_SCALE_MODE_NONE  = "minimum_scale_mode_none";
    
    private boolean mIsDragging, mIsFlinging, mIsScaling, mIsSliding, mShouldLoopScale = true;
    private float mEffectiveMinScale = 0, mMaxScale = 1, mMinScale = 0, mScale = 1;
    private int mAnimationDuration = DEFAULT_ZOOM_PAN_ANIMATION_DURATION;
    private int mBaseHeight, mBaseWidth, mOffsetX, mOffsetY, mScaledHeight, mScaledWidth;
    
    private String mMinimumScaleMode = MINIMUM_SCALE_MODE_FILL;
    
    private GestureDetector mGestureDetector;
    private HashSet<ZoomPanListener> mListeners = new HashSet<>();
    private ScaleGestureDetector mScaleGestureDetector;
    private Scroller mScroller;
    private TouchUpGestureDetector mTouchUpGestureDetector;
    private ZoomPanAnimator mZoomPanAnimator;
    
    /** @hide **/
    @StringDef({MINIMUM_SCALE_MODE_FILL, MINIMUM_SCALE_MODE_FIT, MINIMUM_SCALE_MODE_NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MinimumScaleMode {}
    
    public ZoomPanLayout(Context context) {
        this(context, null);
    }
    
    public ZoomPanLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public ZoomPanLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        setClipChildren(false);
        setWillNotDraw(false);
        
        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);
        mTouchUpGestureDetector = new TouchUpGestureDetector(this);
    
    }
    
    /**
     * Adds a ZoomPanListener to the ZoomPanLayout, which will receive notification of actions
     * relating to zoom and pan events.
     *
     * @param listener ZoomPanListener implementation to add.
     *
     * @return True when the listener set did not already contain the listener, false otherwise.
     */
    public boolean addZoomPanListener(ZoomPanListener listener) {
        return mListeners.add(listener);
    }
    
    private void broadcastDragBegin() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanBegin(getScrollX(), getScrollY(), ZoomPanListener.ORIGINATION_DRAG);
    
    }
    
    private void broadcastDragEnd() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanEnd(getScrollX(), getScrollY(), ZoomPanListener.ORIGINATION_DRAG);
    
    }
    
    private void broadcastDragUpdate() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanUpdate(getScrollX(), getScrollY(), ZoomPanListener.ORIGINATION_DRAG);
    
    }
    
    private void broadcastFlingBegin() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanBegin(mScroller.getStartX(), mScroller.getStartY(), ZoomPanListener.ORIGINATION_FLING);
    
    }
    
    private void broadcastFlingEnd() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanBegin(mScroller.getFinalX(), mScroller.getFinalY(), ZoomPanListener.ORIGINATION_FLING);
    
    }
    
    private void broadcastFlingUpdate() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanBegin(mScroller.getCurrX(), mScroller.getCurrY(), ZoomPanListener.ORIGINATION_FLING);
    
    }
    
    private void broadcastPinchBegin() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onZoomBegin(mScale, ZoomPanListener.ORIGINATION_PINCH);
    
    }
    
    private void broadcastPinchEnd() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onZoomEnd(mScale, ZoomPanListener.ORIGINATION_PINCH);
    
    }
    
    private void broadcastPinchUpdate() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onZoomUpdate(mScale, ZoomPanListener.ORIGINATION_PINCH);
    
    }
    
    private void broadcastProgrammaticPanBegin() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanBegin(getScrollX(), getScrollY(), ZoomPanListener.ORIGINATION_PROGRAMMATIC);
    
    }
    
    private void broadcastProgrammaticPanEnd() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanEnd(getScrollX(), getScrollY(), ZoomPanListener.ORIGINATION_PROGRAMMATIC);
    
    }
    
    private void broadcastProgrammaticPanUpdate() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onPanUpdate(getScrollX(), getScrollY(), ZoomPanListener.ORIGINATION_PROGRAMMATIC);
    
    }
    
    private void broadcastProgrammaticZoomBegin() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onZoomBegin(mScale, ZoomPanListener.ORIGINATION_PROGRAMMATIC);
    
    }
    
    private void broadcastProgrammaticZoomEnd() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onZoomBegin(mScale, ZoomPanListener.ORIGINATION_PROGRAMMATIC);
    
    }
    
    private void broadcastProgrammaticZoomUpdate() {
    
        for (ZoomPanListener listener : mListeners)
            listener.onZoomBegin(mScale, ZoomPanListener.ORIGINATION_PROGRAMMATIC);
    
    }
    
    private void calculateMinimumScaleToFit() {
    
        float minimumScaleX = (getWidth() / (float) mBaseWidth);
        float minimumScaleY = (getHeight() / (float) mBaseHeight);
        float recalculatedMinScale = calculateMinScale(minimumScaleX, minimumScaleY);
        
        if (recalculatedMinScale == mEffectiveMinScale)
            return;
        
        mEffectiveMinScale = recalculatedMinScale;
        
        if (mScale < mEffectiveMinScale)
            setScale(mEffectiveMinScale);
    
    }
    
    private float calculateMinScale(float minimumScaleX, float minimumScaleY) {
    
        switch (mMinimumScaleMode) {
        
            case MINIMUM_SCALE_MODE_FILL:
                return Math.max(minimumScaleX, minimumScaleY);
            case MINIMUM_SCALE_MODE_FIT:
                return Math.min(minimumScaleX, minimumScaleY);
            default:
                return mMinScale;
        
        }
    
    }
    
    @Override
    public boolean canScrollHorizontally(int direction) {
        return ((direction > 0) ? (getScrollX() < getScrollLimitX()) : ((direction < 0) && (getScrollX() > 0)));
    }
    
    @Override
    public void computeScroll() {
    
        if (!mScroller.computeScrollOffset())
            return;
        
        int endX = getConstrainedScrollX(mScroller.getCurrX());
        int endY = getConstrainedScrollY(mScroller.getCurrY());
        
        int startX = getScrollX();
        int startY = getScrollY();
        
        if ((startX != endX) || (startY != endY)) {
        
            scrollTo(endX, endY);
            
            if (mIsFlinging)
                broadcastFlingUpdate();
        
        }
        
        if (mScroller.isFinished()) {
        
            if (mIsFlinging) {
            
                mIsFlinging = false;
                broadcastFlingEnd();
            
            }
        
        } else
            invalidate();
    
    }
    
    private void constrainScrollToLimits() {
    
        int x = getScrollX();
        int y = getScrollY();
        
        int constrainedX = getConstrainedScrollX(x);
        int constrainedY = getConstrainedScrollY(y);
        
        if((x != constrainedX) || (y != constrainedY))
            scrollTo(constrainedX, constrainedY);
    
    }
    
    protected ZoomPanAnimator getAnimator() {
    
        if (mZoomPanAnimator == null) {
        
            mZoomPanAnimator = new ZoomPanAnimator(this);
            mZoomPanAnimator.setDuration(mAnimationDuration);
        
        }
        
        return mZoomPanAnimator;
    
    }
    
    /**
     * Returns the duration zoom and pan animations will use.
     *
     * @return The duration zoom and pan animations will use.
     */
    public int getAnimationDuration() {
        return mAnimationDuration;
    }
    
    /**
     * Returns the base (not scaled) height of the underlying composite image.
     *
     * @return The base (not scaled) height of the underlying composite image.
     */
    public int getBaseHeight() {
        return mBaseHeight;
    }
    
    /**
     * Returns the base (not scaled) width of the underlying composite image.
     *
     * @return The base (not scaled) width of the underlying composite image.
     */
    public int getBaseWidth() {
        return mBaseWidth;
    }
    
    private float getConstrainedDestinationScale(float scale) {
    
        scale = Math.max(scale, mEffectiveMinScale);
        scale = Math.min(scale, mMaxScale);
        
        return scale;
    
    }
    
    private int getConstrainedScrollX(int x) {
        return Math.max(0, Math.min(x, getScrollLimitX()));
    }
    
    private int getConstrainedScrollY(int y) {
        return Math.max(0, Math.min(y, getScrollLimitY()));
    }
    
    protected int getHalfHeight() {
        return FloatMathHelper.scale(getHeight(), 0.5f);
    }
    
    protected int getHalfWidth() {
        return FloatMathHelper.scale(getWidth(), 0.5f);
    }
    
    private int getOffsetScrollXFromScale(int offsetX, float destinationScale, float currentScale) {
    
        int scrollX = (getScrollX() + offsetX);
        float deltaScale = (destinationScale / currentScale);
        
        return (int) ((scrollX * deltaScale) - offsetX);
    
    }

    private int getOffsetScrollYFromScale(int offsetY, float destinationScale, float currentScale) {
    
        int scrollY = (getScrollY() + offsetY);
        float deltaScale = (destinationScale / currentScale);
        
        return (int) ((scrollY * deltaScale) - offsetY);
    
    }
    
    /**
     * Returns the horizontal distance children are offset if the content is scaled smaller than width.
     *
     * @return
     */
    public int getOffsetX() {
        return mOffsetX;
    }
    
    /**
     * Return the vertical distance children are offset if the content is scaled smaller than height.
     *
     * @return
     */
    public int getOffsetY() {
        return mOffsetY;
    }
    
    /**
     * Retrieves the current scale of the ZoomPanLayout.
     *
     * @return The current scale of the ZoomPanLayout.
     */
    public float getScale() {
        return mScale;
    }
    
    /**
     * Returns the scaled height of the underlying composite image.
     *
     * @return The scaled height of the underlying composite image.
     */
    public int getScaledHeight() {
        return mScaledHeight;
    }
    
    /**
     * Returns the scaled width of the underlying composite image.
     *
     * @return The scaled width of the underlying composite image.
     */
    public int getScaledWidth() {
        return mScaledWidth;
    }
    
    private int getScrollLimitX() {
        return (mScaledWidth - getWidth());
    }
    
    private int getScrollLimitY() {
        return (mScaledHeight - getHeight());
    }
    
    /**
     * Returns the Scroller instance used to manage dragging and flinging.
     *
     * @return The Scroller instance use to manage dragging and flinging.
     */
    public Scroller getScroller() {
        return mScroller;
    }
    
    /**
     * Returns whether the ZoomPanLayout is currently being dragged.
     *
     * @return true if the ZoomPanLayout is currently dragging, false otherwise.
     */
    public boolean isDragging() {
        return mIsDragging;
    }
    
    /**
     * Returns whether the ZoomPanLayout is currently being flung.
     *
     * @return true if the ZoomPanLayout is currently flinging, false otherwise.
     */
    public boolean isFlinging() {
        return mIsFlinging;
    }
    
    /**
     * Returns whether the ZoomPanLayout is currently operating a scale tween.
     *
     * @return True if the ZoomPanLayout is currently scaling, false otherwise.
     */
    public boolean isScaling() {
        return mIsScaling;
    }
    
    /**
     * Returns whether the ZoomPanLayout is currently operating a scroll tween.
     *
     * @return True if the ZoomPanLayout is currently scrolling, false otherwise.
     */
    public boolean isSliding() {
        return mIsSliding;
    }
    
    @Override
    public boolean onDoubleTap(MotionEvent event) {
    
        float destination = (float) Math.pow(2, Math.floor(Math.log(mScale * 2) / Math.log(2)));
        float effectiveDestination = ((mShouldLoopScale && (mScale >= mMaxScale)) ? mMinScale : destination);
        
        destination = getConstrainedDestinationScale(effectiveDestination);
        smoothScaleFromFocalPoint((int) event.getX(), (int)event.getY(), destination);
        
        return true;
    
    }
    
    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        return true;
    }
    
    @Override
    public boolean onDown(MotionEvent event) {
    
        if (mIsFlinging && !mScroller.isFinished()) {
        
            mIsFlinging = false;
            mScroller.forceFinished(true);
            
            broadcastFlingEnd();
        
        }
        
        return true;
    
    }
    
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
    
        mScroller.fling(getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY, 0, getScrollLimitX(),
            0, getScrollLimitY());
        
        mIsFlinging = true;
        invalidate();
        
        broadcastFlingBegin();
        return true;
    
    }
    
    // ZoomPanChildren will always be laid out with the scaled dimenions - what is visible during
    // scroll operations.  Thus, a RelativeLayout added as a child that had views within it using
    // rules like ALIGN_PARENT_RIGHT would function as expected; similarly, an ImageView would be
    // stretched between the visible edges.
    // If children further operate on scale values, that should be accounted for
    // in the child's logic (see ScalingLayout).
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    
        final int height = getHeight();
        final int width = getWidth();
        
        mOffsetX = mOffsetY = 0;
        
        if (mScaledHeight < height)
            mOffsetY = ((height / 2) - (mScaledHeight / 2));
        
        if (mScaledWidth < width)
            mOffsetX = ((width / 2) - (mScaledWidth / 2));
        
        for (int i = 0; i < getChildCount(); ++i) {
        
            View child = getChildAt(i);
            
            if (child.getVisibility() != GONE)
                child.layout(mOffsetX, mOffsetY, (mScaledWidth + mOffsetX), (mScaledHeight + mOffsetY));
        
        }
        
        calculateMinimumScaleToFit();
        constrainScrollToLimits();
    
    }
    
    @Override
    public void onLongPress(MotionEvent event) {}
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    
        // the container's children should be the size provided by setSize
        // don't use measureChildren because that grabs the child's LayoutParams
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mScaledHeight, MeasureSpec.EXACTLY);
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mScaledWidth, MeasureSpec.EXACTLY);
        
        for (int i = 0; i < getChildCount(); ++i) {
        
            View child = getChildAt(i);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        
        }
        
        // but the layout itself should report normal (on screen) dimensions
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        
        height = resolveSize(height, heightMeasureSpec);
        width = resolveSize(width, widthMeasureSpec);
        
        setMeasuredDimension(width, height);
    
    }
    
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
    
        float currentScale = (mScale * detector.getScaleFactor());
        setScaleFromPosition((int) detector.getFocusX(), (int)detector.getFocusY(), currentScale);
        
        broadcastPinchUpdate();
        return true;
    
    }
    
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
    
        mIsScaling = true;
        broadcastPinchBegin();
        
        return true;
    
    }
    
    /** Provide this method to be overriden by subclasses. */
    public void onScaleChanged(float currentScale, float previousScale) {}
    
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    
        mIsScaling = false;
        broadcastPinchEnd();
    
    }
    
    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
    
        int scrollEndX = (getScrollX() + (int) distanceX);
        int scrollEndY = (getScrollY() + (int) distanceY);
        
        scrollTo(scrollEndX, scrollEndY);
        
        if (mIsDragging)
            broadcastDragUpdate();
        else {
        
            mIsDragging = true;
            broadcastDragBegin();
        
        }
        
        return true;
    
    }
    
    @Override
    public void onShowPress(MotionEvent event) {}
    
    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        return true;
    }
    
    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return true;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    
        boolean gestureIntercept = mGestureDetector.onTouchEvent(event);
        boolean scaleIntercept = mScaleGestureDetector.onTouchEvent(event);
        boolean touchIntercept = mTouchUpGestureDetector.onTouchEvent(event);
        
        return (gestureIntercept || scaleIntercept || touchIntercept || super.onTouchEvent(event));
    
    }
    
    @Override
    public boolean onTouchUp(MotionEvent event) {
    
        if (mIsDragging) {
        
            mIsDragging = false;
            
            if (!mIsFlinging)
                broadcastDragEnd();
        
        }
        
        return true;
    
    }
    
    /**
     * Removes a ZoomPanListener from the ZoomPanLayout
     *
     * @param listener ZoomPanListener to remove.
     *
     * @return True if the listener was removed, false otherwise.
     */
    public boolean removeZoomPanListener(ZoomPanListener listener) {
        return mListeners.remove(listener);
    }
    
    @Override
    public void scrollTo(int x, int y) {
    
        x = getConstrainedScrollX(x);
        y = getConstrainedScrollY(y);
        
        super.scrollTo(x, y);
    
    }
    
    /**
     * Scrolls and centers the ZoomPanLayout to the x and y values provided.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     */
    public void scrollToAndCenter(int x, int y) {
        scrollTo((x - getHalfWidth()), (y - getHalfHeight()));
    }
    
    /**
     * Set the duration zoom and pan animation will use.
     *
     * @param animationDuration The duration animations will use.
     */
    public void setAnimationDuration(int animationDuration) {
    
        mAnimationDuration = animationDuration;
        
        if (mZoomPanAnimator != null)
            mZoomPanAnimator.setDuration(mAnimationDuration);
    
    }
    
    /**
     * Sets the minimum scale mode.
     *
     * <dl>
     *  <dt> {@link #MINIMUM_SCALE_MODE_FILL} ("minimum_scale_mode_fill")
     *  <dd> Indicates that you want to limit the minimum scale to no less than what
     *       would be required to fill the container.
     *  <dt> {@link #MINIMUM_SCALE_MODE_FIT} ("minimum_scale_mode_fit")
     *  <dd> Indicates that you want to limit the minimum scale to no less than what
     *       would be required to fit inside the container
     *  <dt> {@link #MINIMUM_SCALE_MODE_NONE} ("minimum_scale_mode_none")
     *  <dd> Indicates that you want to allow arbitrary minimum scale.
     * </dl>
     *
     * @param minimumScaleMode The minimum scale mode.
     */
    public void setMinimumScaleMode(@MinimumScaleMode String minimumScaleMode) {
    
        mMinimumScaleMode = minimumScaleMode;
        calculateMinimumScaleToFit();
    
    }
    
    /**
     * Sets the scale (0-1) of the ZoomPanLayout.
     *
     * @param scale The new value of the ZoomPanLayout scale.
     */
    public void setScale(float scale) {
    
        scale = getConstrainedDestinationScale(scale);
        
        if ( mScale == scale)
            return;
        
        float previous = mScale;
        mScale = scale;
        
        updateScaledDimensions();
        
        constrainScrollToLimits();
        onScaleChanged(scale, previous);
        
        invalidate();
    
    }
    
    /**
     * Set the scale of the ZoomPanLayout while maintaining the current center point.
     *
     * @param scale The new value of the ZoomPanLayout scale.
     */
    public void setScaleFromCenter(float scale) {
        setScaleFromPosition(getHalfWidth(), getHalfHeight(), scale);
    }
    
    public void setScaleFromPosition(int offsetX, int offsetY, float scale) {
    
        scale = getConstrainedDestinationScale(scale);
        
        if (scale == mScale)
            return;
        
        int x = getOffsetScrollXFromScale(offsetX, scale, mScale);
        int y = getOffsetScrollYFromScale(offsetY, scale, mScale);
        
        setScale(scale);
        
        x = getConstrainedScrollX(x);
        y = getConstrainedScrollY(y);
        
        scrollTo(x, y);
    
    }
    
    /**
     * Set minimum and maximum mScale values for this ZoomPanLayout.
     * Note that if minimumScaleMode is set to {@link #MINIMUM_SCALE_MODE_FIT} or {@link #MINIMUM_SCALE_MODE_FILL},
     * the minimum value set here will be ignored
     * Default values are 0 and 1.
     *
     * @param min Minimum scale the ZoomPanLayout should accept.
     * @param max Maximum scale the ZoomPanLayout should accept.
     */
    public void setScaleLimits(float min, float max) {
    
        mMinScale = min;
        mMaxScale = max;
        
        setScale(mScale);
    
    }
    
    /**
     * Determines whether the ZoomPanLayout should go back to minimum scale after a double-tap at
     * maximum scale.
     *
     * @param shouldLoopScale True to allow going back to minimum scale, false otherwise.
     */
    public void setShouldLoopScale(boolean shouldLoopScale) {
        mShouldLoopScale = shouldLoopScale;
    }
    
    /**
     * Determines whether the ZoomPanLayout should limit it's minimum scale to no less than what
     * would be required to fill it's container.
     *
     * @param shouldScaleToFit True to limit minimum scale, false to allow arbitrary minimum scale.
     */
    public void setShouldScaleToFit(boolean shouldScaleToFit) {
        setMinimumScaleMode(shouldScaleToFit ? MINIMUM_SCALE_MODE_FILL : MINIMUM_SCALE_MODE_NONE);
    }
    
    /**
     * Sets the size (width and height) of the ZoomPanLayout
     * as it should be rendered at a scale of 1f (100%).
     *
     * @param width  Width of the underlying image, not the view or viewport.
     * @param height Height of the underlying image, not the view or viewport.
     */
    public void setSize(int width, int height) {
    
        mBaseHeight = height;
        mBaseWidth = width;
        
        updateScaledDimensions();
        
        calculateMinimumScaleToFit();
        constrainScrollToLimits();
        
        requestLayout();
    
    }
    
    /**
     * Scrolls the ZoomPanLayout to the x and y values provided using scrolling animation.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     */
    public void slideTo(int x, int y) {
        getAnimator().animatePan(x, y);
    }
    
    /**
     * Scrolls and centers the ZoomPanLayout to the x and y values provided using scrolling animation.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     */
    public void slideToAndCenter(int x, int y) {
        slideTo((x - getHalfWidth()), (y - getHalfHeight()));
    }
    
    /**
     * Animates the ZoomPanLayout to the scale provided, and centers the viewport to the position supplied.
     *
     * @param x     Horizontal destination point.
     * @param y     Vertical destination point.
     * @param scale The final scale value the ZoomPanLayout should animate to.
     */
    public void slideToAndCenterWithScale(int x, int y, float scale) {
        getAnimator().animateZoomPan((x - getHalfWidth()), (y - getHalfHeight()), scale);
    }
    
    /**
     * Animate the scale of the ZoomPanLayout while maintaining the current center point.
     *
     * @param scale The final scale value the ZoomPanLayout should animate to.
     */
    public void smoothScaleFromCenter(float scale) {
        smoothScaleFromFocalPoint(getHalfWidth(), getHalfHeight(), scale);
    }
    
    /**
     * Animates the ZoomPanLayout to the scale provided, while maintaining position determined by
     * the focal point provided.
     *
     * @param focusX The horizontal focal point to maintain, relative to the screen (as supplied by MotionEvent.getX).
     * @param focusY The vertical focal point to maintain, relative to the screen (as supplied by MotionEvent.getY).
     * @param scale  The final scale value the ZoomPanLayout should animate to.
     */
    public void smoothScaleFromFocalPoint(int focusX, int focusY, float scale) {
    
        scale = getConstrainedDestinationScale(scale);
        
        if(scale == mScale)
            return;
        
        int x = getOffsetScrollXFromScale(focusX, scale, mScale);
        int y = getOffsetScrollYFromScale(focusY, scale, mScale);
        
        getAnimator().animateZoomPan(x, y, scale);
    
    }
    
    /**
     * Scales the ZoomPanLayout with animated progress, without maintaining scroll position.
     *
     * @param destination The final scale value the ZoomPanLayout should animate to.
     */
    public void smoothScaleTo(float destination) {
        getAnimator().animateZoom(destination);
    }
    
    private void updateScaledDimensions() {
    
        mScaledHeight = FloatMathHelper.scale(mBaseHeight, mScale);
        mScaledWidth = FloatMathHelper.scale(mBaseWidth, mScale);
    
    }
    
    private static class ZoomPanAnimator extends ValueAnimator implements Animator.AnimatorListener,
        ValueAnimator.AnimatorUpdateListener {
    
        private boolean mHasPendingPanUpdates, mHasPendingZoomUpdates;
        
        private WeakReference<ZoomPanLayout> mLayout;
        
        private ZoomPanState mEndState = new ZoomPanState();
        private ZoomPanState mStartState = new ZoomPanState();
        
        ZoomPanAnimator(ZoomPanLayout layout) {
            super();
            
            addListener(this);
            addUpdateListener(this);
            
            setFloatValues(0f, 1f);
            setInterpolator(new FastEaseInInterpolator());
            
            mLayout = new WeakReference<>(layout);
        
        }
        
        void animatePan(int x, int y) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return;
            
            mHasPendingPanUpdates = setupPanAnimation(x, y);
            
            if (mHasPendingPanUpdates)
                start();
        
        }
        
        void animateZoom(float scale) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return;
            
            mHasPendingZoomUpdates = setupZoomAnimation(scale);
            
            if (mHasPendingZoomUpdates)
                start();
        
        }
        
        void animateZoomPan(int x, int y, float scale) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return;
            
            mHasPendingPanUpdates = setupPanAnimation(x, y);
            mHasPendingZoomUpdates = setupZoomAnimation(scale);
            
            if (mHasPendingPanUpdates || mHasPendingZoomUpdates)
                start();
        
        }
        
        @Override
        public void onAnimationCancel(Animator animation) {
            onAnimationEnd(animation);
        }
        
        @Override
        public void onAnimationEnd(Animator animation) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return;
            
            if (mHasPendingPanUpdates) {
            
                mHasPendingPanUpdates = false;
                
                layout.mIsSliding = false;
                layout.broadcastProgrammaticPanEnd();
            
            }
            
            if (mHasPendingZoomUpdates) {
            
                mHasPendingZoomUpdates = false;
                
                layout.mIsScaling = false;
                layout.broadcastProgrammaticZoomEnd();
            
            }
        
        }
        
        @Override
        public void onAnimationRepeat(Animator animation) {}
        
        @Override
        public void onAnimationStart(Animator animation) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return;
            
            if (mHasPendingPanUpdates) {
            
                layout.mIsSliding = true;
                layout.broadcastProgrammaticPanBegin();
            
            }
            
            if (mHasPendingZoomUpdates) {
            
                layout.mIsScaling = true;
                layout.broadcastProgrammaticZoomBegin();
            
            }
        
        }
        
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return;
            
            float progress = (float) animation.getAnimatedValue();
            
            if (mHasPendingPanUpdates) {
            
                int x = (int) ((mStartState.x + (mEndState.x - mStartState.x)) * progress);
                int y = (int) ((mStartState.y + (mEndState.y - mStartState.y)) * progress);
                
                layout.scrollTo(x, y);
                layout.broadcastProgrammaticPanUpdate();
            
            }
            
            if (mHasPendingZoomUpdates) {
            
                float scale = ((mStartState.scale + (mEndState.scale - mStartState.scale)) * progress);
                
                layout.setScale(scale);
                layout.broadcastProgrammaticZoomEnd();
            
            }
        
        }
        
        private boolean setupPanAnimation(int x, int y) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return false;
            
            mEndState.x = x;
            mEndState.y = y;
            
            mStartState.x = layout.getScrollX();
            mStartState.y = layout.getScrollY();
            
            return ((mEndState.x != mStartState.x) || (mEndState.y != mStartState.y));
        
        }
        
        private boolean setupZoomAnimation(float scale) {
        
            ZoomPanLayout layout = mLayout.get();
            
            if (layout == null)
                return false;
            
            mEndState.scale = scale;
            mStartState.scale = layout.getScale();
            
            return (mEndState.scale != mStartState.scale);
        
        }
        
        private class FastEaseInInterpolator implements Interpolator {
        
            @Override
            public float getInterpolation(float input) {
                return (float) (1 - Math.pow((1 - input), 8));
            }
        
        }
        
        private class ZoomPanState {
        
            float scale;
            int x, y;
        
        }
    
    }
    
    public interface ZoomPanListener {
    
        String ORIGINATION_DRAG         = "origination_drag";
        String ORIGINATION_FLING        = "origination_fling";
        String ORIGINATION_PINCH        = "origination_pinch";
        String ORIGINATION_PROGRAMMATIC = "origination_programmatic";
        
        /** @hide **/
        @StringDef({ORIGINATION_DRAG, ORIGINATION_FLING, ORIGINATION_PINCH, ORIGINATION_PROGRAMMATIC})
        @Retention(RetentionPolicy.SOURCE)
        @interface Origination {}
        
        void onPanBegin(int x, int y, @Origination String origin);
        void onPanEnd(int x, int y, @Origination String origin);
        void onPanUpdate(int x, int y, @Origination String origin);
        
        void onZoomBegin(float scale, @Origination String origin);
        void onZoomEnd(float scale, @Origination String origin);
        void onZoomUpdate(float scale, @Origination String origin);
    
    }

}