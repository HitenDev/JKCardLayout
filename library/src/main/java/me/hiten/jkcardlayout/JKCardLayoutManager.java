package me.hiten.jkcardlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * 自定义LayoutManager，主要管理RecyclerView子View的布局以及Next,Back的动画以及复用
 */
public class JKCardLayoutManager extends RecyclerView.LayoutManager {

    private RecyclerView mRecyclerView;

    private CardAnimatorManager mAnimatorStackManager;

    private OnCardLayoutListener mCardLayoutListener;

    private CardLayoutHelper.State mState = CardLayoutHelper.State.IDLE;


    private CardLayoutHelper.Config mConfig = new CardLayoutHelper.Config();

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


    /**
     * 控制是否执行Back,主要是用给onLayoutChildren做逻辑判断
     */
    private boolean mPendingOptBack;

    /**
     * 控制是否执行Next,主要是用给onLayoutChildren做逻辑判断
     */
    private boolean mPendingOptNext;

    /**
     * 动画是否正在执行
     */
    private boolean mAnimatorRunning;

    /**
     * 是否能够执行Back
     * @return true or false
     */
    boolean canBack(){
        return !mPendingOptBack&&!mAnimatorRunning;
    }

    /**
     * 是否能够执行Next
     * @return true or false
     */
    boolean canNext(){
        return !mPendingOptNext&&!mAnimatorRunning;
    }

    /**
     * 即将进行Back操作，下一步会影响onLayoutChildren的行为
     */
    void pendingOptBack() {
        mPendingOptBack = true;
    }

    /**
     * 即将进行Next操作，下一步会影响onLayoutChildren的行为
     */
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
        //Back操作在onLayoutChildren中的本质是移除最底下的View，添加到最顶层，做动画操作假装从屏幕外进来，掩人耳目
        if (mPendingOptBack) {
            if (childCount < 0) {
                mPendingOptBack = false;
                mState = CardLayoutHelper.State.IDLE;
                onLayoutChildren(recycler, state);
            } else {
                mAnimatorRunning = true;
                //尝试先回收最底下的View
                if (childCount > mConfig.cardCount - 1) {
                    detachAndScrapViewAt(0, recycler);
                }
                //复用上面回收的View
                View view = recycler.getViewForPosition(0);
                //透明度设置成0，要的就是添加的那瞬间不可见
                view.setAlpha(0f);
                addView(view);
                measureLayoutItemView(view);
                doBackAnimator(view);
            }
            return;
        }
        //Next操作在onLayoutChildren中的本质是暂时不执行View的移除，等动画完成之后，执行requestLayout刷新整个布局
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

        //逻辑能走到这里说明没有动画的执行，就是对卡片进行布局

        //移除掉所有的View
        detachAndScrapAttachedViews(recycler);

        //这个ItemCount对应Adapter返回的count，不是RecyclerView孩子数
        int itemCount = getItemCount();
        if (itemCount < 1) {
            return;
        }
        //计算最大卡片个数
        int maxCount = Math.min(mConfig.cardCount, itemCount);
        //要倒序遍历,因为先执行addView的View会在下面
        for (int position=maxCount - 1;position>=0;position--) {
            //position是制从大到小遍历，越大的越排在下面，执行的偏移也就越大
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

    /**
     * 测量并布局子View
     * @param view view
     */
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

    /**
     * 执行Back动画
     * @param view　targetView
     */
    private void doBackAnimator(final View view){
        final CardAnimatorManager.AnimatorInfo add = mAnimatorStackManager.takeRecentInfo();
        int width = mRecyclerView.getWidth();
        int height = mRecyclerView.getHeight();
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(view, new CardAnimatorManager.AnimatorInfoProperty(width, height), new CardAnimatorManager.AnimatorInfoEvaluator(), add,CardAnimatorManager.AnimatorInfo.ZERO);
        objectAnimator.setDuration(mConfig.duration);
        listenerAnimator(false,objectAnimator,view,add);
        objectAnimator.start();
    }


    /**
     * 执行Next动画
     * @param view　targetView
     */
    private void doNextAnimator(final View view){
        final CardAnimatorManager.AnimatorInfo createRemove = mAnimatorStackManager.createRandomInfo();
        int width = mRecyclerView.getWidth();
        int height = mRecyclerView.getHeight();
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(view, new CardAnimatorManager.AnimatorInfoProperty(width, height), new CardAnimatorManager.AnimatorInfoEvaluator(), CardAnimatorManager.AnimatorInfo.ZERO, createRemove);
        objectAnimator.setDuration(mConfig.duration);
        listenerAnimator(true,objectAnimator,view,createRemove);
        objectAnimator.start();
    }

    /**
     * 监听动画执行
     * @param next　true Next行为 / false Back行为
     * @param objectAnimator　动画
     * @param view　targetView
     * @param animatorInfo 动画信息
     */
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
                    mAnimatorStackManager.addToBackStack(animatorInfo);
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
            //记录动画执行前的所有子View的translationX
            Float[] translationXs = new Float[childCount];
            //记录动画执行前的所有子View的translationY
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
                    //对子View进行比例偏移
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
