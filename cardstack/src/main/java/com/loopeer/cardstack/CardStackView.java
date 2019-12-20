package com.loopeer.cardstack;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Observable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

public class CardStackView extends ViewGroup implements ScrollDelegate {

    public static final int INVALID_TYPE = -1;
    public static final int ANIMATION_STATE_START = 0;
    public static final int ANIMATION_STATE_END = 1;
    public static final int ANIMATION_STATE_CANCEL = 2;
    static final int DEFAULT_SELECT_POSITION = -1;
    private static final int INVALID_POINTER = -1;
    private static final String TAG = "CardStackView";
    private final ViewDataObserver mObserver = new ViewDataObserver();

    private int mTotalLength;//内容item长总高度
    private int mOverlapGaps;//预留长度
    private int mOverlapGapsCollapse;
    private int mNumBottomShow;//打开状态下，下面的card 显示几个
    private StackAdapter mStackAdapter;
    //选中的卡片position 其中不包括 mHeadView 和 mBottomView
    private int mSelectPosition = DEFAULT_SELECT_POSITION;
    private int mShowHeight;// view 显示区域高
    private List<ViewHolder> mViewHolders;
    private AnimatorAdapter mAnimatorAdapter;
    private int mDuration;
    private OverScroller mScroller;//负责惯性回弹
    private int mLastMotionY;//上一次
    private boolean mIsBeingDragged = false;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;//触发移动事件的最小距离
    private int mMinimumVelocity;// 判定速度
    private int mMaximumVelocity;
    private int mActivePointerId = INVALID_POINTER; //触点id
    private int mNestedYOffset;
    private boolean mScrollEnable = true;//是否允许滑动
    private ScrollDelegate mScrollDelegate;
    private ItemExpendListener mItemExpendListener;
    /**
     * 头部view
     */
    private View mHeadView;
    /**
     * 底部view
     */
    private View mBottomView;

    public CardStackView(Context context) {
        this(context, null);
    }

