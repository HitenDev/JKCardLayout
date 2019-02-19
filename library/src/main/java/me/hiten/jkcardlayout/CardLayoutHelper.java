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

public class CardLayoutHelper<T> {

    public static class Config {

        static Config DEFAULT = new Config();

        float maxRotation = 10;
        int cardCount =2;
        int offset = Utils.dp2px(8);
        long duration = 250;
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

    public interface BindDataSource<T> {
        List<T> bind();
    }

    public enum State {
        IDLE, SWIPE, BACK_ANIM, LEAVE_ANIM
    }

    private BindDataSource<T> mBindDataSource;


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
        this.mConfig = config;
        if (mJKCardLayoutManager!=null) {
            mJKCardLayoutManager.setConfig(config);
        }
    }

    public void setOnCardLayoutListener(OnCardLayoutListener onCardLayoutListener) {
        mOnCardLayoutListener = onCardLayoutListener;
        mJKCardLayoutManager.setOnCardLayoutListener(onCardLayoutListener);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mAnimatorStackManager = new CardAnimatorManager();
        if (mConfig == null) {
            mConfig = Config.DEFAULT;
        }
        mJKCardLayoutManager = new JKCardLayoutManager(recyclerView, mAnimatorStackManager);
        mJKCardLayoutManager.setConfig(mConfig);
        recyclerView.setLayoutManager(mJKCardLayoutManager);
        setItemTouchHelper(recyclerView);
    }


    private void setItemTouchHelper(final RecyclerView recyclerView) {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            private Boolean mLastIsActive = null;

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
                int position = viewHolder.getLayoutPosition();

                List<T> data = getDataList();
                if (data != null && !data.isEmpty()) {
                    T removeData = data.remove(position);
                    mRemovedDataStack.push(removeData);
                    if (mRecyclerView.getAdapter() != null) {
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                }

                if (mAnimatorStackManager != null) {
                    CardAnimatorManager.AnimatorInfo animatorInfo = new CardAnimatorManager.AnimatorInfo();
                    animatorInfo.targetXr = viewHolder.itemView.getTranslationX() / recyclerView.getWidth();
                    animatorInfo.targetYr = viewHolder.itemView.getTranslationY() / recyclerView.getHeight();
                    animatorInfo.targetRotation = viewHolder.itemView.getRotation();
                    mAnimatorStackManager.addRemoveToBackStack(animatorInfo);
                }

                notifyStateListener(State.IDLE);
                mLastIsActive = null;
                mAnimationType = null;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
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
                if (mLastIsActive!=null&&mLastIsActive&&!isCurrentlyActive){
                    //TOUCH UP
                        if (ANIMATION_TYPE_SWIPE_CANCEL == mAnimationType){
                            notifyStateListener(State.BACK_ANIM);
                        }else if (ANIMATION_TYPE_SWIPE_SUCCESS == mAnimationType){
                            notifyStateListener(State.LEAVE_ANIM);
                        }
                }
                mLastIsActive = isCurrentlyActive;
                notifyDxDyListener(viewHolder.itemView.getTranslationX(), viewHolder.itemView.getTranslationY());
                float dXY = (float) Math.sqrt((dX * dX + dY * dY));
                float wh = (float) Math.sqrt((recyclerView.getWidth() * recyclerView.getWidth() + recyclerView.getHeight() * recyclerView.getHeight()));
                float ratio = Math.min(Math.abs(dXY) / (wh * 0.5f), 1f);
                int childCount = recyclerView.getChildCount();
                if (childCount <= 1) {
                    return;
                }
                for (int i = 0;i < childCount -1;i++){
                    View childAt = recyclerView.getChildAt(i);
                    childAt.setTranslationX(mConfig.offset * (childCount - 1 - i - ratio));
                    childAt.setTranslationY(-mConfig.offset * (childCount - 1 - i - ratio));
                }
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


    public boolean canNext() {
        List<T> data = getDataList();
        return data != null && data.size() > 0 && mState == State.IDLE && mJKCardLayoutManager.canNext();
    }

    public boolean canBack() {
        return !noBack() && mState == State.IDLE && mJKCardLayoutManager.canBack();
    }

    public boolean noBack() {
        return mRemovedDataStack == null || mRemovedDataStack.isEmpty();
    }

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
