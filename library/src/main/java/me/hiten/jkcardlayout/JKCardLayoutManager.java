package me.hiten.jkcardlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;


public class JKCardLayoutManager extends RecyclerView.LayoutManager {

    private RecyclerView mRecyclerView;

    private CardAnimatorManager mAnimatorStackManager;

    private OnCardLayoutListener mCardLayoutListener;

    private CardLayoutHelper.State mState = CardLayoutHelper.State.IDLE;


    private CardLayoutHelper.Config mConfig = CardLayoutHelper.Config.DEFAULT;

    JKCardLayoutManager(RecyclerView mRecyclerView, CardAnimatorManager mAnimatorStackManager) {
        this.mRecyclerView = mRecyclerView;
        this.mAnimatorStackManager = mAnimatorStackManager;
    }

    void setConfig(CardLayoutHelper.Config config) {
        this.mConfig = config;
    }

    void setOnCardLayoutListener(OnCardLayoutListener onCardLayoutListener) {
        this.mCardLayoutListener = onCardLayoutListener;
    }


    private boolean mPendingOptBack;
    private boolean mPendingOptNext;
    private boolean mAnimatorRunning;

    boolean canBack(){
        return !mPendingOptBack&&!mAnimatorRunning;
    }

    boolean canNext(){
        return !mPendingOptNext&&!mAnimatorRunning;
    }

    void pendingOptBack() {
        mPendingOptBack = true;
    }

    void pendingOptNext() {
        mPendingOptNext = true;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        if (recycler == null || state == null) {
            return;
        }
        if (mAnimatorRunning) {
            return;
        }
        int childCount = mRecyclerView.getChildCount();
        if (mPendingOptBack) {
            if (childCount < 0) {
                mPendingOptBack = false;
                mState = CardLayoutHelper.State.IDLE;
                onLayoutChildren(recycler, state);
            } else {
                mAnimatorRunning = true;
                if (childCount > mConfig.cardCount - 1) {
                    detachAndScrapViewAt(0, recycler);
                }
                View view = recycler.getViewForPosition(0);
                addView(view);
                measureLayoutItemView(view);
                view.setAlpha(0f);
                doBackAnimator(view);
            }
            return;
        }

        if (mPendingOptNext) {
            if (childCount < 0) {
                mPendingOptNext = false;
                mState = CardLayoutHelper.State.IDLE;
                onLayoutChildren(recycler, state);
            } else {
                mAnimatorRunning = true;
                View view = mRecyclerView.getChildAt(childCount - 1);
                doNextAnimator(view);
            }
            return;
        }

        detachAndScrapAttachedViews(recycler);

        int itemCount = getItemCount();
        if (itemCount < 1) {
            return;
        }

        int maxCount = Math.min(mConfig.cardCount, itemCount);

        for (int position=maxCount - 1;position>=0;position--) {
            View view = recycler.getViewForPosition(position);
            addView(view);
            measureLayoutItemView(view);
            view.setRotation(0f);
            if (position > 0) {
                view.setTranslationX(mConfig.offset * position);
                view.setTranslationY(-mConfig.offset * position);
            } else {
                view.setTranslationX(0f);
                view.setTranslationY(0f);
            }

        }
    }

    private void measureLayoutItemView(View view){
        measureChildWithMargins(view, 0, 0);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        int left = 0;
        int top = 0;
        if (lp.width>0){
            int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
            int d = getWidth() - mRecyclerView.getPaddingLeft() - mRecyclerView.getPaddingRight() - decoratedMeasuredWidth - lp.leftMargin - lp.rightMargin;
            if (d>0){
                left = (int) (d/2f+0.5f);
            }
        }
        if (lp.height>0){
            int decoratedMeasuredHeight = mRecyclerView.getPaddingTop() - mRecyclerView.getPaddingBottom() - getDecoratedMeasuredHeight(view);
            int d = getHeight() - decoratedMeasuredHeight - lp.topMargin - lp.bottomMargin;
            if (d>0){
                top = (int) (d/2f+0.5f);
            }
        }
        layoutDecoratedWithMargins(view, left, top,
                left+getDecoratedMeasuredWidth(view)+lp.leftMargin+lp.rightMargin,
                top+lp.topMargin+lp.bottomMargin + getDecoratedMeasuredHeight(view));
    }

