package by.lwo.trafficpolice.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import androidx.appcompat.R
import androidx.appcompat.graphics.drawable.DrawableWrapper
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.widget.ListViewAutoScrollHelper
import java.lang.reflect.Field

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * Wrapper class for a ListView. This wrapper can hijack the focus to
 * make sure the list uses the appropriate drawables and states when
 * displayed on screen within a drop down. The focus is never actually
 * passed to the drop down in this mode; the list only looks focused.
 */
internal open class CustomDropDownList
/**
 *
 * Creates a new list view wrapper.
 *
 * @param context this view's context
 */
    (
    context: Context,
    /**
     * True if this wrapper should fake focus.
     */
    private val mHijackFocus: Boolean
) : ListView(context, null, R.attr.dropDownListViewStyle) {

    private val mSelectorRect = Rect()
    private var mSelectionLeftPadding = 0
    private var mSelectionTopPadding = 0
    private var mSelectionRightPadding = 0
    private var mSelectionBottomPadding = 0

    private var mMotionPosition: Int = 0

    private var mIsChildViewEnabled: Field? = null

    private var mSelector: GateKeeperDrawable? = null

    /*
    * WARNING: This is a workaround for a touch mode issue.
    *
    * Touch mode is propagated lazily to windows. This causes problems in
    * the following scenario:
    * - Type something in the AutoCompleteTextView and get some results
    * - Move down with the d-pad to select an item in the list
    * - Move up with the d-pad until the selection disappears
    * - Type more text in the AutoCompleteTextView *using the soft keyboard*
    *   and get new results; you are now in touch mode
    * - The selection comes back on the first item in the list, even though
    *   the list is supposed to be in touch mode
    *
    * Using the soft keyboard triggers the touch mode change but that change
    * is propagated to our window only after the first list layout, therefore
    * after the list attempts to resurrect the selection.
    *
    * The trick to work around this issue is to pretend the list is in touch
    * mode when we know that the selection should not appear, that is when
    * we know the user moved the selection away from the list.
    *
    * This boolean is set to true whenever we explicitly hide the list's
    * selection and reset to false whenever we know the user moved the
    * selection back to the list.
    *
    * When this boolean is true, isInTouchMode() returns true, otherwise it
    * returns super.isInTouchMode().
    */
    private var mListSelectionHidden: Boolean = false

    /** Whether to force drawing of the pressed state selector.  */
    private var mDrawsInPressedState: Boolean = false

    /** Current drag-to-open click animation, if any.  */
    private var mClickAnimation: ViewPropertyAnimatorCompat? = null

    /** Helper for drag-to-open auto scrolling.  */
    private var mScrollHelper: ListViewAutoScrollHelper? = null

    /**
     * Runnable posted when we are awaiting hover event resolution. When set,
     * drawable state changes are postponed.
     */
    private var mResolveHoverRunnable: ResolveHoverRunnable? = null

    init {
        cacheColorHint = 0 // Transparent, since the background drawable could be anything.

        try {
            mIsChildViewEnabled = AbsListView::class.java.getDeclaredField("mIsChildViewEnabled")
            mIsChildViewEnabled!!.isAccessible = true
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }
    }

    override fun isInTouchMode(): Boolean {
        // WARNING: Please read the comment where mListSelectionHidden is declared
        return mHijackFocus && mListSelectionHidden || super.isInTouchMode()
    }

    /**
     *
     * Returns the focus state in the drop down.
     *
     * @return true always if hijacking focus
     */
    override fun hasWindowFocus(): Boolean {
        return mHijackFocus || super.hasWindowFocus()
    }

    /**
     *
     * Returns the focus state in the drop down.
     *
     * @return true always if hijacking focus
     */
    override fun isFocused(): Boolean {
        return mHijackFocus || super.isFocused()
    }

    /**
     *
     * Returns the focus state in the drop down.
     *
     * @return true always if hijacking focus
     */
    override fun hasFocus(): Boolean {
        return mHijackFocus || super.hasFocus()
    }

    override fun setSelector(sel: Drawable?) {
        mSelector = if (sel != null) GateKeeperDrawable(sel) else null
        super.setSelector(mSelector)

        val padding = Rect()
        sel?.getPadding(padding)

        mSelectionLeftPadding = padding.left
        mSelectionTopPadding = padding.top
        mSelectionRightPadding = padding.right
        mSelectionBottomPadding = padding.bottom
    }

    override fun drawableStateChanged() {
        //postpone drawableStateChanged until pending hover to pressed transition finishes.
        if (mResolveHoverRunnable != null) {
            return
        }

        super.drawableStateChanged()

        setSelectorEnabled(true)
        updateSelectorStateCompat()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val drawSelectorOnTop = false
        if (!drawSelectorOnTop) {
            drawSelectorCompat(canvas)
        }

        super.dispatchDraw(canvas)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> mMotionPosition = pointToPosition(ev.x.toInt(), ev.y.toInt())
        }
        if (mResolveHoverRunnable != null) {
            // Resolved hover event as hover => touch transition.
            mResolveHoverRunnable!!.cancel()
        }
        return super.onTouchEvent(ev)
    }

    /**
     * Find a position that can be selected (i.e., is not a separator).
     *
     * @param position The starting position to look at.
     * @param lookDown Whether to look down for other positions.
     * @return The next selectable position starting at position and then searching either up or
     * down. Returns [.INVALID_POSITION] if nothing can be found.
     */
    fun lookForSelectablePosition(position: Int, lookDown: Boolean): Int {
        var position = position
        val adapter = adapter
        if (adapter == null || isInTouchMode) {
            return INVALID_POSITION
        }

        val count = adapter.count
        if (!getAdapter().areAllItemsEnabled()) {
            if (lookDown) {
                position = Math.max(0, position)
                while (position < count && !adapter.isEnabled(position)) {
                    position++
                }
            } else {
                position = Math.min(position, count - 1)
                while (position >= 0 && !adapter.isEnabled(position)) {
                    position--
                }
            }

            return if (position < 0 || position >= count) {
                INVALID_POSITION
            } else position
        } else {
            return if (position < 0 || position >= count) {
                INVALID_POSITION
            } else position
        }
    }

    /**
     * Measures the height of the given range of children (inclusive) and returns the height
     * with this ListView's padding and divider heights included. If maxHeight is provided, the
     * measuring will stop when the current height reaches maxHeight.
     *
     * @param widthMeasureSpec             The width measure spec to be given to a child's
     * [View.measure].
     * @param startPosition                The position of the first child to be shown.
     * @param endPosition                  The (inclusive) position of the last child to be
     * shown. Specify [.NO_POSITION] if the last child
     * should be the last available child from the adapter.
     * @param maxHeight                    The maximum height that will be returned (if all the
     * children don't fit in this value, this value will be
     * returned).
     * @param disallowPartialChildPosition In general, whether the returned height should only
     * contain entire children. This is more powerful--it is
     * the first inclusive position at which partial
     * children will not be allowed. Example: it looks nice
     * to have at least 3 completely visible children, and
     * in portrait this will most likely fit; but in
     * landscape there could be times when even 2 children
     * can not be completely shown, so a value of 2
     * (remember, inclusive) would be good (assuming
     * startPosition is 0).
     * @return The height of this ListView with the given children.
     */
    fun measureHeightOfChildrenCompat(
        widthMeasureSpec: Int, startPosition: Int,
        endPosition: Int, maxHeight: Int,
        disallowPartialChildPosition: Int
    ): Int {

        val paddingTop = listPaddingTop
        val paddingBottom = listPaddingBottom
        val paddingLeft = listPaddingLeft
        val paddingRight = listPaddingRight
        val reportedDividerHeight = dividerHeight
        val divider = divider

        val adapter = adapter ?: return paddingTop + paddingBottom

// Include the padding of the list
        var returnedHeight = paddingTop + paddingBottom
        val dividerHeight = if (reportedDividerHeight > 0 && divider != null)
            reportedDividerHeight
        else
            0

        // The previous height value that was less than maxHeight and contained
        // no partial children
        var prevHeightWithoutPartialChild = 0

        var child: View? = null
        var viewType = 0
        val count = adapter.count
        for (i in 0 until count) {
            val newType = adapter.getItemViewType(i)
            if (newType != viewType) {
                child = null
                viewType = newType
            }
            child = adapter.getView(i, child, this)

            // Compute child height spec
            val heightMeasureSpec: Int
            var childLp: ViewGroup.LayoutParams? = child!!.layoutParams

            if (childLp == null) {
                childLp = generateDefaultLayoutParams()
                child.layoutParams = childLp
            }

            if (childLp!!.height > 0) {
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    childLp.height,
                    View.MeasureSpec.EXACTLY
                )
            } else {
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            }
            child.measure(widthMeasureSpec, heightMeasureSpec)

            // Since this view was measured directly against the parent measure
            // spec, we must measure it again before reuse.
            child.forceLayout()

            if (i > 0) {
                // Count the divider for all but one child
                returnedHeight += dividerHeight
            }

            returnedHeight += child.measuredHeight

            if (returnedHeight >= maxHeight) {
                // We went over, figure out which height to return.  If returnedHeight >
                // maxHeight, then the i'th position did not fit completely.
                return if (disallowPartialChildPosition >= 0 // Disallowing is enabled (> -1)

                    && i > disallowPartialChildPosition // We've past the min pos

                    && prevHeightWithoutPartialChild > 0 // We have a prev height

                    && returnedHeight != maxHeight // i'th child did not fit completely
                )
                    prevHeightWithoutPartialChild
                else
                    maxHeight
            }

            if (disallowPartialChildPosition >= 0 && i >= disallowPartialChildPosition) {
                prevHeightWithoutPartialChild = returnedHeight
            }
        }

        // At this point, we went through the range of children, and they each
        // completely fit, so return the returnedHeight
        return returnedHeight
    }

    private fun setSelectorEnabled(enabled: Boolean) {
        if (mSelector != null) {
            mSelector!!.setEnabled(enabled)
        }
    }

    private class GateKeeperDrawable internal constructor(drawable: Drawable) : DrawableWrapper(drawable) {
        private var mEnabled: Boolean = false

        init {
            mEnabled = true
        }

        internal fun setEnabled(enabled: Boolean) {
            mEnabled = enabled
        }

        override fun setState(stateSet: IntArray): Boolean {
            return if (mEnabled) {
                super.setState(stateSet)
            } else false
        }

        override fun draw(canvas: Canvas) {
            if (mEnabled) {
                super.draw(canvas)
            }
        }

        override fun setHotspot(x: Float, y: Float) {
            if (mEnabled) {
                super.setHotspot(x, y)
            }
        }

        override fun setHotspotBounds(left: Int, top: Int, right: Int, bottom: Int) {
            if (mEnabled) {
                super.setHotspotBounds(left, top, right, bottom)
            }
        }

        override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
            return if (mEnabled) {
                super.setVisible(visible, restart)
            } else false
        }
    }

    override fun onHoverEvent(ev: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // For SDK_INT prior to O the code below fails to change the selection.
            // This is because prior to O mouse events used to enable touch mode, and
            //  View.setSelectionFromTop does not do the right thing in touch mode.
            return super.onHoverEvent(ev)
        }

        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_HOVER_EXIT && mResolveHoverRunnable == null) {
            // This may be transitioning to TOUCH_DOWN. Postpone drawable state
            // updates until either the next frame or the next touch event.
            mResolveHoverRunnable = ResolveHoverRunnable()
            mResolveHoverRunnable!!.post()
        }

        // Allow the super class to handle hover state management first.
        val handled = super.onHoverEvent(ev)
        if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE) {
            val position = pointToPosition(ev.x.toInt(), ev.y.toInt())

            if (position != INVALID_POSITION && position != selectedItemPosition) {
                val hoveredItem = getChildAt(position - firstVisiblePosition)
                if (hoveredItem.isEnabled) {
                    // Force a focus on the hovered item so that
                    // the proper selector state gets used when we update.
                    setSelectionFromTop(position, hoveredItem.top - this.top)
                }
                updateSelectorStateCompat()
            }
        } else {
            // Do not cancel the selected position if the selection is visible
            // by other means.
            setSelection(INVALID_POSITION)
        }

        return handled
    }

    override fun onDetachedFromWindow() {
        mResolveHoverRunnable = null
        super.onDetachedFromWindow()
    }

    /**
     * Handles forwarded events.
     *
     * @param activePointerId id of the pointer that activated forwarding
     * @return whether the event was handled
     */
    fun onForwardedEvent(event: MotionEvent, activePointerId: Int): Boolean {
        var handledEvent = true
        var clearPressedItem = false

        val actionMasked = event.actionMasked
        when (actionMasked) {
            MotionEvent.ACTION_CANCEL -> handledEvent = false
            MotionEvent.ACTION_UP -> {
                handledEvent = false
                val activeIndex = event.findPointerIndex(activePointerId)
                if (activeIndex < 0) {
                    handledEvent = false
                }

                val x = event.getX(activeIndex).toInt()
                val y = event.getY(activeIndex).toInt()
                val position = pointToPosition(x, y)
                if (position == INVALID_POSITION) {
                    clearPressedItem = true
                }

                val child = getChildAt(position - firstVisiblePosition)
                setPressedItem(child, position, x.toFloat(), y.toFloat())
                handledEvent = true

                if (actionMasked == MotionEvent.ACTION_UP) {
                    clickPressedItem(child, position)
                }
            }
            // $FALL-THROUGH$
            MotionEvent.ACTION_MOVE -> {
                val activeIndex = event.findPointerIndex(activePointerId)
                if (activeIndex < 0) {
                    handledEvent = false
                }
                val x = event.getX(activeIndex).toInt()
                val y = event.getY(activeIndex).toInt()
                val position = pointToPosition(x, y)
                if (position == INVALID_POSITION) {
                    clearPressedItem = true
                }
                val child = getChildAt(position - firstVisiblePosition)
                setPressedItem(child, position, x.toFloat(), y.toFloat())
                handledEvent = true
                if (actionMasked == MotionEvent.ACTION_UP) {
                    clickPressedItem(child, position)
                }
            }
        }

        // Failure to handle the event cancels forwarding.
        if (!handledEvent || clearPressedItem) {
            clearPressedItem()
        }

        // Manage automatic scrolling.
        if (handledEvent) {
            if (mScrollHelper == null) {
                mScrollHelper = ListViewAutoScrollHelper(this)
            }
            mScrollHelper!!.isEnabled = true
            mScrollHelper!!.onTouch(this, event)
        } else if (mScrollHelper != null) {
            mScrollHelper!!.isEnabled = false
        }

        return handledEvent
    }

    /**
     * Starts an alpha animation on the selector. When the animation ends,
     * the list performs a click on the item.
     */
    private fun clickPressedItem(child: View, position: Int) {
        val id = getItemIdAtPosition(position)
        performItemClick(child, position, id)
    }

    /**
     * Sets whether the list selection is hidden, as part of a workaround for a
     * touch mode issue (see the declaration for mListSelectionHidden).
     *
     * @param hideListSelection `true` to hide list selection,
     * `false` to show
     */
    fun setListSelectionHidden(hideListSelection: Boolean) {
        mListSelectionHidden = hideListSelection
    }

    private fun updateSelectorStateCompat() {
        val selector = selector
        if (selector != null && touchModeDrawsInPressedStateCompat() && isPressed) {
            selector.state = drawableState
        }
    }

    private fun drawSelectorCompat(canvas: Canvas) {
        if (!mSelectorRect.isEmpty) {
            val selector = selector
            if (selector != null) {
                selector.bounds = mSelectorRect
                selector.draw(canvas)
            }
        }
    }

    private fun positionSelectorLikeTouchCompat(position: Int, sel: View, x: Float, y: Float) {
        positionSelectorLikeFocusCompat(position, sel)

        val selector = selector
        if (selector != null && position != INVALID_POSITION) {
            DrawableCompat.setHotspot(selector, x, y)
        }
    }

    private fun positionSelectorLikeFocusCompat(position: Int, sel: View) {
        // If we're changing position, update the visibility since the selector
        // is technically being detached from the previous selection.
        val selector = selector
        val manageState = selector != null && position != INVALID_POSITION
        if (manageState) {
            selector!!.setVisible(false, false)
        }

        positionSelectorCompat(position, sel)

        if (manageState) {
            val bounds = mSelectorRect
            val x = bounds.exactCenterX()
            val y = bounds.exactCenterY()
            selector!!.setVisible(visibility == View.VISIBLE, false)
            DrawableCompat.setHotspot(selector, x, y)
        }
    }

    private fun positionSelectorCompat(position: Int, sel: View) {
        val selectorRect = mSelectorRect
        selectorRect.set(sel.left, sel.top, sel.right, sel.bottom)

        // Adjust for selection padding.
        selectorRect.left -= mSelectionLeftPadding
        selectorRect.top -= mSelectionTopPadding
        selectorRect.right += mSelectionRightPadding
        selectorRect.bottom += mSelectionBottomPadding

        try {
            // AbsListView.mIsChildViewEnabled controls the selector's state so we need to
            // modify its value
            val isChildViewEnabled = mIsChildViewEnabled!!.getBoolean(this)
            if (sel.isEnabled != isChildViewEnabled) {
                mIsChildViewEnabled!!.set(this, !isChildViewEnabled)
                if (position != INVALID_POSITION) {
                    refreshDrawableState()
                }
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private fun clearPressedItem() {
        mDrawsInPressedState = false
        isPressed = false
        // This will call through to updateSelectorState()
        drawableStateChanged()

        val motionView = getChildAt(mMotionPosition - firstVisiblePosition)
        if (motionView != null) {
            motionView.isPressed = false
        }

        if (mClickAnimation != null) {
            mClickAnimation!!.cancel()
            mClickAnimation = null
        }
    }

    private fun setPressedItem(child: View, position: Int, x: Float, y: Float) {
        mDrawsInPressedState = true

        // Ordering is essential. First, update the container's pressed state.
        if (Build.VERSION.SDK_INT >= 21) {
            drawableHotspotChanged(x, y)
        }
        if (!isPressed) {
            isPressed = true
        }

        // Next, run layout to stabilize child positions.
        layoutChildren()

        // Manage the pressed view based on motion position. This allows us to
        // play nicely with actual touch and scroll events.
        if (mMotionPosition != INVALID_POSITION) {
            val motionView = getChildAt(mMotionPosition - firstVisiblePosition)
            if (motionView != null && motionView !== child && motionView.isPressed) {
                motionView.isPressed = false
            }
        }
        mMotionPosition = position

        // Offset for child coordinates.
        val childX = x - child.left
        val childY = y - child.top
        if (Build.VERSION.SDK_INT >= 21) {
            child.drawableHotspotChanged(childX, childY)
        }
        if (!child.isPressed) {
            child.isPressed = true
        }

        // Ensure that keyboard focus starts from the last touched position.
        positionSelectorLikeTouchCompat(position, child, x, y)

        // This needs some explanation. We need to disable the selector for this next call
        // due to the way that ListViewCompat works. Otherwise both ListView and ListViewCompat
        // will draw the selector and bad things happen.
        setSelectorEnabled(false)

        // Refresh the drawable state to reflect the new pressed state,
        // which will also update the selector state.
        refreshDrawableState()
    }

    private fun touchModeDrawsInPressedStateCompat(): Boolean {
        return mDrawsInPressedState
    }

    /**
     * Runnable that forces hover event resolution and updates drawable state.
     */
    private inner class ResolveHoverRunnable internal constructor() : Runnable {

        override fun run() {
            // Resolved hover event as standard hover exit.
            mResolveHoverRunnable = null
            drawableStateChanged()
        }

        fun cancel() {
            mResolveHoverRunnable = null
            removeCallbacks(this)
        }

        fun post() {
            this@CustomDropDownList.post(this)
        }
    }

    companion object {
        val INVALID_POSITION = -1
        val NO_POSITION = -1
    }
}
