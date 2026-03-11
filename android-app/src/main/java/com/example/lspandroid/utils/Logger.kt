package com.example.lspandroid.utils

import android.util.Log

/**
 * Comprehensive logging utility for the LSP Android app.
 */
class Logger {
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun d(message: String) = d("LSP", message)
    fun i(message: String) = i("LSP", message)
    fun w(message: String) = w("LSP", message)
    fun e(message: String, throwable: Throwable? = null) = e("LSP", message, throwable)
}
