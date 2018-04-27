package com.github.teocci.av.editor.views;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Scroller;

import com.github.teocci.av.editor.R;
import com.github.teocci.av.editor.views.VideoThumbHorizontalListView.OnScrollStateChangedListener.ScrollState;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static com.github.teocci.av.editor.views.VideoThumbHorizontalListView.OnScrollStateChangedListener.ScrollState.SCROLL_STATE_IDLE;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-19
 */

public class VideoThumbHorizontalListView extends AdapterView<ListAdapter>
{
    private static final String TAG = VideoThumbHorizontalListView.class.getSimpleName();

    /**
     * Defines where to insert items into the ViewGroup, as defined in
     * {@code ViewGroup#addViewInLayout(View, int, LayoutParams, boolean)}
     */
    private static final int INSERT_AT_END_OF_LIST = -1;
    private static final int INSERT_AT_START_OF_LIST = 0;

    /**
     * The velocity to use for over scroll absorption
     */
    private static final float FLING_DEFAULT_ABSORB_VELOCITY = 30f;

    /**
     * The friction amount to use for the fling tracker
     */
    private static final float FLING_FRICTION = 0.009f;

    /**
     * Tracks ongoing flings
     */
    private Scroller flingTracker = new Scroller(getContext());

    /**
     * Holds a reference to the adapter bounds to this view
     */
    private ListAdapter listAdapter;

    /**
     * Holds a cache of recycled views to be reused as needed
     */
    private List<Queue<View>> removedViewsCache = new ArrayList<>();

    /**
     * The x position of the currently rendered view
     */
    protected int currentX;

    /**
     * The x position of the next to be rendered view
     */
    protected int nextX;

    /**
     * Tracks the starting layout position of the leftmost view
     */
    private int displayOffset;

    /**
     * The adapter index of the leftmost view currently visible
     */
    private int leftViewAdapterIndex;

    /**
     * The adapter index of the rightmost view currently visible
     */
    private int rightViewAdapterIndex;

    /**
     * Tracks the currently selected accessibility item
     */
    private int currentlySelectedAdapterIndex;

    /**
     * Tracks the maximum possible X position, stays at max value until last item is laid out and it can be determined
     */
    private int maxX = Integer.MAX_VALUE;

    /**
     * Temporary rectangle to be used for measurements
     */
    private Rect rect = new Rect();

    /**
     * The width of the divider that will be used between list items
     */
    private int dividerWidth = 0;

    /**
     * The drawable that will be used as the list divider
     */
    private Drawable divider = null;

    /**
     * Gesture listener to receive callback when gestures are detected
     */
    private final GestureListener gestureListener = new GestureListener();

    /**
     * Used for detecting gestures within this view so they can be handled
     */
    private GestureDetector gestureDetector;

    /**
     * Tracks the state of left edge glow
     */
    private EdgeEffectCompat edgeGlowLeft;

    /**
     * Tracks the state of right edge glow
     */
    private EdgeEffectCompat edgeGlowRight;

    /**
     * The height measure specific for this view, used to help size the child views
     */
    private int heightMeasureSpec;

    /**
     * Flag used to mark the adapter data has changed so the view can be relaid out
     */
    private boolean dataChanged;

    /**
     * Callback interface to be invoked when scroll state has changed
     */
    private OnScrollStateChangedListener scrollStateChangedListener = null;

    /***
     * Represent current scroll state. Needed when scroll state has changed so scroll listener can notify a change.
     */
    private ScrollState currentScrollState = SCROLL_STATE_IDLE;

    /**
     * Used to schedule a request layout via a runnable
     */
    private Runnable delayedLayout = () -> requestLayout();

    /**
     * DataSetObserver used to capture adapter data change events
     */
    private DataSetObserver adapterDataObserver = new DataSetObserver()
    {
        @Override
        public void onChanged()
        {
            dataChanged = true;

            // Invalidate and request layout to force this view to completely redraw itself
            invalidate();
            requestLayout();
        }

        @Override
        public void onInvalidated()
        {
            // Clear so we can notify again as we run out of data
            reset();

            // Invalidate and request layout to force this view to completely redraw itself
            invalidate();
            requestLayout();
        }
    };


