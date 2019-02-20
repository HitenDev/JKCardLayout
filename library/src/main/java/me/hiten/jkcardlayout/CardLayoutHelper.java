package me.hiten.jkcardlayout;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

import static me.hiten.jkcardlayout.ItemTouchHelper.ACTION_STATE_SWIPE;
import static me.hiten.jkcardlayout.ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL;
import static me.hiten.jkcardlayout.ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS;

/**
 * 卡片布局管理类，也是调用的入口类，对外提供功能有：绑定RecyclerView和数据源、参数设置、状态监听回调；对内主要是设置RecyclerView的LayoutManager以及子Item的拖拽行为等
 * @param <T>　数据源类型
 */
public class CardLayoutHelper<T> {

    /**
     * 配置参数实体
     */
    public static class Config {

        static Config DEFAULT = new Config();

        /**
         * 旋转最大角度
         */
        float maxRotation = 10;

        /**
         * 展示的卡片个数，最小是2个
         */
        int cardCount =2;

        /**
         * 卡片位置之间的偏移量
         */
        int offset = Utils.dp2px(8);

        /**
         * 动画执行时间
         */
        long duration = 250;

        /**
         * 拖拽时触发移除的阈值比例
         */
        float swipeThreshold = 0.2f;

        public Config() {
        }

        public Config setMaxRotation(float maxRotation) {
            this.maxRotation = maxRotation;
            return this;
        }

        public Config setCardCount(int cardCount) {
            this.cardCount = cardCount;
            return this;
        }

        public Config setOffset(int offset) {
            this.offset = offset;
            return this;
        }

        public Config setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public Config setSwipeThreshold(float swipeThreshold) {
            this.swipeThreshold = swipeThreshold;
            return this;
        }
    }


    /**
     * 数据源绑定回调
     * @param <T>　数据源类型
     */
    public interface BindDataSource<T> {

        /**
         * 返回Adapter对应的数据源
         * @return　
         */
        List<T> bind();
    }

    /**
     * 卡片状态
     */
    public enum State {

        /**
         * 初始状态，即无拖动行为和动画执行
         */
        IDLE,

        /**
         * 手指拖动中的状态
         */
        SWIPE,

        /**
         * 执行Back动画或者松手回到原始位置的动画过程
         */
        BACK_ANIM,

        /**
         * 执行Next动画或者松手移除卡片的动画过程
         */
        LEAVE_ANIM
    }

    private BindDataSource<T> mBindDataSource;


    /**
     * 移除的数据回退栈
     */
    private LinkedList<T> mRemovedDataStack = new LinkedList<>();

    private OnCardLayoutListener mOnCardLayoutListener;

    private JKCardLayoutManager mJKCardLayoutManager;

    private CardAnimatorManager mAnimatorStackManager;

    private Config mConfig;

    private State mState = State.IDLE;


    private RecyclerView mRecyclerView;


    public void bindDataSource(BindDataSource<T> bindDataSource) {
        this.mBindDataSource = bindDataSource;
    }

    public void setConfig(Config config) {
        if (config == null){
            return;
        }
        this.mConfig = config;
        if (mJKCardLayoutManager!=null) {
            mJKCardLayoutManager.setConfig(config);
        }
        if (mAnimatorStackManager!=null){
            mAnimatorStackManager.setRotation(config.maxRotation);
        }
    }

    public void setOnCardLayoutListener(OnCardLayoutListener onCardLayoutListener) {
        mOnCardLayoutListener = onCardLayoutListener;
        mJKCardLayoutManager.setOnCardLayoutListener(onCardLayoutListener);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        if (mConfig == null) {
            mConfig = Config.DEFAULT;
        }
        mAnimatorStackManager = new CardAnimatorManager();
        mAnimatorStackManager.setRotation(mConfig.maxRotation);

        mJKCardLayoutManager = new JKCardLayoutManager(recyclerView, mAnimatorStackManager);
        mJKCardLayoutManager.setConfig(mConfig);
        recyclerView.setLayoutManager(mJKCardLayoutManager);
        setItemTouchHelper(recyclerView);
    }


