package com.uu_uce.gestureDetection

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

/**
 * performs action on double tap
 * @param[parent] the context this lives in
 * @param[action] action to be performed after a double tap
 * @constructor creates a DoubleTapper TouchChild
 */
class DoubleTapper(
    parent: Context,
    var action: () -> Unit)
    : TouchChild,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener{
    private var gestureDetector: GestureDetectorCompat? = null

    init{
        gestureDetector = GestureDetectorCompat(parent, this)
        gestureDetector?.setOnDoubleTapListener(this)
    }

    override fun getOnTouchEvent(event: MotionEvent) {
        gestureDetector?.onTouchEvent(event)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        action()
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean { return true }
    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean { return true }
    override fun onShowPress(e: MotionEvent?) {}
    override fun onSingleTapUp(e: MotionEvent?): Boolean { return true }
    override fun onDown(e: MotionEvent?): Boolean { return false }
    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean { return false }
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean { return false }
    override fun onLongPress(e: MotionEvent?) {}
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

