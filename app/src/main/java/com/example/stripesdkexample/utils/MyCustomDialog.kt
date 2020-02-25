package com.example.stripesdkexample.utils

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.stripesdkexample.R


class MyCustomDialog(context: Context) {

    private var mContext: Context = context
    private var Bt_action: Button
    private var Bt_dismiss: Button
    private var TextViewTitle: TextView
    private var TextViewMessage: TextView
    private var Rl_button: RelativeLayout
    private var dialog: Dialog
    private var view: View
    private var isPositiveAvailable = false
    private var isNegativeAvailable = false

    init {
        //--------Adjusting Dialog width-----
        val metrics = mContext.resources.displayMetrics
        val screenWidth = (metrics.widthPixels * 0.85).toInt()//fill only 85% of the screen

        view = View.inflate(mContext, R.layout.my_custom_dialog, null)
        dialog = Dialog(mContext)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setLayout(screenWidth, LinearLayout.LayoutParams.WRAP_CONTENT)

        TextViewTitle = view.findViewById(R.id.TextViewTitle)
        TextViewMessage = view.findViewById(R.id.TextViewMessage)
        Bt_action = view.findViewById(R.id.custom_dialog_library_ok_button) as Button
        Bt_dismiss = view.findViewById(R.id.custom_dialog_library_cancel_button) as Button
        Rl_button = view.findViewById(R.id.custom_dialog_library_button_layout)
    }


    fun show() {

        /*Enable or Disable positive Button*/
        if (isPositiveAvailable) {
            Bt_action.visibility = View.VISIBLE
        } else {
            Bt_action.visibility = View.GONE
        }

        /*Enable or Disable Negative Button*/
        if (isNegativeAvailable) {
            Bt_dismiss.visibility = View.VISIBLE
        } else {
            Bt_dismiss.visibility = View.GONE
        }

        /*Changing color for Button Layout*/
        if (isPositiveAvailable && isNegativeAvailable) {
            Rl_button.setBackgroundColor(mContext.resources.getColor(R.color.white))
        } else {
            Rl_button.setBackgroundColor(mContext.resources.getColor(R.color.colorPrimary))
        }

        dialog.show()
    }


    fun dismiss() {
        dialog.dismiss()
    }


    fun setDialogTitle(title: String) {
        TextViewTitle.text = title
    }


    fun setDialogMessage(message: String) {
        TextViewMessage.text = message
    }


    fun setCancelOnTouchOutside(value: Boolean) {
        dialog.setCanceledOnTouchOutside(value)
    }


    /*Action Button for Dialog*/
    fun setPositiveButton(text: String, listener: View.OnClickListener) {

        isPositiveAvailable = true

        Bt_action.text = text
        Bt_action.setOnClickListener(listener)
    }

    fun setNegativeButton(text: String, listener: View.OnClickListener) {
        isNegativeAvailable = true

        Bt_dismiss.text = text
        Bt_dismiss.setOnClickListener(listener)
    }
}