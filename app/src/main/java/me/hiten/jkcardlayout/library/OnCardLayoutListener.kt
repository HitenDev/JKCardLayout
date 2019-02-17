package me.hiten.jkcardlayout.library

interface OnCardLayoutListener {
    fun onSwipe(dx:Float,dy:Float)
    fun onStateChanged(state:CardLayoutHelper.State)
}