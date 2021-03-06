package com.playground.notification.ui.ib;

import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.util.Property;
import com.playground.notification.ui.BakedBezierInterpolator;


/**
 * Created by Xinyue Zhao
 */
public abstract class IBLayoutBase<T extends View> extends FrameLayout {
	private static final String TAG = IBLayoutBase.class.getName();
	private View topView;
	public final static int LINEARPARAMS = 1;
	public final static int RELATIVEPARAMS = 2;
	public final static int RECYCLERVIEWPARAMS = 3;
	private int params = 0;
	private float mLastMotionX, mLastMotionY;
	private float mInitialMotionX, mInitialMotionY;
	protected T mDragableView;
	private int mTouchSlop;
	private int ANIMDURA = 300;
	private int closeDistance;
	private boolean mIsBeingDragged = false;
	private boolean mFilterTouchEvents = true;
	private boolean shouldRollback;
	private Mode mMode = Mode.getDefault();
	private OnDragStateChangeListener onDragStateChangeListener;
	private Runnable closeRunnable = new Runnable() {
		@Override
		public void run() {
			closeWithAnim();
		}
	};

	private Mode mCurrentMode;

	enum Mode {
		DISABLED(0x0),
		PULL_FROM_START(0x1),
		PULL_FROM_END(0x2),
		BOTH(0x3);
		private int mIntValue;

		Mode(int modeInt) {
			mIntValue = modeInt;
		}

		static Mode getDefault() {
			return BOTH;
		}
	}

	private DragState dragState = DragState.CANNOTCLOSE;

	public enum DragState {
		CANCLOSE(0x0),
		CANNOTCLOSE(0X1);

		DragState(int value) {
		}
	}

	;

	public IBLayoutBase(Context context) {
		this(context, null);
	}

	public IBLayoutBase(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public IBLayoutBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setVisibility(INVISIBLE);

		ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();
		closeDistance = dp2px(60);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		}
		mHeightAnimator = ObjectAnimator.ofInt(this, aHeight, 0, 0);
		mScrollYAnimator = ObjectAnimator.ofInt(this, aScrollY, 0, 0);
		mHeightAnimator.setDuration(ANIMDURA);
		mScrollYAnimator.setDuration(ANIMDURA);
		animatorSet.playTogether(mHeightAnimator, mScrollYAnimator);
		animatorSet.setInterpolator(mInterpolator);

		mDragableView = createDragableView(context, attrs);
		addDragableView(mDragableView);

	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		ViewGroup inboxBackgroundV = mIBBackground.toViewGroup();
		if (inboxBackgroundV.getHeight() != 0 && inboxBackgroundV.getChildCount() > 0 && inboxBackgroundV.getHeight() > inboxBackgroundV.getChildAt(0)
		                                                                                        .getHeight()) {
			View view = inboxBackgroundV.getChildAt(0)
			                            .findViewWithTag("empty_view");
			if (view == null) {
				return;
			}
			ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
			layoutParams.height = inboxBackgroundV.getHeight() - inboxBackgroundV.getChildAt(0)
			                                                                     .getHeight();
			view.setLayoutParams(layoutParams);
			view.requestLayout();
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			mIsBeingDragged = false;
			return false;
		}

