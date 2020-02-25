package com.example.stripesdkexample.widgets

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class CustomEditTextBold : AppCompatEditText {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }


    fun init() {
        val tf = Typeface.createFromAsset(context.resources.assets, "fonts/Roboto-Medium.ttf")
        typeface = tf
    }
}