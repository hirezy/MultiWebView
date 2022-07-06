package com.hirezy.web

/**
 * Created by hirezy on 2020/6/30.
 */
abstract class OnTitleProgressCallback {
    open fun onReceivedTitle(title: String?) {}
    fun onProgressChanged(newProgress: Int) {}
}