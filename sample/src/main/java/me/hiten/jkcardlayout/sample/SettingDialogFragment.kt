package me.hiten.jkcardlayout.sample

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.dialog_setting.*

class SettingDialogFragment : DialogFragment(){

    var mStartValue:Int = 0
    var mCurValue:Int = 0
    var mEndValue:Int = 0
    var mDivisor:Int = 1

    var mProgress:Int =0

    var mInitProgress:Int = 0

    var mTitlePrefix:String? = null

    companion object {

        const val KEY_TITLE = "title"
        const val KEY_START = "start"
        const val KEY_CUR = "cur"
        const val KEY_END = "end"
        const val KEY_DIVISOR = "mDivisor"

        fun newInstance(title: String,start:Int,cur:Int,end:Int,divisor:Int):SettingDialogFragment{
            val settingDialogFragment = SettingDialogFragment()
            val bundle = Bundle()
            bundle.putString(KEY_TITLE,title)
            bundle.putInt(KEY_START,start)
            bundle.putInt(KEY_CUR,cur)
            bundle.putInt(KEY_END,end)
            bundle.putInt(KEY_DIVISOR,divisor)
            settingDialogFragment.arguments = bundle
            return settingDialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_setting,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            val title = it.getString(KEY_TITLE)
            mStartValue = it.getInt(KEY_START)
            mCurValue = it.getInt(KEY_CUR)
            mEndValue = it.getInt(KEY_END)
            mDivisor = it.getInt(KEY_DIVISOR,1)
            tv_title.text = title
            mTitlePrefix = title?.split(":")?.get(0)
            val max = mEndValue - mStartValue
            mProgress = mCurValue-mStartValue
            mInitProgress = mProgress
            seek_bar.max = max
            seek_bar.progress = mProgress

            seek_bar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    mProgress = progress
                    updateTitle()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })
        }
    }

    fun updateTitle(){
        val num:Number
        if (mDivisor==1) {
            num = mProgress+mStartValue
        }else{
            num = (mProgress+mStartValue) / mDivisor.toFloat()
        }
        mTitlePrefix?.let {
            tv_title.text = it.plus(":").plus(num)
        }

    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        if (mProgress == mInitProgress){
            return
        }
        if (mDivisor==1) {
            mCallback?.invoke(mProgress+mStartValue)
        }else{
            mCallback?.invoke((mProgress+mStartValue) / mDivisor.toFloat())
        }
    }


    private var mCallback : ((Number) -> Unit)? = null

    fun setCallback(callback:((Number) -> Unit)){
        this.mCallback = callback
    }

}