    public VideoThumbHorizontalListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        edgeGlowLeft = new EdgeEffectCompat(context);
        edgeGlowRight = new EdgeEffectCompat(context);
        gestureDetector = new GestureDetector(context, gestureListener);
        bindGestureDetector();
        initView();
        retrieveXmlConfiguration(context, attrs);
        setWillNotDraw(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            HoneyCombPlus.setFriction(flingTracker, FLING_FRICTION);
        }
    }

    /**
     * Register gesture detector to receive gesture notifications for this view.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void bindGestureDetector()
    {
        // Generic touch listener that can be applied to any view that needs to process gestures.
        final OnTouchListener gestureListenerHandler = (v, event) -> {
            // Delegate the touch event to our gesture detector.
            return gestureDetector.onTouchEvent(event);
        };
        setOnTouchListener(gestureListenerHandler);
    }

    private void initView()
    {
        currentX = 0;
        nextX = 0;
        displayOffset = 0;
        leftViewAdapterIndex = -1;
        rightViewAdapterIndex = -1;
        maxX = Integer.MAX_VALUE;
        setCurrentScrollState(SCROLL_STATE_IDLE);
    }

    /**
     * Parse the XML configuration for this widget
     *
     * @param context Context used for extracting attributes
     * @param attrs   The Attribute Set containing the ColumnView attributes
     */
    private void retrieveXmlConfiguration(Context context, AttributeSet attrs)
    {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HorizontalListView);

            // Get the provided drawable from the XML
            final Drawable d = a.getDrawable(R.styleable.HorizontalListView_android_divider);
            if (d != null) {
                // If a drawable is provided to use as the divider then use its intrinsic width for the divider width
                setDivider(d);
            }

            // If a width is explicitly specified then use that width
            final int dividerWidth = a.getDimensionPixelSize(R.styleable.HorizontalListView_hori_listview_dividerWidth, 0);
            if (dividerWidth != 0) {
                setDividerWidth(dividerWidth);
            }

            a.recycle();
        }
    }

    /**
     * Sets the drawable that will be drawn between each item in the list. If the drawable does
     * not have an intrinsic width, you should also call {@link #setDividerWidth(int)}
     *
     * @param divider The drawable to use.
     */
    public void setDivider(Drawable divider)
    {
        this.divider = divider;

        if (divider != null) {
            setDividerWidth(divider.getIntrinsicWidth());
        } else {
            setDividerWidth(0);
        }
    }

    /**
     * Sets the width of the divider that will be drawn between each item in the list. Calling
     * this will override the intrinsic width as set by {@link #setDivider(Drawable)}
     *
     * @param width The width of the divider in pixels.
     */
    public void setDividerWidth(int width)
    {
        dividerWidth = width;

        // Force the view to re-render itself
        requestLayout();
        invalidate();
    }

    @Override
    public void setSelection(int position)
    {
        currentlySelectedAdapterIndex = position;
    }

    @Override
    public View getSelectedView()
    {
        return getChild(currentlySelectedAdapterIndex);
    }

    @Override
    public ListAdapter getAdapter()
    {
        return listAdapter;
    }

    @Override
    public void setAdapter(ListAdapter adapter)
    {
        if (listAdapter != null) {
            listAdapter.unregisterDataSetObserver(adapterDataObserver);
        }

        if (adapter != null) {
            listAdapter = adapter;
            listAdapter.registerDataSetObserver(adapterDataObserver);
            initializeRemovedViewCache(listAdapter.getViewTypeCount());
        }
        reset();
    }

    /**
     * Will create and initialize a cache for the given number of different type of views
     *
     * @param viewTypeCount The total number of different views supported
     */
    private void initializeRemovedViewCache(int viewTypeCount)
    {
        // The cache is created such that the response from listAdapter.getItemViewType is the array index
        // to the correct cache for that item.
        removedViewsCache.clear();
        for (int i = 0; i < viewTypeCount; i++) {
            removedViewsCache.add(new LinkedList<View>());
        }
    }

    /**
     * Will re-initialize the HorizontalListView to remove all the child views rendered and
     * reset to initial configuration
     */
    private void reset()
    {
        initView();
        removeAllViewsInLayout();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Cache off the measure spec
        this.heightMeasureSpec = heightMeasureSpec;
    }

    @SuppressLint("WrongCall")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);

        if (listAdapter == null) {
            return;
        }

        if (dataChanged) {
            int oldCurrentX = currentX;
            initView();
            removeAllViewsInLayout();
            nextX = oldCurrentX;
            dataChanged = false;
        }

        // If in a fling
        if (flingTracker.computeScrollOffset()) {
            // Compute the next position
            nextX = flingTracker.getCurrX();
        }

        // Prevent from scrolling past 0 so you can not scroll past the end of list to the left
        if (nextX < 0) {
            nextX = 0;

            // Show an edge effect absorbing the current velocity
            if (edgeGlowLeft.isFinished()) {
                edgeGlowLeft.onAbsorb((int) determineFlingAbsorbVelocity());
            }
            flingTracker.forceFinished(true);
            setCurrentScrollState(SCROLL_STATE_IDLE);
        }

        if (nextX > maxX) {
            nextX = maxX;

            if (edgeGlowRight.isFinished()) {
                edgeGlowRight.onAbsorb((int) determineFlingAbsorbVelocity());
            }
            flingTracker.forceFinished(true);
            setCurrentScrollState(SCROLL_STATE_IDLE);
        }

        int dx = currentX - nextX;
        removeNonVisibleChildren(dx);
        fillList(dx);
        positionChildren(dx);

        // Since the view has now been drawn, update the currentX
        currentX = nextX;

        // If we have scrolled enough to lay out all views, then determine the maximum scroll position now
        if (determineMaxX()) {
            // Redo the layout pass since we now know the maximum scroll position
            onLayout(changed, l, r, t, b);
            return;
        }

        if (flingTracker.isFinished()) {
            // If fling just ended
            if (currentScrollState == OnScrollStateChangedListener.ScrollState.SCROLL_STATE_FLING) {
                setCurrentScrollState(SCROLL_STATE_IDLE);
            }
        } else {
            ViewCompat.postOnAnimation(this, delayedLayout);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        super.dispatchDraw(canvas);
        drawEdgeGlow(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        drawDividers(canvas);
    }

    private void drawDividers(Canvas canvas)
    {
        final int childCount = getChildCount();
        final Rect bound = rect;
        bound.top = getPaddingTop();
        bound.bottom = getRenderHeight() + getPaddingTop();

        for (int i = 0; i < childCount; i++) {
            // Don't draw a divider to the right of the last item in the adapter
            if (!(i == childCount - 1 && isLastItemInAdapter(rightViewAdapterIndex))) {
                View child = getChildAt(i);
                bound.left = child.getRight();
                bound.right = bound.left + dividerWidth;

//                // Clip at the left edge of the screen
//                if (bound.left < getPaddingLeft()) {
//                    bound.left = getPaddingLeft();
//                }
//
//                // Clip at the right edge of the screen
//                if (bound.right > getWidth() - getPaddingRight()) {
//                    bound.right = getWidth() - getPaddingRight();
//                }

                // Draw a divider to the right of the child
                drawDivider(canvas, bound);

                // If the first view, determine if a divider should be shown to the left of it.
                // A divider should be shown if the left side of this view does not fill to the left edge of the screen.
                if (i == 0 && child.getLeft() > getPaddingLeft()) {
                    bound.left = getPaddingLeft();
                    bound.right = child.getLeft();
                    drawDivider(canvas, bound);
                }
            }
        }
    }

    private void drawDivider(Canvas canvas, Rect bound)
    {
        if (divider != null) {
            divider.setBounds(bound);
            divider.draw(canvas);
        }
    }

    /**
     * Determines the current fling absorb velocity
     */
    private float determineFlingAbsorbVelocity()
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return IceCreamSandwichPlus.getCurrVelocity(flingTracker);
        } else {
            // Unable to get the velocity so just return a default.
            // In actuality this is never used since EdgeEffectCompat does not draw anything unless the device is ICS+.
            // Less then ICS EdgeEffectCompat essentially performs a NOP.
            return FLING_DEFAULT_ABSORB_VELOCITY;
        }
    }

    private void removeNonVisibleChildren(final int dx)
    {
        View child = getLeftmostChild();

        // Loop removing the leftmost child, until that child is bound to on the screen
        while (child != null && child.getRight() + dx <= 0) {
            // The child is being completely removed so remove its width from the display offset and its divider if it has one.
            // To remove add the size of the child and its divider (if it has one) to the offset.
            // You need to add since its being removed from the left side, i.e. shifting the offset to the right.
            displayOffset += isLastItemInAdapter(leftViewAdapterIndex) ?
                    child.getMeasuredWidth() : dividerWidth + child.getMeasuredWidth();

            // Recycle the removed view
            recycleView(leftViewAdapterIndex, child);

            // Keep track of the adapter index of the leftmost child
            leftViewAdapterIndex++;

            // Actually remove the child
            removeViewInLayout(child);

            child = getLeftmostChild();
        }

        child = getRightmostChild();
        // Loop removing the rightmost child, until that child is bound to on the screen
        while (child != null && child.getLeft() + dx >= getWidth()) {
            recycleView(rightViewAdapterIndex, child);
            rightViewAdapterIndex--;
            removeViewInLayout(child);
            child = getRightmostChild();
        }
    }

    /**
     * Adds child views to the left and right of current view until the screen if full of views
     */
    private void fillList(final int dx)
    {
        int edge = 0;
        View child = getRightmostChild();

        if (child != null) {
            edge = child.getRight();
        }
        // Adds child views to the right, until past the edge of the screen
        fillListRight(edge, dx);

        edge = 0;
        child = getLeftmostChild();
        if (child != null) {
            edge = child.getLeft();
        }
        // Adds child views to the left, until past the edge of the screen
        fillListLeft(edge, dx);
    }

    private void fillListLeft(int leftEdge, final int dx)
    {
        while (leftEdge + dx > 0 && leftViewAdapterIndex >= 1) {
            leftViewAdapterIndex--;
            View child = listAdapter.getView(leftViewAdapterIndex, getRecycledView(leftViewAdapterIndex), this);
            addAndMeasureChild(child, INSERT_AT_START_OF_LIST);
            leftEdge -= leftViewAdapterIndex == 0 ? child.getMeasuredWidth() : dividerWidth + child.getMeasuredWidth();
            // If on a clean edge then just remove the child, otherwise remove the divider as well
            displayOffset -= leftEdge + dx == 0 ? child.getMeasuredWidth() : dividerWidth + child.getMeasuredWidth();
        }
    }

    private void fillListRight(int rightEdge, final int dx)
    {
        while (rightEdge + dx < getWidth() && rightViewAdapterIndex + 1 < listAdapter.getCount()) {
            rightViewAdapterIndex++;
            // If leftViewAdapterIndex < 0 then this is the first time a view is being added, and left == right
            if (leftViewAdapterIndex < 0) {
                leftViewAdapterIndex = rightViewAdapterIndex;
            }
            View child = listAdapter.getView(rightViewAdapterIndex, getRecycledView(rightViewAdapterIndex), this);
            addAndMeasureChild(child, INSERT_AT_END_OF_LIST);
            rightEdge += (rightViewAdapterIndex == 0 ? 0 : dividerWidth) + child.getMeasuredWidth();
        }

    }

    private void positionChildren(final int dx)
    {
        int childCount = getChildCount();
        int leftOffset = displayOffset;
        displayOffset += dx;
        // Loop each child view
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int left = leftOffset + getPaddingLeft();
            int top = getPaddingTop();
            int right = left + child.getMeasuredWidth();
            int bottom = top + child.getMeasuredHeight();
            // Layout the child
            child.layout(left, top, right, bottom);

            leftOffset += child.getMeasuredWidth() + dividerWidth;
        }
    }

    /**
     * Determine the Max X position. This is the farthest that the user can scroll the screen.
     * Until the last adapter item has been laid out it is impossible to calculate; once that has
     * occurred this will perform the calculation, and if necessary force a redraw and relayout
     * of this view.
     *
     * @return true if the maxx position was just determined
     */
    private boolean determineMaxX()
    {
        // If the last view has been laid out, then we can determine the maximum x position
        if (isLastItemInAdapter(rightViewAdapterIndex)) {
            View rightView = getRightmostChild();

            if (rightView != null) {
                int oldMaxX = maxX;
                maxX = currentX + (rightView.getRight() - getPaddingLeft()) - getRenderWidth();

                if (maxX < 0) {
                    maxX = 0;
                }

                if (oldMaxX != maxX) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Simple convenience method for determining if this index is the last index in the adapter
     */
    private boolean isLastItemInAdapter(int index)
    {
        return index == listAdapter.getCount() - 1;
    }

    /**
     * Gets the height in px this view will be rendered. (padding removed)
     */
    private int getRenderHeight()
    {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    /**
     * Gets the width in px this view will be rendered. (padding removed)
     */
    private int getRenderWidth()
    {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Draws the overscroll edge glow effect on the left and right sides of the horizontal list
     */
    private void drawEdgeGlow(Canvas canvas)
    {
        if (edgeGlowLeft != null && !edgeGlowLeft.isFinished() && isEdgeGlowEnabled()) {
            // The Edge glow is meant to come from the top of the screen, so rotate it to draw on the left side.
            final int restoreCount = canvas.save();
            final int height = getHeight();

            canvas.rotate(-90, 0, 0);
            canvas.translate(-height + getPaddingBottom(), 0);

            edgeGlowLeft.setSize(getRenderHeight(), getRenderWidth());
            if (edgeGlowLeft.draw(canvas)) {
                invalidate();
            }

            canvas.restoreToCount(restoreCount);
        } else if (edgeGlowRight != null && !edgeGlowRight.isFinished() && isEdgeGlowEnabled()) {
            // The Edge glow is meant to come from the top of the screen, so rotate it to draw on the right side.
            final int restoreCount = canvas.save();
            final int width = getWidth();

            canvas.rotate(90, 0, 0);
            canvas.translate(getPaddingTop(), -width);
            edgeGlowRight.setSize(getRenderHeight(), getRenderWidth());
            if (edgeGlowRight.draw(canvas)) {
                invalidate();
            }

            canvas.restoreToCount(restoreCount);
        }
    }

    /**
     * Checks if the edge glow should be used enabled.
     * The glow is not enabled unless there are more views than can fit on the screen at one time.
     */
    private boolean isEdgeGlowEnabled()
    {
        if (listAdapter == null || listAdapter.isEmpty()) return false;

        // If the maxx is more then zero then the user can scroll, so the edge effects should be shown
        return maxX > 0;
    }

    /**
     * Gets the leftmost child that is on the screen
     */
    private View getLeftmostChild()
    {
        return getChildAt(0);
    }

    /**
     * Gets the rightmost child that is on the screen
     */
    private View getRightmostChild()
    {
        return getChildAt(getChildCount() - 1);
    }

    /**
     * Finds a child that is contained within this view, given the adapter index.
     *
     * @return The child view or null if not found
     */
    private View getChild(int adapterIndex)
    {
        if (adapterIndex >= leftViewAdapterIndex && adapterIndex <= rightViewAdapterIndex) {
            return getChildAt(adapterIndex - leftViewAdapterIndex);
        }
        return null;
    }

    private void recycleView(int adapterIndex, View child)
    {
        int itemViewType = listAdapter.getItemViewType(adapterIndex);
        // There is one Queue of views for each different type of view.
        // Just add the view to the pile of other views of the same type.
        // The order they are added and removed does not matter.

        if (isItemViewTypeValid(itemViewType)) {
            removedViewsCache.get(itemViewType).offer(child);
        }
    }

    private View getRecycledView(int adapterIndex)
    {
        int itemViewType = listAdapter.getItemViewType(adapterIndex);

        if (isItemViewTypeValid(itemViewType)) {
            return removedViewsCache.get(itemViewType).poll();
        }
        return null;
    }

    private boolean isItemViewTypeValid(int itemViewType)
    {
        return itemViewType < removedViewsCache.size();
    }

    /**
     * Adds child to this view group and measures it so it renders the right size
     */
    private void addAndMeasureChild(View child, int viewPos)
    {
        LayoutParams params = getLayoutParams(child);
        addViewInLayout(child, viewPos, params, true);
        measureChild(child);
    }

    /**
     * Measure the provided child
     */
    private void measureChild(View child)
    {
        LayoutParams layoutParams = getLayoutParams(child);
        int heightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), layoutParams.height);

        int widthSpec;
        if (layoutParams.width > 0) {
            widthSpec = MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY);
        } else {
            widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        child.measure(widthSpec, heightSpec);
    }

    private LayoutParams getLayoutParams(View child)
    {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        }
        return params;
    }

    protected boolean onDown(MotionEvent e)
    {
        // Allow a finger down event to catch a fling
        flingTracker.forceFinished(true);
        setCurrentScrollState(SCROLL_STATE_IDLE);
        return true;
    }

    protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        flingTracker.fling(nextX, 0, (int) -velocityX, 0, 0, maxX, 0, 0);
        setCurrentScrollState(OnScrollStateChangedListener.ScrollState.SCROLL_STATE_FLING);
        requestLayout();
        Log.i(TAG, "print after onScroll  currentX  = " + currentX);
        return true;
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener
    {

        @Override
        public boolean onDown(MotionEvent e)
        {
            return VideoThumbHorizontalListView.this.onDown(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            return VideoThumbHorizontalListView.this.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            nextX += distanceX;
            updateOverScrollAnimation(Math.round(distanceX));
            setCurrentScrollState(OnScrollStateChangedListener.ScrollState.SCROLL_STATE_TOUCH_SCROLL);
            requestLayout();
            return true;
        }
    }

    /**
     * Return the current scroll x
     */
    public int getCurrentX()
    {
        return currentX;
    }

    public interface OnScrollStateChangedListener
    {
        enum ScrollState
        {
            /**
             * The view is not scrolling. Note navigating the list using trackball counts as being in the
             * idle state since these transitions are not animated.
             */
            SCROLL_STATE_IDLE,

            /**
             * The user is scrolling using touch, and their fingers are still on the screen.
             */
            SCROLL_STATE_TOUCH_SCROLL,

            /**
             * The user had previously been scrolling using touch and had performed a fling. The animation
             * is now coasting to stop.
             */
            SCROLL_STATE_FLING
        }

        /**
         * Callback method to be invoked when the scroll state changes.
         *
         * @param scrollState The current scroll state
         */
        void onScrollStateChanged(ScrollState scrollState, int scrollDirection);
    }

    /**
     * Sets a listener to be invoked when the scroll state has changed.
     *
     * @param listener The listener to be invoked
     */
    public void setOnScrollStateChangedListener(OnScrollStateChangedListener listener)
    {
        scrollStateChangedListener = listener;
    }

    /**
     * Call to set a new state
     * If it has changed and a listener is registered then it will be notified.
     */

    private int _scrolledOffset = 0;

    public void setCurrentScrollState(OnScrollStateChangedListener.ScrollState newScrollState)
    {
        Log.i("Jason", "setCurrentScrollState  newScrollState  = " + newScrollState);
        Log.i("Jason", "setCurrentScrollState  currentScrollState  = " + currentScrollState);
        if (scrollStateChangedListener != null) {
            scrollStateChangedListener.onScrollStateChanged(newScrollState, _scrolledOffset);
        }
        currentScrollState = newScrollState;
    }

    /**
     * Updates the over scroll animation based on the scrolled offset.
     *
     * @param scrolledOffset The scroll offset
     */
    private void updateOverScrollAnimation(final int scrolledOffset)
    {
        if (edgeGlowLeft == null || edgeGlowRight == null) return;

        // Calculate where the next scroll position would be
        _scrolledOffset = scrolledOffset;

        int nextScrollPosition = currentX + scrolledOffset;

        Log.i("Jason", "test scrolledOffset = " + scrolledOffset);
        Log.i("Jason", "test currentX  = " + currentX);
        Log.i("Jason", "test nextScrollPosition  = " + nextScrollPosition);

        // If not currently in a fling (Don't want to allow fling offset updates to cause over scroll animation)
        if (flingTracker == null || flingTracker.isFinished()) {
            // If currently scrolled off the left side of the list and the adapter is not empty
            if (nextScrollPosition < 0) {

                // Calculate the amount we have scrolled since last frame
                int overscroll = Math.abs(scrolledOffset);

                // Tell the edge glow to redraw itself at the new offset
                edgeGlowLeft.onPull((float) overscroll / getRenderWidth());

                // Cancel animating right glow
                if (!edgeGlowRight.isFinished()) {
                    edgeGlowRight.onRelease();
                }
            } else if (nextScrollPosition > maxX) {
                // Scrolled off the right of the list

                // Calculate the amount we have scrolled since last frame
                int overscroll = Math.abs(scrolledOffset);

                // Tell the edge glow to redraw itself at the new offset
                edgeGlowRight.onPull((float) overscroll / getRenderWidth());

                // Cancel animating left glow
                if (!edgeGlowLeft.isFinished()) {
                    edgeGlowLeft.onRelease();
                }
            }
        }
    }

    @TargetApi(11)
    /**
     * Wrapper class to protect access to api version is 11 and above.
     */
    private static final class HoneyCombPlus
    {
        static {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                throw new RuntimeException("Should not get HoneyCombPlus class unless sdk is >= 11!");
            }
        }

        /**
         * Sets the friction to the provided scroller.
         */
        public static void setFriction(Scroller scroller, float friction)
        {
            if (scroller != null) {
                scroller.setFriction(friction);
            }
        }
    }

    @TargetApi(14)
    /**
     * Wrapper class to protect access to api version is 14 and above.
     */
    private static final class IceCreamSandwichPlus
    {
        static {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                throw new RuntimeException("Should not get IceCreamSandwichPlus class unless sdk is >= 14");
            }
        }

        /**
         * Gets the current velocity from the provided scroller.
         */
        public static float getCurrVelocity(Scroller scroller)
        {
            if (scroller != null) {
                return scroller.getCurrVelocity();
            }
            return 0;
        }
    }
}
