package com.example.stripesdkexample.utils

import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.stripesdkexample.R

class SupportClass(var mContext: Context) {
    lateinit var dialog: Dialog

    fun checkInternetStatus(): Boolean {
        val connectivity =
            mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = connectivity.allNetworkInfo
        if (info != null)
            for (i in info.indices)
                if (info[i].state == NetworkInfo.State.CONNECTED) {
                    return true
                }

        return false
    }

    fun showNoInternetConnectAlert() {
        val myCustomDialog = MyCustomDialog(mContext)
        myCustomDialog.setDialogTitle(mContext.getString(R.string.alert_title))
        myCustomDialog.setDialogMessage(mContext.getString(R.string.no_internet_connection))
        myCustomDialog.setPositiveButton(mContext.getString(R.string.ok), View.OnClickListener {
            myCustomDialog.dismiss()
        })
        myCustomDialog.show()
    }

    fun displayLoader(LoadingMessage: String) {
        val builder = AlertDialog.Builder(mContext)
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.loader_progress, null)
        dialogView.findViewById<TextView>(R.id.TextViewLoaderMessage).text = LoadingMessage
        builder.setView(dialogView)
        builder.setCancelable(false)
        dialog = builder.create()
        dialog.show()
    }

    fun dismissLoader() {
        dialog.dismiss()
    }

    fun shortToast(message: String) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT)
            .show()
    }
}