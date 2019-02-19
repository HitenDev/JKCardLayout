package me.hiten.jkcardlayout;

public interface OnCardLayoutListener {
    void onSwipe(float dx,float dy);

    void onStateChanged(CardLayoutHelper.State state);
}