    private void setItemTouchHelper(final RecyclerView recyclerView) {
        //基于ItemTouchHelper源码修改，改动1:对动画结束位置的关键逻辑进行修改，可见代码621-629行 改动2:优化多指触控，代码413-415行
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            /**
             * 记录上一次有无拖动行为，为true表示手指在拖动
             */
            private Boolean mLastIsActive = null;

            /**
             * 动画执行类型，一般是ANIMATION_TYPE_SWIPE_CANCEL或者ANIMATION_TYPE_SWIPE_SUCCESS两种情况
             */
            private Integer mAnimationType = null;

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ACTION_STATE_SWIPE) {
                    notifyStateListener(State.SWIPE);
                }
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                notifyStateListener(State.IDLE);
                mLastIsActive = null;
                mAnimationType = null;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                //该方法是在Item移除动画结束后被调用，下面的代码主要是记录数据回退栈
                int position = viewHolder.getLayoutPosition();
                List<T> data = getDataList();
                if (data != null && !data.isEmpty()) {
                    T removeData = data.remove(position);
                    mRemovedDataStack.push(removeData);
                    if (mRecyclerView.getAdapter() != null) {
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                }
                //动画回退栈处理
                if (mAnimatorStackManager != null) {
                    CardAnimatorManager.AnimatorInfo animatorInfo = new CardAnimatorManager.AnimatorInfo();
                    animatorInfo.targetXr = viewHolder.itemView.getTranslationX() / recyclerView.getWidth();
                    animatorInfo.targetYr = viewHolder.itemView.getTranslationY() / recyclerView.getHeight();
                    animatorInfo.targetRotation = viewHolder.itemView.getRotation();
                    mAnimatorStackManager.addToBackStack(animatorInfo);
                }
                //状态回调
                notifyStateListener(State.IDLE);
                mLastIsActive = null;
                mAnimationType = null;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                //控制除了LayoutManager中的第一个View，其他都不能被拖拽
                if (viewHolder.getAdapterPosition()!=0){
                    return makeMovementFlags(0, 0);
                }
                return super.getMovementFlags(recyclerView, viewHolder);

            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return mConfig.swipeThreshold;
            }

            @Override
            public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
                mAnimationType = animationType;
                return (long) (mConfig.duration * 0.5f);
            }


            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (mLastIsActive!=null&&mLastIsActive&&!isCurrentlyActive){//监听手指抬起
                    //TOUCH UP
                        if (ANIMATION_TYPE_SWIPE_CANCEL == mAnimationType){
                            notifyStateListener(State.BACK_ANIM);
                        }else if (ANIMATION_TYPE_SWIPE_SUCCESS == mAnimationType){
                            notifyStateListener(State.LEAVE_ANIM);
                        }
                }
                mLastIsActive = isCurrentlyActive;
                //回调进度
                notifyDxDyListener(viewHolder.itemView.getTranslationX(), viewHolder.itemView.getTranslationY());

                //下面是计算动画执行的比例
                float dXY = (float) Math.sqrt((dX * dX + dY * dY));
                float wh = (float) Math.sqrt((recyclerView.getWidth() * recyclerView.getWidth() + recyclerView.getHeight() * recyclerView.getHeight()));
                float ratio = Math.min(Math.abs(dXY) / (wh * 0.5f), 1f);
                int childCount = recyclerView.getChildCount();
                if (childCount <= 1) {
                    return;
                }
                //除了最上面的卡片，其余都得做相应的平移
                for (int i = 0;i < childCount -1;i++){
                    View childAt = recyclerView.getChildAt(i);
                    childAt.setTranslationX(mConfig.offset * (childCount - 1 - i - ratio));
                    childAt.setTranslationY(-mConfig.offset * (childCount - 1 - i - ratio));
                }
                //最上面的卡片需要做角度的旋转
                viewHolder.itemView.setRotation(Math.signum(-dX) * Math.min(1f, Math.abs(dX) / recyclerView.getWidth()) * mConfig.maxRotation);

            }

        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    private void notifyStateListener(State state) {
        if (state != mState) {
            mState = state;
            if (mOnCardLayoutListener != null) {
                mOnCardLayoutListener.onStateChanged(mState);
            }
        }
    }

    private void notifyDxDyListener(float dx, float dy) {
        if (mOnCardLayoutListener != null) {
            mOnCardLayoutListener.onSwipe(dx, dy);
        }
    }

    private List<T> getDataList() {
        return mBindDataSource != null ? mBindDataSource.bind() : null;
    }


    /**
     * 是否能够执行Next行为
     * @return　
     */
    public boolean canNext() {
        List<T> data = getDataList();
        return data != null && data.size() > 0 && mState == State.IDLE && mJKCardLayoutManager.canNext();
    }

    /**
     * 是否能够执行Back行为
     * @return　
     */
    public boolean canBack() {
        return !noBack() && mState == State.IDLE && mJKCardLayoutManager.canBack();
    }


    /**
     * 是不是没有回退栈了
     * @return　
     */
    public boolean noBack() {
        return mRemovedDataStack == null || mRemovedDataStack.isEmpty();
    }

    /**
     * 执行Next行为
     */
    public void doNext() {
        List<T> data = getDataList();
        if (data==null){
            return;
        }
        if (!canNext()) {
            return;
        }
        T removeData = data.remove(0);
        mJKCardLayoutManager.pendingOptNext();
        mRemovedDataStack.push(removeData);
        if (mRecyclerView.getAdapter() != null) {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * 执行Back行为
     */
    public void doBack() {
        List<T> data = getDataList();
        if (data==null){
            return;
        }
        if (!canBack()) {
            return;
        }
        if (mRemovedDataStack.size() > 0) {
            T pop = mRemovedDataStack.pop();
            if (pop != null) {
                mJKCardLayoutManager.pendingOptBack();
                data.add(0, pop);
                if (mRecyclerView.getAdapter() != null) {
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        }
    }
}
