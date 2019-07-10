package by.lwo.trafficpolice.ui.widgets

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.PopupWindow
import androidx.annotation.AttrRes
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import androidx.annotation.StyleRes
import androidx.appcompat.R
import androidx.core.view.ViewCompat
import androidx.core.widget.PopupWindowCompat
import java.lang.reflect.Method

/**
 * Static library support version of the framework's [android.widget.ListPopupWindow].
 * Used to write apps that run on platforms prior to Android L. When running
 * on Android L or above, this implementation is still used; it does not try
 * to switch to the framework's implementation. See the framework SDK
 * documentation for a class overview.
 *
 * @see android.widget.ListPopupWindow
 */
open class CustomListPopup
/**
 * Create a new, empty popup window capable of displaying items from a ListAdapter.
 * Backgrounds should be set using [.setBackgroundDrawable].
 *
 * @param context Context used for contained views.
 * @param attrs Attributes from inflating parent views used to style the popup.
 * @param defStyleAttr Style attribute to read for default styling of popup content.
 * @param defStyleRes Style resource ID to use for default styling of popup content.
 */
@JvmOverloads constructor(
    private val mContext: Context, attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.listPopupWindowStyle, @StyleRes defStyleRes: Int = 0
) {
    private var mAdapter: ListAdapter? = null
    internal var mDropDownList: CustomDropDownList? = null

    /**
     * @return The height of the popup window in pixels.
     */
    /**
     * Sets the height of the popup window in pixels. Can also be [.MATCH_PARENT].
     *
     * @param height Height of the popup window must be a positive value,
     * [.MATCH_PARENT], or [.WRAP_CONTENT].
     *
     * @throws IllegalArgumentException if height is set to negative value
     */
    var height = ViewGroup.LayoutParams.WRAP_CONTENT
        set(height) {
            if (height < 0 && ViewGroup.LayoutParams.WRAP_CONTENT != height
                && ViewGroup.LayoutParams.MATCH_PARENT != height
            ) {
                throw IllegalArgumentException(
                    "Invalid height. Must be a positive value, MATCH_PARENT, or WRAP_CONTENT."
                )
            }
            field = height
        }
    /**
     * @return The width of the popup window in pixels.
     */
    /**
     * Sets the width of the popup window in pixels. Can also be [.MATCH_PARENT]
     * or [.WRAP_CONTENT].
     *
     * @param width Width of the popup window.
     */
    var width = ViewGroup.LayoutParams.WRAP_CONTENT
    /**
     * @return The horizontal offset of the popup from its anchor in pixels.
     */
    /**
     * Set the horizontal offset of this popup from its anchor view in pixels.
     *
     * @param offset The horizontal offset of the popup from its anchor.
     */
    var horizontalOffset: Int = 0
    private var mDropDownVerticalOffset: Int = 0
    private var mDropDownWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL
    private var mDropDownVerticalOffsetSet: Boolean = false
    private val mIsAnimatedFromAnchor = true
    private var mOverlapAnchor: Boolean = false
    private var mOverlapAnchorSet: Boolean = false

    private var mDropDownGravity = Gravity.NO_GRAVITY

    /**
     * @return Whether the drop-down is visible under special conditions.
     *
     * @hide Only used by AutoCompleteTextView under special conditions.
     */
    /**
     * Sets whether the drop-down should remain visible under certain conditions.
     *
     * The drop-down will occupy the entire screen below [.getAnchorView] regardless
     * of the size or content of the list.  [.getBackground] will fill any space
     * that is not used by the list.
     *
     * @param dropDownAlwaysVisible Whether to keep the drop-down visible.
     *
     * @hide Only used by AutoCompleteTextView under special conditions.
     */
    @get:RestrictTo(LIBRARY_GROUP_PREFIX)
    @set:RestrictTo(LIBRARY_GROUP_PREFIX)
    var isDropDownAlwaysVisible = false
    private var mForceIgnoreOutsideTouch = false
    internal var mListItemExpandMaximum = Integer.MAX_VALUE

    private var mPromptView: View? = null
    /**
     * @return Where the optional prompt view should appear.
     *
     * @see .POSITION_PROMPT_ABOVE
     *
     * @see .POSITION_PROMPT_BELOW
     */
    /**
     * Set where the optional prompt view should appear. The default is
     * [.POSITION_PROMPT_ABOVE].
     *
     * @param position A position constant declaring where the prompt should be displayed.
     *
     * @see .POSITION_PROMPT_ABOVE
     *
     * @see .POSITION_PROMPT_BELOW
     */
    var promptPosition = POSITION_PROMPT_ABOVE

    private var mObserver: DataSetObserver? = null

    /**
     * Returns the view that will be used to anchor this popup.
     *
     * @return The popup's anchor view
     */
    /**
     * Sets the popup's anchor view. This popup will always be positioned relative to
     * the anchor view when shown.
     *
     * @param anchor The view to use as an anchor.
     */
    var anchorView: View? = null

    private var mDropDownListHighlight: Drawable? = null

    private var mItemClickListener: AdapterView.OnItemClickListener? = null
    private var mItemSelectedListener: OnItemSelectedListener? = null

    private val mResizePopupRunnable = ResizePopupRunnable()
    private val mTouchInterceptor = PopupTouchInterceptor()
    private val mScrollListener = PopupScrollListener()
    private val mHideSelector = ListSelectorHider()
    private var mShowDropDownRunnable: Runnable? = null

    internal val mHandler: Handler

    private val mTempRect = Rect()

    /**
     * Optional anchor-relative bounds to be used as the transition epicenter.
     * When `null`, the anchor bounds are used as the epicenter.
     */
    private var mEpicenterBounds: Rect? = null

    /**
     * Returns whether the popup window will be modal when shown.
     *
     * @return `true` if the popup window will be modal, `false` otherwise.
     */
    /**
     * Set whether this window should be modal when shown.
     *
     *
     * If a popup window is modal, it will receive all touch and key input.
     * If the user touches outside the popup window's content area the popup window
     * will be dismissed.
     *
     * @param modal `true` if the popup window should be modal, `false` otherwise.
     */
    var isModal: Boolean = false
        set(modal) {
            field = modal
            mPopup!!.isFocusable = modal
        }

    var mPopup: PopupWindow? = null

    /**
     * Returns the current value in [.setSoftInputMode].
     *
     * @see .setSoftInputMode
     * @see android.view.WindowManager.LayoutParams.softInputMode
     */
    /**
     * Sets the operating mode for the soft input area.
     *
     * @param mode The desired mode, see
     * [android.view.WindowManager.LayoutParams.softInputMode]
     * for the full list
     *
     * @see android.view.WindowManager.LayoutParams.softInputMode
     *
     * @see .getSoftInputMode
     */
    var softInputMode: Int
        get() = mPopup!!.softInputMode
        set(mode) {
            mPopup!!.softInputMode = mode
        }

    /**
     * @return The background drawable for the popup window.
     */
    val background: Drawable?
        get() = mPopup!!.background

    /**
     * Returns the animation style that will be used when the popup window is
     * shown or dismissed.
     *
     * @return Animation style that will be used.
     */
    /**
     * Set an animation style to use when the popup window is shown or dismissed.
     *
     * @param animationStyle Animation style to use.
     */
    var animationStyle: Int
        @StyleRes get() = mPopup!!.animationStyle
        set(@StyleRes animationStyle) {
            mPopup!!.animationStyle = animationStyle
        }

    /**
     * @return The vertical offset of the popup from its anchor in pixels.
     */
    /**
     * Set the vertical offset of this popup from its anchor view in pixels.
     *
     * @param offset The vertical offset of the popup from its anchor.
     */
    var verticalOffset: Int
        get() = if (!mDropDownVerticalOffsetSet) {
            0
        } else mDropDownVerticalOffset
        set(offset) {
            mDropDownVerticalOffset = offset
            mDropDownVerticalOffsetSet = true
        }

    /**
     * Return custom anchor-relative bounds of the popup's transition epicenter
     *
     * @return anchor-relative bounds, or @`null` if not set
     * @see .setEpicenterBounds
     */
    /**
     * Specifies the custom anchor-relative bounds of the popup's transition
     * epicenter.
     *
     * @param bounds anchor-relative bounds or `null` to use default epicenter
     * @see .getEpicenterBounds
     */
    var epicenterBounds: Rect?
        get() = if (mEpicenterBounds != null) Rect(mEpicenterBounds) else null
        set(bounds) {
            mEpicenterBounds = if (bounds != null) Rect(bounds) else null
        }

    /**
     * Return the current value in [.setInputMethodMode].
     *
     * @see .setInputMethodMode
     */
    /**
     * Control how the popup operates with an input method: one of
     * [.INPUT_METHOD_FROM_FOCUSABLE], [.INPUT_METHOD_NEEDED],
     * or [.INPUT_METHOD_NOT_NEEDED].
     *
     *
     * If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to the [.show]
     * method.
     *
     * @see .getInputMethodMode
     * @see .show
     */
    var inputMethodMode: Int
        get() = mPopup!!.inputMethodMode
        set(mode) {
            mPopup!!.inputMethodMode = mode
        }

    /**
     * @return `true` if this popup is configured to assume the user does not need
     * to interact with the IME while it is showing, `false` otherwise.
     */
    val isInputMethodNotNeeded: Boolean
        get() = mPopup!!.inputMethodMode == INPUT_METHOD_NOT_NEEDED

    /**
     * @return The currently selected item or null if the popup is not showing.
     */
    val selectedItem: Any?
        get() {
            return if (!isShowing) {
                null
            } else mDropDownList!!.getSelectedItem()
        }

    /**
     * @return The position of the currently selected item or [ListView.INVALID_POSITION]
     * if [.isShowing] == `false`.
     *
     * @see ListView.getSelectedItemPosition
     */
    val selectedItemPosition: Int
        get() {
            return if (!isShowing) {
                ListView.INVALID_POSITION
            } else mDropDownList!!.getSelectedItemPosition()
        }

    /**
     * @return The ID of the currently selected item or [ListView.INVALID_ROW_ID]
     * if [.isShowing] == `false`.
     *
     * @see ListView.getSelectedItemId
     */
    val selectedItemId: Long
        get() {
            return if (!isShowing) {
                ListView.INVALID_ROW_ID
            } else mDropDownList!!.getSelectedItemId()
        }

    /**
     * @return The View for the currently selected item or null if
     * [.isShowing] == `false`.
     *
     * @see ListView.getSelectedView
     */
    val selectedView: View?
        get() {
            return if (!isShowing) {
                null
            } else mDropDownList!!.getSelectedView()
        }

    init {
        mHandler = Handler(mContext.mainLooper)

        val a = mContext.obtainStyledAttributes(
            attrs, R.styleable.ListPopupWindow,
            defStyleAttr, defStyleRes
        )
        horizontalOffset = a.getDimensionPixelOffset(
            R.styleable.ListPopupWindow_android_dropDownHorizontalOffset, 0
        )
        mDropDownVerticalOffset = a.getDimensionPixelOffset(
            R.styleable.ListPopupWindow_android_dropDownVerticalOffset, 0
        )
        if (mDropDownVerticalOffset != 0) {
            mDropDownVerticalOffsetSet = true
        }
        a.recycle()

        mPopup = PopupWindow(mContext, attrs, defStyleAttr, defStyleRes)
        mPopup!!.inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
    }

    /**
     * Sets the adapter that provides the data and the views to represent the data
     * in this popup window.
     *
     * @param adapter The adapter to use to create this window's content.
     */
    open fun setAdapter(adapter: ListAdapter?) {
        if (mObserver == null) {
            mObserver = PopupDataSetObserver()
        } else if (mAdapter != null) {
            mAdapter!!.unregisterDataSetObserver(mObserver)
        }
        mAdapter = adapter
        if (adapter != null) {
            adapter.registerDataSetObserver(mObserver)
        }

        if (mDropDownList != null) {
            mDropDownList!!.setAdapter(mAdapter)
        }
    }

    /**
     * Forces outside touches to be ignored. Normally if [.isDropDownAlwaysVisible] is
     * false, we allow outside touch to dismiss the dropdown. If this is set to true, then we
     * ignore outside touch even when the drop down is not set to always visible.
     *
     * @hide Used only by AutoCompleteTextView to handle some internal special cases.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    fun setForceIgnoreOutsideTouch(forceIgnoreOutsideTouch: Boolean) {
        mForceIgnoreOutsideTouch = forceIgnoreOutsideTouch
    }

    /**
     * Sets a drawable to use as the list item selector.
     *
     * @param selector List selector drawable to use in the popup.
     */
    fun setListSelector(selector: Drawable) {
        mDropDownListHighlight = selector
    }

    /**
     * Sets a drawable to be the background for the popup window.
     *
     * @param d A drawable to set as the background.
     */
    fun setBackgroundDrawable(d: Drawable?) {
        mPopup!!.setBackgroundDrawable(d)
    }

    /**
     * Set the gravity of the dropdown list. This is commonly used to
     * set gravity to START or END for alignment with the anchor.
     *
     * @param gravity Gravity value to use
     */
    fun setDropDownGravity(gravity: Int) {
        mDropDownGravity = gravity
    }

    /**
     * Sets the width of the popup window by the size of its content. The final width may be
     * larger to accommodate styled window dressing.
     *
     * @param width Desired width of content in pixels.
     */
    fun setContentWidth(width: Int) {
        val popupBackground = mPopup!!.background
        if (popupBackground != null) {
            popupBackground.getPadding(mTempRect)
            this.width = mTempRect.left + mTempRect.right + width
        } else {
            this.width = width
        }
    }

    /**
     * Set the layout type for this popup window.
     *
     *
     * See [WindowManager.LayoutParams.type] for possible values.
     *
     * @param layoutType Layout type for this window.
     *
     * @see WindowManager.LayoutParams.type
     */
    fun setWindowLayoutType(layoutType: Int) {
        mDropDownWindowLayoutType = layoutType
    }

    /**
     * Sets a listener to receive events when a list item is clicked.
     *
     * @param clickListener Listener to register
     *
     * @see ListView.setOnItemClickListener
     */
    fun setOnItemClickListener(clickListener: AdapterView.OnItemClickListener) {
        mItemClickListener = clickListener
    }

    /**
     * Sets a listener to receive events when a list item is selected.
     *
     * @param selectedListener Listener to register.
     *
     * @see ListView.setOnItemSelectedListener
     */
    fun setOnItemSelectedListener(selectedListener: OnItemSelectedListener?) {
        mItemSelectedListener = selectedListener
    }

    /**
     * Set a view to act as a user prompt for this popup window. Where the prompt view will appear
     * is controlled by [.setPromptPosition].
     *
     * @param prompt View to use as an informational prompt.
     */
    fun setPromptView(prompt: View?) {
        val showing = isShowing
        if (showing) {
            removePromptView()
        }
        mPromptView = prompt
        if (showing) {
            show()
        }
    }

    /**
     * Post a [.show] call to the UI thread.
     */
    fun postShow() {
        mHandler.post(mShowDropDownRunnable)
    }

    /**
     * Show the popup list. If the list is already showing, this method
     * will recalculate the popup's size and position.
     */
    fun show() {
        val height = buildDropDown()

        val noInputMethod = isInputMethodNotNeeded
        PopupWindowCompat.setWindowLayoutType(mPopup!!, mDropDownWindowLayoutType)

        if (mPopup!!.isShowing) {
            if (!ViewCompat.isAttachedToWindow(anchorView!!)) {
                //Don't update position if the anchor view is detached from window.
                return
            }
            val widthSpec: Int
            if (width == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The call to PopupWindow's update method below can accept -1 for any
                // value you do not want to update.
                widthSpec = -1
            } else if (width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                widthSpec = anchorView!!.width
            } else {
                widthSpec = width
            }

            val heightSpec: Int
            if (this.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The call to PopupWindow's update method below can accept -1 for any
                // value you do not want to update.
                heightSpec = if (noInputMethod) height else ViewGroup.LayoutParams.MATCH_PARENT
                if (noInputMethod) {
                    mPopup!!.width = if (width == ViewGroup.LayoutParams.MATCH_PARENT)
                        ViewGroup.LayoutParams.MATCH_PARENT
                    else
                        0
                    mPopup!!.height = 0
                } else {
                    mPopup!!.width = if (width == ViewGroup.LayoutParams.MATCH_PARENT)
                        ViewGroup.LayoutParams.MATCH_PARENT
                    else
                        0
                    mPopup!!.height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            } else if (this.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                heightSpec = height
            } else {
                heightSpec = this.height
            }

            mPopup!!.isOutsideTouchable = !mForceIgnoreOutsideTouch && !isDropDownAlwaysVisible

            mPopup!!.update(
                anchorView, horizontalOffset,
                mDropDownVerticalOffset, if (widthSpec < 0) -1 else widthSpec,
                if (heightSpec < 0) -1 else heightSpec
            )
        } else {
            val widthSpec: Int
            if (width == ViewGroup.LayoutParams.MATCH_PARENT) {
                widthSpec = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                if (width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    widthSpec = anchorView!!.width
                } else {
                    widthSpec = width
                }
            }

            val heightSpec: Int
            if (this.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                heightSpec = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                if (this.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    heightSpec = height
                } else {
                    heightSpec = this.height
                }
            }

            mPopup!!.width = widthSpec
            mPopup!!.height = heightSpec
            setPopupClipToScreenEnabled(true)

            // use outside touchable to dismiss drop down when touching outside of it, so
            // only set this if the dropdown is not always visible
            mPopup!!.isOutsideTouchable = !mForceIgnoreOutsideTouch && !isDropDownAlwaysVisible
            mPopup!!.setTouchInterceptor(mTouchInterceptor)
            if (mOverlapAnchorSet) {
                PopupWindowCompat.setOverlapAnchor(mPopup!!, mOverlapAnchor)
            }
            if (sSetEpicenterBoundsMethod != null) {
                try {
                    sSetEpicenterBoundsMethod!!.invoke(mPopup, mEpicenterBounds)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not invoke setEpicenterBounds on PopupWindow", e)
                }
            }
            PopupWindowCompat.showAsDropDown(
                mPopup!!, anchorView!!, horizontalOffset,
                mDropDownVerticalOffset, mDropDownGravity
            )
            mDropDownList!!.setSelection(ListView.INVALID_POSITION)

            if (!isModal || mDropDownList!!.isInTouchMode()) {
                clearListSelection()
            }
            if (!isModal) {
                mHandler.post(mHideSelector)
            }
        }
    }

    /**
     * Dismiss the popup window.
     */
    fun dismiss() {
        mPopup!!.dismiss()
        removePromptView()
        mPopup!!.contentView = null
        mDropDownList = null
        mHandler.removeCallbacks(mResizePopupRunnable)
    }

    /**
     * Set a listener to receive a callback when the popup is dismissed.
     *
     * @param listener Listener that will be notified when the popup is dismissed.
     */
    fun setOnDismissListener(listener: PopupWindow.OnDismissListener?) {
        mPopup!!.setOnDismissListener(listener)
    }

    private fun removePromptView() {
        if (mPromptView != null) {
            val parent = mPromptView!!.parent
            if (parent is ViewGroup) {
                parent.removeView(mPromptView)
            }
        }
    }

    /**
     * Set the selected position of the list.
     * Only valid when [.isShowing] == `true`.
     *
     * @param position List position to set as selected.
     */
    fun setSelection(position: Int) {
        val list = mDropDownList
        if (isShowing && list != null) {
            list.setListSelectionHidden(false)
            list.setSelection(position)

            if (list.getChoiceMode() != ListView.CHOICE_MODE_NONE) {
                list.setItemChecked(position, true)
            }
        }
    }

    /**
     * Clear any current list selection.
     * Only valid when [.isShowing] == `true`.
     */
    fun clearListSelection() {
        val list = mDropDownList
        if (list != null) {
            // WARNING: Please read the comment where mListSelectionHidden is declared
            list.setListSelectionHidden(true)
            //list.hideSelector();
            list.requestLayout()
        }
    }

    /**
     * @return `true` if the popup is currently showing, `false` otherwise.
     */
    val isShowing: Boolean
        get() = mPopup!!.isShowing

    /**
     * Perform an item click operation on the specified list adapter position.
     *
     * @param position Adapter position for performing the click
     * @return true if the click action could be performed, false if not.
     * (e.g. if the popup was not showing, this method would return false.)
     */
    fun performItemClick(position: Int): Boolean {
        if (isShowing) {
            if (mItemClickListener != null) {
                val list = mDropDownList
                val child = list!!.getChildAt(position - list.getFirstVisiblePosition())
                val adapter = list.getAdapter()
                mItemClickListener!!.onItemClick(list, child, position, adapter.getItemId(position))
            }
            return true
        }
        return false
    }

    /**
     * @return The [ListView] displayed within the popup window.
     * Only valid when [.isShowing] == `true`.
     */
    fun getListView(): ListView? {
        return mDropDownList
    }

    internal open fun createDropDownListView(context: Context, hijackFocus: Boolean): CustomDropDownList {
        return CustomDropDownList(context, hijackFocus)
    }

    /**
     * The maximum number of list items that can be visible and still have
     * the list expand when touched.
     *
     * @param max Max number of items that can be visible and still allow the list to expand.
     */
    internal fun setListItemExpandMax(max: Int) {
        mListItemExpandMaximum = max
    }

    /**
     * Filter key down events. By forwarding key down events to this function,
     * views using non-modal ListPopupWindow can have it handle key selection of items.
     *
     * @param keyCode keyCode param passed to the host view's onKeyDown
     * @param event event param passed to the host view's onKeyDown
     * @return true if the event was handled, false if it was ignored.
     *
     * @see .setModal
     * @see .onKeyUp
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // when the drop down is shown, we drive it directly
        if (isShowing) {
            // the key events are forwarded to the list in the drop down view
            // note that ListView handles space but we don't want that to happen
            // also if selection is not currently in the drop down, then don't
            // let center or enter presses go there since that would cause it
            // to select one of its items
            if (keyCode != KeyEvent.KEYCODE_SPACE && (mDropDownList!!.getSelectedItemPosition() >= 0 || !isConfirmKey(
                    keyCode
                ))
            ) {
                val curIndex = mDropDownList!!.getSelectedItemPosition()
                val consumed: Boolean

                val below = !mPopup!!.isAboveAnchor

                val adapter = mAdapter

                val allEnabled: Boolean
                var firstItem = Integer.MAX_VALUE
                var lastItem = Integer.MIN_VALUE

                if (adapter != null) {
                    allEnabled = adapter.areAllItemsEnabled()
                    firstItem = if (allEnabled)
                        0
                    else
                        mDropDownList!!.lookForSelectablePosition(0, true)
                    lastItem = if (allEnabled)
                        adapter.count - 1
                    else
                        mDropDownList!!.lookForSelectablePosition(adapter.count - 1, false)
                }

                if (below && keyCode == KeyEvent.KEYCODE_DPAD_UP && curIndex <= firstItem || !below && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && curIndex >= lastItem) {
                    // When the selection is at the top, we block the key
                    // event to prevent focus from moving.
                    clearListSelection()
                    mPopup!!.inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                    show()
                    return true
                } else {
                    // WARNING: Please read the comment where mListSelectionHidden
                    //          is declared
                    mDropDownList!!.setListSelectionHidden(false)
                }

                consumed = mDropDownList!!.onKeyDown(keyCode, event)
                if (DEBUG) Log.v(TAG, "Key down: code=$keyCode list consumed=$consumed")

                if (consumed) {
                    // If it handled the key event, then the user is
                    // navigating in the list, so we should put it in front.
                    mPopup!!.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
                    // Here's a little trick we need to do to make sure that
                    // the list view is actually showing its focus indicator,
                    // by ensuring it has focus and getting its window out
                    // of touch mode.
                    mDropDownList!!.requestFocusFromTouch()
                    show()

                    when (keyCode) {
                        // avoid passing the focus from the text view to the
                        // next component
                        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP -> return true
                    }
                } else {
                    if (below && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        // when the selection is at the bottom, we block the
                        // event to avoid going to the next focusable widget
                        if (curIndex == lastItem) {
                            return true
                        }
                    } else if (!below && keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                        curIndex == firstItem
                    ) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Filter key up events. By forwarding key up events to this function,
     * views using non-modal ListPopupWindow can have it handle key selection of items.
     *
     * @param keyCode keyCode param passed to the host view's onKeyUp
     * @param event event param passed to the host view's onKeyUp
     * @return true if the event was handled, false if it was ignored.
     *
     * @see .setModal
     * @see .onKeyDown
     */
    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isShowing && mDropDownList!!.getSelectedItemPosition() >= 0) {
            val consumed = mDropDownList!!.onKeyUp(keyCode, event)
            if (consumed && isConfirmKey(keyCode)) {
                // if the list accepts the key events and the key event was a click, the text view
                // gets the selected item from the drop down as its content
                dismiss()
            }
            return consumed
        }
        return false
    }

    /**
     * Filter pre-IME key events. By forwarding [View.onKeyPreIme]
     * events to this function, views using ListPopupWindow can have it dismiss the popup
     * when the back key is pressed.
     *
     * @param keyCode keyCode param passed to the host view's onKeyPreIme
     * @param event event param passed to the host view's onKeyPreIme
     * @return true if the event was handled, false if it was ignored.
     *
     * @see .setModal
     */
    fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isShowing) {
            // special case for the back key, we do not even try to send it
            // to the drop down list but instead, consume it immediately
            val anchorView = this.anchorView
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val state = anchorView!!.keyDispatcherState
                if (state != null) {
                    state.startTracking(event, this)
                }
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val state = anchorView!!.keyDispatcherState
                if (state != null) {
                    state.handleUpEvent(event)
                }
                if (event.isTracking && !event.isCanceled) {
                    dismiss()
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns an [OnTouchListener] that can be added to the source view
     * to implement drag-to-open behavior. Generally, the source view should be
     * the same view that was passed to [.setAnchorView].
     *
     *
     * When the listener is set on a view, touching that view and dragging
     * outside of its bounds will open the popup window. Lifting will select the
     * currently touched list item.
     *
     *
     * Example usage:
     * <pre>
     * ListPopupWindow myPopup = new ListPopupWindow(context);
     * myPopup.setAnchor(myAnchor);
     * OnTouchListener dragListener = myPopup.createDragToOpenListener(myAnchor);
     * myAnchor.setOnTouchListener(dragListener);
    </pre> *
     *
     * @param src the view on which the resulting listener will be set
     * @return a touch listener that controls drag-to-open behavior
     */

    /**
     *
     * Builds the popup window's content and returns the height the popup
     * should have. Returns -1 when the content already exists.
     *
     * @return the content's height or -1 if content already exists
     */
    private fun buildDropDown(): Int {
        var dropDownView: ViewGroup
        var otherHeights = 0

        if (mDropDownList == null) {
            val context = mContext

            /**
             * This Runnable exists for the sole purpose of checking if the view layout has got
             * completed and if so call showDropDown to display the drop down. This is used to show
             * the drop down as soon as possible after user opens up the search dialog, without
             * waiting for the normal UI pipeline to do its job which is slower than this method.
             */
            mShowDropDownRunnable = Runnable {
                // View layout should be all done before displaying the drop down.
                val view = anchorView
                if (view != null && view!!.windowToken != null) {
                    show()
                }
            }

            mDropDownList = createDropDownListView(context, !isModal)
            if (mDropDownListHighlight != null) {
                mDropDownList!!.setSelector(mDropDownListHighlight)
            }
            mDropDownList!!.adapter = mAdapter
            mDropDownList!!.onItemClickListener = mItemClickListener
            mDropDownList!!.isFocusable = true
            mDropDownList!!.isFocusableInTouchMode = true
            mDropDownList!!.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View,
                    position: Int, id: Long
                ) {

                    if (position != -1) {
                        val dropDownList = mDropDownList

                        if (dropDownList != null) {
                            dropDownList.setListSelectionHidden(false)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            mDropDownList!!.setOnScrollListener(mScrollListener)

            if (mItemSelectedListener != null) {
                mDropDownList!!.setOnItemSelectedListener(mItemSelectedListener)
            }

            dropDownView = mDropDownList as CustomDropDownList

            val hintView = mPromptView
            if (hintView != null) {
                // if a hint has been specified, we accommodate more space for it and
                // add a text view in the drop down menu, at the bottom of the list
                val hintContainer = LinearLayout(context)
                hintContainer.orientation = LinearLayout.VERTICAL

                var hintParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
                )

                when (promptPosition) {
                    POSITION_PROMPT_BELOW -> {
                        hintContainer.addView(dropDownView, hintParams)
                        hintContainer.addView(hintView)
                    }

                    POSITION_PROMPT_ABOVE -> {
                        hintContainer.addView(hintView)
                        hintContainer.addView(dropDownView, hintParams)
                    }

                    else -> Log.e(TAG, "Invalid hint position $promptPosition")
                }

                // Measure the hint's height to find how much more vertical
                // space we need to add to the drop down's height.
                val widthSize: Int
                val widthMode: Int
                if (width >= 0) {
                    widthMode = MeasureSpec.AT_MOST
                    widthSize = width
                } else {
                    widthMode = MeasureSpec.UNSPECIFIED
                    widthSize = 0
                }
                val widthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode)
                val heightSpec = MeasureSpec.UNSPECIFIED
                hintView.measure(widthSpec, heightSpec)

                hintParams = hintView.layoutParams as LinearLayout.LayoutParams
                otherHeights = (hintView.measuredHeight + hintParams.topMargin
                    + hintParams.bottomMargin)

                dropDownView = hintContainer
            }

            mPopup!!.contentView = dropDownView
        } else {
            dropDownView = mPopup!!.contentView as ViewGroup
            val view = mPromptView
            if (view != null) {
                val hintParams = view.layoutParams as LinearLayout.LayoutParams
                otherHeights = (view.measuredHeight + hintParams.topMargin
                    + hintParams.bottomMargin)
            }
        }

        // getMaxAvailableHeight() subtracts the padding, so we put it back
        // to get the available height for the whole window.
        val padding: Int
        val background = mPopup!!.background
        if (background != null) {
            background.getPadding(mTempRect)
            padding = mTempRect.top + mTempRect.bottom

            // If we don't have an explicit vertical offset, determine one from
            // the window background so that content will line up.
            if (!mDropDownVerticalOffsetSet) {
                mDropDownVerticalOffset = -mTempRect.top
            }
        } else {
            mTempRect.setEmpty()
            padding = 0
        }

        // Max height available on the screen for a popup.
        val ignoreBottomDecorations = mPopup!!.inputMethodMode == PopupWindow.INPUT_METHOD_NOT_NEEDED
        val maxHeight = getMaxAvailableHeight(
            anchorView, mDropDownVerticalOffset,
            ignoreBottomDecorations
        )
        if (isDropDownAlwaysVisible || height == ViewGroup.LayoutParams.MATCH_PARENT) {
            return maxHeight + padding
        }

        val childWidthSpec: Int
        when (width) {
            ViewGroup.LayoutParams.WRAP_CONTENT -> childWidthSpec = MeasureSpec.makeMeasureSpec(
                mContext.resources.displayMetrics.widthPixels - (mTempRect.left + mTempRect.right),
                MeasureSpec.AT_MOST
            )
            ViewGroup.LayoutParams.MATCH_PARENT -> childWidthSpec = MeasureSpec.makeMeasureSpec(
                mContext.resources.displayMetrics.widthPixels - (mTempRect.left + mTempRect.right),
                MeasureSpec.EXACTLY
            )
            else -> childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        }

        // Add padding only if the list has items in it, that way we don't show
        // the popup if it is not needed.
        val listContent = mDropDownList!!.measureHeightOfChildrenCompat(
            childWidthSpec,
            0, CustomDropDownList.NO_POSITION, maxHeight - otherHeights, -1
        )
        if (listContent > 0) {
            val listPadding = mDropDownList!!.getPaddingTop() + mDropDownList!!.getPaddingBottom()
            otherHeights += padding + listPadding
        }

        return listContent + otherHeights
    }

    /**
     * @hide Only used by [androidx.appcompat.view.menu.CascadingMenuPopup] to position
     * a submenu correctly.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    fun setOverlapAnchor(overlapAnchor: Boolean) {
        mOverlapAnchorSet = true
        mOverlapAnchor = overlapAnchor
    }

    private inner class PopupDataSetObserver internal constructor() : DataSetObserver() {

        override fun onChanged() {
            if (isShowing) {
                // Resize the popup to fit new content
                show()
            }
        }

        override fun onInvalidated() {
            dismiss()
        }
    }

    private inner class ListSelectorHider internal constructor() : Runnable {

        override fun run() {
            clearListSelection()
        }
    }

    private inner class ResizePopupRunnable internal constructor() : Runnable {

        override fun run() {
            if (mDropDownList != null && ViewCompat.isAttachedToWindow(mDropDownList!!)
                && mDropDownList!!.getCount() > mDropDownList!!.getChildCount()
                && mDropDownList!!.getChildCount() <= mListItemExpandMaximum
            ) {
                mPopup!!.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
                show()
            }
        }
    }

    private inner class PopupTouchInterceptor internal constructor() : OnTouchListener {

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val action = event.action
            val x = event.x.toInt()
            val y = event.y.toInt()

            if (action == MotionEvent.ACTION_DOWN &&
                mPopup != null && mPopup!!.isShowing &&
                x >= 0 && x < mPopup!!.width && y >= 0 && y < mPopup!!.height
            ) {
                mHandler.postDelayed(mResizePopupRunnable, EXPAND_LIST_TIMEOUT.toLong())
            } else if (action == MotionEvent.ACTION_UP) {
                mHandler.removeCallbacks(mResizePopupRunnable)
            }
            return false
        }
    }

    private inner class PopupScrollListener internal constructor() : AbsListView.OnScrollListener {

        override fun onScroll(
            view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
            totalItemCount: Int
        ) {
        }

        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL &&
                !isInputMethodNotNeeded && mPopup!!.contentView != null
            ) {
                mHandler.removeCallbacks(mResizePopupRunnable)
                mResizePopupRunnable.run()
            }
        }
    }

    private fun setPopupClipToScreenEnabled(clip: Boolean) {
        if (sClipToWindowEnabledMethod != null) {
            try {
                sClipToWindowEnabledMethod!!.invoke(mPopup, clip)
            } catch (e: Exception) {
                Log.i(TAG, "Could not call setClipToScreenEnabled() on PopupWindow. Oh well.")
            }
        }
    }

    private fun getMaxAvailableHeight(anchor: View?, yOffset: Int, ignoreBottomDecorations: Boolean): Int {
        if (sGetMaxAvailableHeightMethod != null) {
            try {
                return sGetMaxAvailableHeightMethod!!.invoke(
                    mPopup, anchor, yOffset,
                    ignoreBottomDecorations
                ) as Int
            } catch (e: Exception) {
                Log.i(
                    TAG,
                    "Could not call getMaxAvailableHeightMethod(View, int, boolean)" + " on PopupWindow. Using the public version."
                )
            }
        }
        return mPopup!!.getMaxAvailableHeight(anchor!!, yOffset)
    }

    companion object {
        private val TAG = "ListPopupWindow"
        private val DEBUG = false

        /**
         * This value controls the length of time that the user
         * must leave a pointer down without scrolling to expand
         * the autocomplete dropdown list to cover the IME.
         */
        internal val EXPAND_LIST_TIMEOUT = 250

        private var sClipToWindowEnabledMethod: Method? = null
        private var sGetMaxAvailableHeightMethod: Method? = null
        private var sSetEpicenterBoundsMethod: Method? = null

        init {
            try {
                sClipToWindowEnabledMethod = PopupWindow::class.java.getDeclaredMethod(
                    "setClipToScreenEnabled", Boolean::class.javaPrimitiveType!!
                )
            } catch (e: NoSuchMethodException) {
                Log.i(TAG, "Could not find method setClipToScreenEnabled() on PopupWindow. Oh well.")
            }

            try {

                sGetMaxAvailableHeightMethod = PopupWindow::class.java.getDeclaredMethod(
                    "getMaxAvailableHeight",
                    View::class.java,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType
                )
            } catch (e: NoSuchMethodException) {
                Log.i(
                    TAG,
                    "Could not find method getMaxAvailableHeight(View, int, boolean)" + " on PopupWindow. Oh well."
                )
            }

            try {
                sSetEpicenterBoundsMethod = PopupWindow::class.java.getDeclaredMethod(
                    "setEpicenterBounds", Rect::class.java
                )
            } catch (e: NoSuchMethodException) {
                Log.i(TAG, "Could not find method setEpicenterBounds(Rect) on PopupWindow. Oh well.")
            }
        }

        /**
         * The provided prompt view should appear above list content.
         *
         * @see .setPromptPosition
         * @see .getPromptPosition
         * @see .setPromptView
         */
        val POSITION_PROMPT_ABOVE = 0

        /**
         * The provided prompt view should appear below list content.
         *
         * @see .setPromptPosition
         * @see .getPromptPosition
         * @see .setPromptView
         */
        val POSITION_PROMPT_BELOW = 1

        /**
         * Alias for [ViewGroup.LayoutParams.MATCH_PARENT].
         * If used to specify a popup width, the popup will match the width of the anchor view.
         * If used to specify a popup height, the popup will fill available space.
         */
        val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT

        /**
         * Alias for [ViewGroup.LayoutParams.WRAP_CONTENT].
         * If used to specify a popup width, the popup will use the width of its content.
         */
        val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT

        /**
         * Mode for [.setInputMethodMode]: the requirements for the
         * input method should be based on the focusability of the popup.  That is
         * if it is focusable than it needs to work with the input method, else
         * it doesn't.
         */
        val INPUT_METHOD_FROM_FOCUSABLE = PopupWindow.INPUT_METHOD_FROM_FOCUSABLE

        /**
         * Mode for [.setInputMethodMode]: this popup always needs to
         * work with an input method, regardless of whether it is focusable.  This
         * means that it will always be displayed so that the user can also operate
         * the input method while it is shown.
         */
        val INPUT_METHOD_NEEDED = PopupWindow.INPUT_METHOD_NEEDED

        /**
         * Mode for [.setInputMethodMode]: this popup never needs to
         * work with an input method, regardless of whether it is focusable.  This
         * means that it will always be displayed to use as much space on the
         * screen as needed, regardless of whether this covers the input method.
         */
        val INPUT_METHOD_NOT_NEEDED = PopupWindow.INPUT_METHOD_NOT_NEEDED

        private fun isConfirmKey(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
        }
    }
}
/**
 * Create a new, empty popup window capable of displaying items from a ListAdapter.
 * Backgrounds should be set using [.setBackgroundDrawable].
 *
 * @param context Context used for contained views.
 */
/**
 * Create a new, empty popup window capable of displaying items from a ListAdapter.
 * Backgrounds should be set using [.setBackgroundDrawable].
 *
 * @param context Context used for contained views.
 * @param attrs   Attributes from inflating parent views used to style the popup.
 */
/**
 * Create a new, empty popup window capable of displaying items from a ListAdapter.
 * Backgrounds should be set using [.setBackgroundDrawable].
 *
 * @param context Context used for contained views.
 * @param attrs Attributes from inflating parent views used to style the popup.
 * @param defStyleAttr Default style attribute to use for popup content.
 */
