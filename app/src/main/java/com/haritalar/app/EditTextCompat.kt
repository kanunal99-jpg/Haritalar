package com.haritalar.app

import android.widget.EditText

/** Compatibility shim for the search UI's singleLine assignment. */
var EditText.singleLine: Boolean
    get() = maxLines == 1
    set(value) {
        setSingleLine(value)
    }