		if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
			return true;
		}
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				if (isReadyForPull()) {
					mLastMotionY = mInitialMotionY = ev.getY();
					mLastMotionX = mInitialMotionX = ev.getX();
					mIsBeingDragged = false;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (isReadyForPull()) {
					final float y = ev.getY(), x = ev.getX();
					final float diff, oppositeDiff, absDiff;

					diff = y - mLastMotionY;
					oppositeDiff = x - mLastMotionX;

					absDiff = Math.abs(diff);

					if (absDiff > mTouchSlop && (!mFilterTouchEvents || absDiff > Math.abs(oppositeDiff))) {
						if (diff >= 1f && isReadyForDragStart()) {
							mLastMotionY = y;
							mLastMotionX = x;
							mIsBeingDragged = true;
							//mCurrentMode = Mode.PULL_FROM_START;
							if (mMode == Mode.BOTH) {
								mCurrentMode = Mode.PULL_FROM_START;
								mIBBackground.setCurrentMode(mCurrentMode);
							}
						} else if (diff <= -1f && isReadyForDragEnd()) {
							mLastMotionY = y;
							mLastMotionX = x;
							mIsBeingDragged = true;
							if (mMode == Mode.BOTH) {
								mCurrentMode = Mode.PULL_FROM_END;
								mIBBackground.setCurrentMode(mCurrentMode);
							}
						}
					}
				}
				break;
		}
		return mIsBeingDragged;
	}

	private boolean isReadyForPull() {
		switch (mMode) {
			case PULL_FROM_START:
				return isReadyForDragStart();
			case PULL_FROM_END:
				return isReadyForDragEnd();
			case BOTH:
				return isReadyForDragEnd() || isReadyForDragStart();
			default:
				return false;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				if (mIsBeingDragged) {
					mLastMotionY = event.getY();
					mLastMotionX = event.getX();
					pullEvent();
					return true;
				}
				break;
			case MotionEvent.ACTION_DOWN: {
				if (isReadyForPull()) {
					mLastMotionY = mInitialMotionY = event.getY();
					mLastMotionX = mInitialMotionX = event.getX();
					return true;
				}
				break;
			}
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				if (mIsBeingDragged) {
					mIsBeingDragged = false;
					if (true) {
						smoothScrollTo(0, 200, 0);
						prevOffSetY = 0;
						return true;
					}
					return true;
				}
				break;
			}
		}
		return false;
	}

	static final float FRICTION = 2.0f;

	private void pullEvent() {
		final int newScrollValue;
		final float initialMotionValue, lastMotionValue;

		initialMotionValue = mInitialMotionY;
		lastMotionValue = mLastMotionY;

		switch (mCurrentMode) {
			case PULL_FROM_END:
				newScrollValue = Math.round(Math.max(initialMotionValue - lastMotionValue, 0) / FRICTION);
				break;
			case PULL_FROM_START:
			default:
				newScrollValue = Math.round(Math.min(initialMotionValue - lastMotionValue, 0) / FRICTION);
				break;
		}
		moveContent(newScrollValue);
	}

	private void addDragableView(T DragableView) {
		addView(DragableView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	}

	protected abstract T createDragableView(Context context, AttributeSet attrs);

	protected abstract boolean isReadyForDragStart();

	protected abstract boolean isReadyForDragEnd();

	public final T getDragableView() {
		return mDragableView;
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		final T refreshableView = getDragableView();
		if (child == refreshableView) {
			super.addView(child, index, params);
			return;
		}

		if (refreshableView instanceof ViewGroup) {
			((ViewGroup) refreshableView).addView(child, index, params);
		} else {
			throw new UnsupportedOperationException("Dragable View is not a ViewGroup so can't addView");
		}
	}


	private int realOffsetY;
	private int prevOffSetY = 0;
	private int dy;

	private int moveContent(int offsetY) {

		realOffsetY = (int) (offsetY / 1.4f);
		scrollTo(0, realOffsetY);
		dy = prevOffSetY - realOffsetY;
		prevOffSetY = realOffsetY;
		ViewGroup inboxBackgroundV = mIBBackground.toViewGroup();
		inboxBackgroundV.scrollBy(0, -dy);

		if (realOffsetY < -closeDistance || realOffsetY > closeDistance && onDragStateChangeListener != null) {
			onDragStateChangeListener.dragStateChange(DragState.CANCLOSE);
			dragState = DragState.CANCLOSE;
		} else if (dragState == DragState.CANCLOSE && realOffsetY < closeDistance && realOffsetY > -closeDistance) {
			onDragStateChangeListener.dragStateChange(DragState.CANNOTCLOSE);
			dragState = DragState.CANNOTCLOSE;
		}
		/*
		* Draw Shadow
        * */
		switch (mCurrentMode) {
			case PULL_FROM_END:
				mIBBackground.drawBottomShadow(inboxBackgroundV.getScrollY() + inboxBackgroundV.getHeight() - realOffsetY, inboxBackgroundV.getScrollY() + inboxBackgroundV.getHeight(), 60);
				break;
			case PULL_FROM_START:
			default:
				mIBBackground.drawTopShadow(inboxBackgroundV.getScrollY(), -realOffsetY, 60);
				break;
		}
		inboxBackgroundV.invalidate();
		return realOffsetY;
	}

	private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
	private Interpolator mScrollAnimationInterpolator = new DecelerateInterpolator();

	final class SmoothScrollRunnable implements Runnable {
		private final Interpolator mInterpolator;
		private final int mScrollToY;
		private final int mScrollFromY;
		private final long mDuration;

		private boolean mContinueRunning = true;
		private long mStartTime = -1;
		private int mCurrentY = -1;
		private int PrevY = 0;
		private int offsetY = 0;

		public SmoothScrollRunnable(int fromY, int toY, long duration) {
			mScrollFromY = fromY;
			mScrollToY = toY;
			mInterpolator = mScrollAnimationInterpolator;
			mDuration = duration;
		}

		@Override
		public void run() {
			if (mStartTime == -1) {
				mStartTime = System.currentTimeMillis();
			} else {
				long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

				final int deltaY = Math.round((mScrollFromY - mScrollToY) * mInterpolator.getInterpolation(normalizedTime / 1000f));
				mCurrentY = mScrollFromY - deltaY;
				if (PrevY == 0) { /*the PrevY will be 0 at first time */
					PrevY = mScrollFromY;
				}
				offsetY = PrevY - mCurrentY;
				PrevY = mCurrentY;
				scrollTo(0, mCurrentY);
				if (shouldRollback) {
					ViewGroup inboxBackgroundV = mIBBackground.toViewGroup();
					inboxBackgroundV.scrollBy(0, -offsetY);
				}
			}
			// keep going...
			if (mContinueRunning && mScrollToY != mCurrentY) {
				IBLayoutBase.this.postDelayed(this, 16);
			} else {
				//Finish
			}
		}

		public void stop() {
			mContinueRunning = false;
			removeCallbacks(this);
		}
	}

	private final void smoothScrollTo(int newScrollValue, long duration, long delayMillis) {
		if (null != mCurrentSmoothScrollRunnable) {
			mCurrentSmoothScrollRunnable.stop();
		}
		final int oldScrollValue;
		oldScrollValue = getScrollY();

		if (oldScrollValue < -closeDistance || oldScrollValue > closeDistance) {
			setVisibility(View.INVISIBLE);
			postDelayed(closeRunnable, 100);
			shouldRollback = false;
		} else {
			shouldRollback = true;
		}

		if (oldScrollValue != newScrollValue) {
			mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration);
			if (delayMillis > 0) {
				postDelayed(mCurrentSmoothScrollRunnable, delayMillis);
			} else {
				post(mCurrentSmoothScrollRunnable);
			}
		}
	}

	public void setIBBackground(IBBackground IBBackground) {
		mIBBackground = IBBackground;
	}

	public void setOnDragStateChangeListener(OnDragStateChangeListener listener) {
		onDragStateChangeListener = listener;
	}

	public void setCloseDistance(int dp) {
		closeDistance = dp2px(dp);
	}

	private int mHeight = 0;
	private int iScrollY;
	private IBBackground mIBBackground;
	private ViewGroup.LayoutParams layoutParams;
	private LinearLayout.LayoutParams linearLayoutParams;
	private RelativeLayout.LayoutParams relativeLayoutParams;
	private RecyclerView.LayoutParams recyclerViewParams;
	private AnimatorSet animatorSet = new AnimatorSet();
	private ObjectAnimator mHeightAnimator;
	private ObjectAnimator mScrollYAnimator;
	private Interpolator mInterpolator = BakedBezierInterpolator.INSTACNCE;
	private int beginScrollY, endScrollY;
	private int beginBottomMargin;
	private int heightRange;
	private boolean isStartAnim = false;
	private Runnable showRunnable = new Runnable() {
		@Override
		public void run() {
			setVisibility(View.VISIBLE);
		}
	};

	public void openWithAnim(View topView) {
		this.topView = topView;
	    /*
         *  eat the touch event when anim start
         */
		mIBBackground.setTouchable(false);
		layoutParams = topView.getLayoutParams();
		if (layoutParams instanceof LinearLayout.LayoutParams) {
			params = LINEARPARAMS;
			linearLayoutParams = (LinearLayout.LayoutParams) layoutParams;
			heightRange = linearLayoutParams.bottomMargin;
		} else if (layoutParams instanceof RelativeLayout.LayoutParams) {
			params = RELATIVEPARAMS;
			relativeLayoutParams = (RelativeLayout.LayoutParams) layoutParams;
			heightRange = relativeLayoutParams.bottomMargin;
		} else if (layoutParams instanceof RecyclerView.LayoutParams) {
			params = RECYCLERVIEWPARAMS;
			recyclerViewParams = (RecyclerView.LayoutParams) layoutParams;
			heightRange = recyclerViewParams.bottomMargin;
		} else {
			Log.e("error", "topView's parent should be linearlayout, relativelayout or recyclerview.");
			return;
		}

		ViewGroup inboxBackgroundV = mIBBackground.toViewGroup();

		isStartAnim = true;
		mIBBackground.setNeedToDrawShadow(true);
		beginBottomMargin = heightRange;
		ViewCompat.setAlpha(topView, 0);

		if (animatorSet.isRunning()) {
			animatorSet.cancel();
		}

		int scrollViewHeight = inboxBackgroundV.getHeight();
		endScrollY = topView.getTop();
		beginScrollY = inboxBackgroundV.getScrollY();
		heightRange = scrollViewHeight - topView.getHeight();
		mHeightAnimator.setIntValues(beginBottomMargin, heightRange);
		mScrollYAnimator.setIntValues(beginScrollY, endScrollY);
		mIBBackground.drawTopShadow(beginScrollY, endScrollY - beginScrollY, 0);
		mIBBackground.drawBottomShadow(topView.getBottom(), beginScrollY + scrollViewHeight, 0);
		animatorSet.start();
		postDelayed(showRunnable, ANIMDURA + 10);
	}

	public void closeWithAnim() {
		ViewGroup inboxBackgroundV = mIBBackground.toViewGroup();

		ViewCompat.setAlpha(topView, 1);
		mIBBackground.setNeedToDrawSmallShadow(false);
		isStartAnim = false;
		dragState = DragState.CANNOTCLOSE;
		if (onDragStateChangeListener != null) {
			onDragStateChangeListener.dragStateChange(dragState);
		}
		if (animatorSet.isRunning()) {
			animatorSet.cancel();
		}

		mHeightAnimator.setIntValues(heightRange, beginBottomMargin);
		mScrollYAnimator.setIntValues(inboxBackgroundV.getScrollY(), beginScrollY);
		animatorSet.start();
	}

	private void heightChangeAnim() {
		switch (params) {
			case LINEARPARAMS:
				linearLayoutParams.bottomMargin = mHeight;
				break;
			case RELATIVEPARAMS:
				relativeLayoutParams.bottomMargin = mHeight;
				break;
			case RECYCLERVIEWPARAMS:
				recyclerViewParams.bottomMargin = mHeight;
				break;
		}
		topView.setLayoutParams(layoutParams);
	}

	private int alpha;

	private void scrollYChangeAnim() {
		ViewGroup inboxBackgroundV = mIBBackground.toViewGroup();

		alpha = 60 * mHeight / heightRange;
		inboxBackgroundV.scrollTo(0, iScrollY);
		mIBBackground.drawTopShadow(iScrollY, topView.getTop() - iScrollY, alpha);
		mIBBackground.drawBottomShadow(topView.getBottom() + mHeight, inboxBackgroundV.getScrollY() + inboxBackgroundV.getHeight(), alpha);
		inboxBackgroundV.invalidate();
	}

	Property<IBLayoutBase, Integer> aHeight = new Property<IBLayoutBase, Integer>(Integer.class, "mHeight") {
		@Override
		public Integer get(IBLayoutBase object) {
			return object.mHeight;
		}

		@Override
		public void set(IBLayoutBase object, Integer value) {
			object.mHeight = value;
			heightChangeAnim();
			if (value == heightRange && isStartAnim) {
				//Open Anim Stop
				mIBBackground.setNeedToDrawSmallShadow(true);
			} else if (value == beginBottomMargin && !isStartAnim) {
				//Close Anim Stop
                /*
                 * enable touch event when top view close
                 */
				mIBBackground.setTouchable(true);
			}
		}
	};

	Property<IBLayoutBase, Integer> aScrollY = new Property<IBLayoutBase, Integer>(Integer.class, "iScrollY") {
		@Override
		public Integer get(IBLayoutBase object) {
			return object.iScrollY;
		}

		@Override
		public void set(IBLayoutBase object, Integer value) {
			object.iScrollY = value;
			scrollYChangeAnim();
		}
	};

	private int dp2px(float dp) {
		return (int) (dp * getContext().getResources()
		                               .getDisplayMetrics().density + 0.5f);
	}

	/**
	 * Created by Xinyue Zhao
	 */
	public interface OnDragStateChangeListener {
		void dragStateChange(DragState state);
	}

	public void close() {
		if (isStartAnim) {
			closeWithAnim();
			setVisibility(View.INVISIBLE);
		}
	}
}