    private void doBackAnimator(final View view){
        final CardAnimatorManager.AnimatorInfo add = mAnimatorStackManager.takeAdd();
        int width = mRecyclerView.getWidth();
        int height = mRecyclerView.getHeight();
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(view, new CardAnimatorManager.AnimatorInfoProperty(width, height), new CardAnimatorManager.AnimatorInfoEvaluator(), add,CardAnimatorManager.AnimatorInfo.ZERO);
        objectAnimator.setDuration(mConfig.duration);
        listenerAnimator(false,objectAnimator,view,add);
        objectAnimator.start();
    }


    private void doNextAnimator(final View view){
        final CardAnimatorManager.AnimatorInfo createRemove = mAnimatorStackManager.createRemove();
        int width = mRecyclerView.getWidth();
        int height = mRecyclerView.getHeight();
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(view, new CardAnimatorManager.AnimatorInfoProperty(width, height), new CardAnimatorManager.AnimatorInfoEvaluator(), CardAnimatorManager.AnimatorInfo.ZERO, createRemove);
        objectAnimator.setDuration(mConfig.duration);
        listenerAnimator(true,objectAnimator,view,createRemove);
        objectAnimator.start();
    }

    private void listenerAnimator(final boolean next,ObjectAnimator objectAnimator,final View view,final CardAnimatorManager.AnimatorInfo animatorInfo){

        objectAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (!next){
                    view.setAlpha(1f);
                }
                if (next){
                    notifyStateListener(CardLayoutHelper.State.LEAVE_ANIM);
                }else {
                    notifyStateListener(CardLayoutHelper.State.BACK_ANIM);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (next) {
                    mPendingOptNext = false;
                    mAnimatorRunning = false;
                    mAnimatorStackManager.addRemoveToBackStack(animatorInfo);
                    notifyStateListener(CardLayoutHelper.State.IDLE);
                    requestLayout();
                }else {
                    mPendingOptBack = false;
                    mAnimatorRunning = false;
                    notifyStateListener(CardLayoutHelper.State.IDLE);
                    requestLayout();
                }
            }
        });
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            int childCount = mRecyclerView.getChildCount();
            Float[] translationXs = new Float[childCount];
            Float[]  translationYs = new Float[childCount];
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                notifyDxDyListener(view.getTranslationX(),view.getTranslationY());
                float fraction = animation.getAnimatedFraction();
                float fl = fraction * mConfig.offset;
                for (int i = 0;i<childCount;i++) {
                    View childAt = mRecyclerView.getChildAt(i);
                    if (childAt == view) {
                        continue;
                    }
                    if (translationXs[i] == null) {
                        translationXs[i] = childAt.getTranslationX();
                    }
                    if (translationYs[i] == null) {
                        translationYs[i] = childAt.getTranslationY();
                    }
                    if (next) {
                        childAt.setTranslationX(translationXs[i] - fl);
                        childAt.setTranslationY(translationYs[i] + fl);
                    }else {
                        childAt.setTranslationX(translationXs[i]+fl);
                        childAt.setTranslationY(translationYs[i]-fl);
                    }
                }
            }

        });
    }


    private void notifyStateListener(CardLayoutHelper.State state) {
        if (state != mState) {
            mState = state;
            if (mCardLayoutListener != null) {
                mCardLayoutListener.onStateChanged(mState);
            }
        }
    }

    private void notifyDxDyListener(float dx, float dy) {
        if (mCardLayoutListener != null) {
            mCardLayoutListener.onSwipe(dx, dy);
        }
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
