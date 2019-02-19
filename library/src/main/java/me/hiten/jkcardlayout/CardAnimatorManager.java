package me.hiten.jkcardlayout;



import android.animation.TypeEvaluator;
import android.util.Property;
import android.view.View;

import java.util.LinkedList;
import java.util.Random;

public class CardAnimatorManager {

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

    public static class AnimatorInfoProperty extends Property<View,AnimatorInfo>{

        private float baseX;

        private float baseY;

        public AnimatorInfoProperty(float baseX,float baseY) {
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


    private LinkedList<AnimatorInfo> mStack = new LinkedList<>();

    public AnimatorInfo takeAdd() {
        AnimatorInfo pop = null;
        if (mStack.size() > 0) {
            pop = mStack.pop();
        }
        if (pop == null) {
            pop = new AnimatorInfo();
        }
        return pop;
    }


    public void addRemoveToBackStack(AnimatorInfo animatorInfo) {
        mStack.push(animatorInfo);
    }

    public AnimatorInfo createRemove() {
        AnimatorInfo animatorInfo = new AnimatorInfo();
        boolean b = new Random().nextBoolean();
        animatorInfo.targetXr = b ? -1.0f : 1.0f;
        animatorInfo.targetYr = 1f;
        animatorInfo.targetRotation = 10;
        return animatorInfo;
    }


    public static class AnimatorInfo {
        float targetXr;
        float targetYr;
        float targetRotation;

        public static AnimatorInfo ZERO = new AnimatorInfo();
    }

}