    public CardStackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CardStackView, defStyleAttr, defStyleRes);
        setOverlapGaps(array.getDimensionPixelSize(R.styleable.CardStackView_stackOverlapGaps, dp2px(100)));
        setOverlapGapsCollapse(array.getDimensionPixelSize(R.styleable.CardStackView_stackOverlapGapsCollapse, dp2px(20)));
        setDuration(array.getInt(R.styleable.CardStackView_stackDuration, AnimatorAdapter.ANIMATION_DURATION));

        setNumBottomShow(array.getInt(R.styleable.CardStackView_stackNumBottomShow, 3));
        array.recycle();

        AnimatorAdapter animatorAdapter = new AllMoveDownAnimatorAdapter(this);
        setAnimatorAdapter(animatorAdapter);
        mViewHolders = new ArrayList<>();
        initScroller();

    }

    private int dp2px(int value) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    public void setAnimatorAdapter(AnimatorAdapter animatorAdapter) {
        clearScrollYAndTranslation();
        mAnimatorAdapter = animatorAdapter;
        mScrollDelegate = this;
    }

    private void initScroller() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        //先分发给Child View进行处理，如果所有的Child View都沒有处理，則自己再处理
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public void clearScrollYAndTranslation() {
        if (mSelectPosition != DEFAULT_SELECT_POSITION) {
            clearSelectPosition();
        }
        if (mScrollDelegate != null) mScrollDelegate.setViewScrollY(0);
        requestLayout();
    }

    public void clearSelectPosition() {
        updateSelectPosition(mSelectPosition);
    }

    public void updateSelectPosition(final int selectPosition) {
        post(new Runnable() {
            @Override
            public void run() {
                doCardClickAnimation(mViewHolders.get(selectPosition), selectPosition);
            }
        });
    }

    private void doCardClickAnimation(final ViewHolder viewHolder, int position) {
        if (mViewHolders.size() <= 1) return;

        if (mSelectPosition != DEFAULT_SELECT_POSITION) {// 状态已经打开
            //关闭动画
            mAnimatorAdapter.onItemCollapse(mViewHolders.get(mSelectPosition));
        } else {
            //打开动画
            mAnimatorAdapter.onItemExpand(viewHolder, position);

        }

    }

    private void checkContentHeightByParent(int heightMeasureSpec) {
        View parentView = (View) getParent();

        mShowHeight =  getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec) - parentView.getPaddingTop() - parentView.getPaddingBottom();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CardStackView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setAdapter(StackAdapter stackAdapter) {
        mStackAdapter = stackAdapter;
        mStackAdapter.registerObserver(mObserver);
        refreshView();
    }

    private void refreshView() {
        removeAllViews();
        mViewHolders.clear();

        if (mHeadView != null) addView(mHeadView);
        for (int i = 0; i < mStackAdapter.getItemCount(); i++) {
            ViewHolder holder = getViewHolder(i);
            holder.position = i;
            holder.onItemExpand(i == mSelectPosition);
            addView(holder.itemView);
            setClickAnimator(holder, i);
            mStackAdapter.bindViewHolder(holder, i);
        }
        if (mBottomView != null) addView(mBottomView);
        requestLayout();
    }

    ViewHolder getViewHolder(int i) {
        if (i == DEFAULT_SELECT_POSITION) return null;
        ViewHolder viewHolder;
        if (mViewHolders.size() <= i || mViewHolders.get(i).mItemViewType != mStackAdapter.getItemViewType(i)) {
            viewHolder = mStackAdapter.createView(this, mStackAdapter.getItemViewType(i));
            mViewHolders.add(viewHolder);
        } else {
            viewHolder = mViewHolders.get(i);
        }
        return viewHolder;
    }

    private void setClickAnimator(final ViewHolder holder, final int position) {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectPosition == DEFAULT_SELECT_POSITION) return;
                performItemClick(mViewHolders.get(mSelectPosition));
            }
        });
        holder.itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                performItemClick(holder);
            }
        });
    }

    public void performItemClick(ViewHolder viewHolder) {
        doCardClickAnimation(viewHolder, viewHolder.position);
    }

    public View getmHeadView() {
        return mHeadView;
    }

    public void setmHeadView(View mHeadView) {
        this.mHeadView = mHeadView;
    }

    public View getmBottomView() {
        return mBottomView;
    }

    public void setmBottomView(View mBottomView) {
        this.mBottomView = mBottomView;

    }

    public boolean isExpending() {
        return mSelectPosition != DEFAULT_SELECT_POSITION;
    }

    /**
     * 整个方法是，滑动交给自己touch处理（包括屏蔽parent），非滑动，交给子item处理
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        //运动中 并 在滑动中的 被拦截为
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }
        // 没有滑动 并 不能滑动 给子view
        if (getViewScrollY() == 0 && !canScrollVertically(1)) {
            return false;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int y = (int) ev.getY(pointerIndex);
                final int yDiff = Math.abs(y - mLastMotionY);//触点y 与 上一次触点的y
                if (yDiff > mTouchSlop) {//大于，判断开始滑动
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    mNestedYOffset = 0;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);//屏蔽父类事件
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int y = (int) ev.getY();
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);// 得到触摸的单点
                initOrResetVelocityTracker();  // 重置手势速度的那东西
                mVelocityTracker.addMovement(ev);
                mIsBeingDragged = !mScroller.isFinished();// 滑动中状态
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP://滑动完成
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                if (mScroller.springBack(getViewScrollX(), getViewScrollY(), 0, 0, 0, getScrollRange())) {
                    postInvalidate();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        if (!mScrollEnable) {
            mIsBeingDragged = false;
        }
        return mIsBeingDragged;
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }


    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            // item 实际超过显示区域的长度 到 0  零之间随机一个惯性数
            scrollRange = Math.max(0, mTotalLength - mShowHeight);
        }
        return scrollRange;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutChild();
    }

    private void layoutChild() {
        int childTop = getPaddingTop();
        int childLeft = getPaddingLeft();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            final int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            final LayoutParams lp =
                    (LayoutParams) child.getLayoutParams();
            childTop += lp.topMargin;
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            childTop += lp.mHeight;
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /**
     * 自己处理滑动
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mIsBeingDragged) {//没有在 滑动中
            super.onTouchEvent(ev);
        }
        if (!mScrollEnable) {//不能滑动 ，直接消耗掉事件
            return true;
        }


        initVelocityTrackerIfNotExists();//检测速度那个初始化

        MotionEvent vtev = MotionEvent.obtain(ev);

        final int actionMasked = ev.getActionMasked();//动作

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {//没有item 自然跳过
                    return false;
                }
                if ((mIsBeingDragged = !mScroller.isFinished())) {//正在滑动，屏蔽父布局
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (!mScroller.isFinished()) {//点击时，正在动画，暂停动画
                    mScroller.abortAnimation();
                }
                mLastMotionY = (int) ev.getY();//位置
                mActivePointerId = ev.getPointerId(0);//触点
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int y = (int) ev.getY(activePointerIndex);
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {//没有滑动，但是达到了滑动条件
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);//屏蔽
                    }
                    mIsBeingDragged = true;
                    //下面是滑动距离计算，跟最小滑动距离挂勾
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {// 滑动中
                    mLastMotionY = y;
                    final int range = getScrollRange();//惯性的长度

                    if (overScrollBy(0, deltaY, 0, getViewScrollY(),
                            0, range, 0, 0, true)) {
                        mVelocityTracker.clear();

                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    // 一秒内 完成指定 距离的速度
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                    if (getChildCount() > 0) {
                        if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                            fling(-initialVelocity);//超速 惯性滑动
                        } else {
                            //没超速，惯性回弹
                            if (mScroller.springBack(getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, 0, 0,
                                    getScrollRange())) {
                                postInvalidate();
                            }
                        }
                        mActivePointerId = INVALID_POINTER;
                    }
                }
                endDrag();//结束滑动状态
                break;
            case MotionEvent.ACTION_CANCEL:
                // 取消惯性回弹
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, 0, 0, getScrollRange())) {
                        postInvalidate();
                    }
                    mActivePointerId = INVALID_POINTER;
                }
                endDrag();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            int height = mShowHeight;
            int bottom = mTotalLength;
            mScroller.fling(mScrollDelegate.getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, velocityY, 0, 0, 0,
                    Math.max(0, bottom - height), 0, 0);
            postInvalidate();
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;
        recycleVelocityTracker();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (getChildCount() > 0) {
            x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), getWidth());
            y = clamp(y, mShowHeight, mTotalLength);
            if (x != mScrollDelegate.getViewScrollX() || y != mScrollDelegate.getViewScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mScrollDelegate.scrollViewTo(0, mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int contentHeight = mShowHeight;
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = mTotalLength;
        final int scrollY = mScrollDelegate.getViewScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }    @Override
    public int getViewScrollX() {
        return getScrollX();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        checkContentHeightByParent(heightMeasureSpec);
        measureChild(widthMeasureSpec, heightMeasureSpec);
    }    @Override
    public void setViewScrollY(int y) {
        setScrollY(y);
    }

    private void measureChild(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = 0;
        mTotalLength = 0;
        mTotalLength += getPaddingTop() + getPaddingBottom();

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            final int totalLength = mTotalLength;

            final LayoutParams lp =
                    (LayoutParams) child.getLayoutParams();


            if (needOpen(i)) {//需要完全展示出来的不折叠
                lp.mHeight = child.getMeasuredHeight();
            } else {
                lp.mHeight = mOverlapGaps;
            }
            mTotalLength = Math.max(totalLength, totalLength + lp.mHeight + lp.topMargin +
                    lp.bottomMargin);
            final int margin = lp.leftMargin + lp.rightMargin;
            final int measuredWidth = child.getMeasuredWidth() + margin;
            maxWidth = Math.max(maxWidth, measuredWidth);
        }


        int heightSize = mTotalLength;
        heightSize = Math.max(heightSize, mShowHeight);
        int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                heightSizeAndState);
    }    @Override
    public void setViewScrollX(int x) {
        setScrollX(x);
    }

    /**
     * 需要完全展开的view
     */
    public boolean needOpen(int i) {
        if (mHeadView != null && i == 0) {//头部正常计算
            return true;
        } else if (i == getChildCount() - 1) {//最后一个正常计算
            return true;
        } else if (mBottomView != null && i == getChildCount() - 2) {//有尾部的情况倒数第二也要正常见计算
            return true;
        }
        return false;
    }    @Override
    public int getViewScrollY() {
        return getScrollY();
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        if (!mScroller.isFinished()) {
            final int oldX = mScrollDelegate.getViewScrollX();
            final int oldY = mScrollDelegate.getViewScrollY();
            mScrollDelegate.setViewScrollX(scrollX);
            mScrollDelegate.setViewScrollY(scrollY);
            onScrollChanged(mScrollDelegate.getViewScrollX(), mScrollDelegate.getViewScrollY(), oldX, oldY);
            if (clampedY) {
                mScroller.springBack(mScrollDelegate.getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }
    }

    @Override
    public void scrollViewTo(int x, int y) {
        scrollTo(x, y);
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            return 0;
        }
        if ((my + n) > child) {
            return child - my;
        }
        return n;
    }

    public int getCardSelectPosition() {
        return mSelectPosition;
    }

    public void setCardSelectPosition(int selectPosition) {
        if(selectPosition==DEFAULT_SELECT_POSITION){//动画结束，状态为开
            AlphaAnimation showAnim = new AlphaAnimation(0, 1);
            showAnim.setDuration(300);
            if (mHeadView != null) {
                mHeadView.setAnimation(showAnim);
                mHeadView.setVisibility(View.VISIBLE);
            }
            if (mBottomView != null) {
                mBottomView.setAnimation(showAnim);
                mBottomView.setVisibility(View.VISIBLE);
            }
        }else {//状态为关
            if (mHeadView != null) mHeadView.setVisibility(View.GONE);
            if (mBottomView != null) mBottomView.setVisibility(View.GONE);
        }
        mSelectPosition = selectPosition;
        mItemExpendListener.onItemExpend(mSelectPosition != DEFAULT_SELECT_POSITION);
    }

    public int getChildSeletPosition() {
        if (mHeadView == null) return mSelectPosition;
        return mSelectPosition + 1;
    }

    public int getOverlapGaps() {
        return mOverlapGaps;
    }

    public void setOverlapGaps(int overlapGaps) {
        mOverlapGaps = overlapGaps;
    }

    public int getOverlapGapsCollapse() {
        return mOverlapGapsCollapse;
    }

    public void setOverlapGapsCollapse(int overlapGapsCollapse) {
        mOverlapGapsCollapse = overlapGapsCollapse;
    }

    public void setScrollEnable(boolean scrollEnable) {
        mScrollEnable = scrollEnable;
    }

    public int getShowHeight() {
        return mShowHeight;
    }

    public int getTotalLength() {
        return mTotalLength;
    }

    public int getDuration() {
        if (mAnimatorAdapter != null) return mDuration;
        return 0;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public int getNumBottomShow() {
        return mNumBottomShow;
    }

    public void setNumBottomShow(int numBottomShow) {
        mNumBottomShow = numBottomShow;
    }

    public ScrollDelegate getScrollDelegate() {
        return mScrollDelegate;
    }

    public ItemExpendListener getItemExpendListener() {
        return mItemExpendListener;
    }

    public void setItemExpendListener(ItemExpendListener itemExpendListener) {
        mItemExpendListener = itemExpendListener;
    }

    public interface ItemExpendListener {
        void onItemExpend(boolean expend);
    }

    public static class LayoutParams extends MarginLayoutParams {


        public int mHeight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public static abstract class Adapter<VH extends ViewHolder> {


        private final AdapterDataObservable mObservable = new AdapterDataObservable();

        VH createView(ViewGroup parent, int viewType) {
            VH holder = onCreateView(parent, viewType);
            holder.mItemViewType = viewType;
            return holder;

        }


        protected abstract VH onCreateView(ViewGroup parent, int viewType);

        public void bindViewHolder(VH holder, int position) {
            onBindViewHolder(holder, position);
        }

        protected abstract void onBindViewHolder(VH holder, int position);

        public abstract int getItemCount();

        public final int getItemViewType(int position) {
            return 0;
        }

        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public void registerObserver(AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }
    }

    public static abstract class ViewHolder {

        public View itemView;
        public int beforeOpenHeight;
        int mItemViewType = INVALID_TYPE;
        int position;


        public ViewHolder(View view) {
            itemView = view;
        }

        public Context getContext() {
            return itemView.getContext();
        }

        public abstract void onItemExpand(boolean b);

        protected void onAnimationStateChange(int state, boolean willBeSelect) {

        }
    }

    public static class AdapterDataObservable extends Observable<AdapterDataObserver> {
        public boolean hasObservers() {
            return !mObservers.isEmpty();
        }

        public void notifyChanged() {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }
    }

    public static abstract class AdapterDataObserver {
        public void onChanged() {
        }
    }

    private class ViewDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            refreshView();
        }
    }








}
