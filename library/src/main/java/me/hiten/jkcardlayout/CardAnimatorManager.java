package me.hiten.jkcardlayout;



import android.animation.TypeEvaluator;
import android.util.Property;
import android.view.View;

import java.util.LinkedList;
import java.util.Random;

/**
 * 卡片动画管理类，管理动画的参数和回退栈
 */
class CardAnimatorManager {


    /**
     * 动画旋转角度参数
     */
    private float mRotation = 20f;

    /**
     * AnimatorInfo变化过程估值器
     */
    public static class AnimatorInfoEvaluator implements TypeEvaluator<AnimatorInfo> {

        AnimatorInfo mTemp = new AnimatorInfo();

        @Override
        public AnimatorInfo evaluate(float fraction, AnimatorInfo startValue, AnimatorInfo endValue) {
            mTemp.targetRotation = startValue.targetRotation+(endValue.targetRotation-startValue.targetRotation)*fraction;
            mTemp.targetXr = startValue.targetXr+(endValue.targetXr-startValue.targetXr)*fraction;
            mTemp.targetYr = startValue.targetYr+(endValue.targetYr-startValue.targetYr)*fraction;
            return mTemp;
        }
    }

    /**
     * 与AnimatorInfo对应的View属性
     */
    public static class AnimatorInfoProperty extends Property<View,AnimatorInfo>{

        /**
         * x偏移基数
         */
        private float baseX;

        /**
         * y偏移基数
         */
        private float baseY;

        AnimatorInfoProperty(float baseX, float baseY) {
            super(AnimatorInfo.class, null);
            this.baseX = baseX;
            this.baseY = baseY;
        }

        @Override
        public void set(View object, AnimatorInfo value) {
           object.setTranslationX(value.targetXr*baseX);
           object.setTranslationY(value.targetYr*baseY);
           object.setRotation(value.targetRotation);
        }

        @Override
        public AnimatorInfo get(View object) {
            return null;
        }
    }


    void setRotation(float maxRotation) {
        this.mRotation = maxRotation;
    }


    /**
     * 存储动画信息的回退栈
     */
    private LinkedList<AnimatorInfo> mBackStack = new LinkedList<>();

    /**
     * 尝试从回退栈栈顶获取AnimatorInfo并返回，如果栈为空，创建新的并返回
     * @return　
     */
    AnimatorInfo takeRecentInfo() {
        AnimatorInfo pop = null;
        if (mBackStack.size() > 0) {
            pop = mBackStack.pop();
        }
        if (pop == null) {
            pop = new AnimatorInfo();
        }
        return pop;
    }


    /**
     * 将移除的AnimatorInfo添加到回退栈
     * @param removed 移除栈顶
     */
    void addToBackStack(AnimatorInfo removed) {
        mBackStack.push(removed);
    }

    /**
     * 创建一个随机的动画Info
     * @return　
     */
    AnimatorInfo createRandomInfo() {
        AnimatorInfo animatorInfo = new AnimatorInfo();
        boolean b = new Random().nextBoolean();
        animatorInfo.targetXr = b ? -1.0f : 1.0f;
        animatorInfo.targetYr = 1f;
        animatorInfo.targetRotation = mRotation;
        return animatorInfo;
    }


    static class AnimatorInfo {
        /**
         *x偏移比例
         */
        float targetXr;

        /**
         * y偏移比例
         */
        float targetYr;

        /**
         * 目标旋转角度
         */
        float targetRotation;

        /**
         * 代表动画执行到原始位置
         */
        static AnimatorInfo ZERO = new AnimatorInfo();
    }

}
