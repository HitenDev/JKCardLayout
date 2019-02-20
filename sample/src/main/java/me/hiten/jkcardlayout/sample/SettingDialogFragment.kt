package me.hiten.jkcardlayout.sample

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_setting.*

class SettingDialogFragment : DialogFragment(){



    companion object {

        const val KEY_TITLE = "title"

        fun newInstance(title: String):SettingDialogFragment{
            val settingDialogFragment = SettingDialogFragment()
            val bundle = Bundle()
            bundle.putString(KEY_TITLE,title)
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
            tv_title.text = title
        }
    }

}