package com.wc2.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class PadView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    var onKey: ((Int, Boolean) -> Unit)? = null
    private val active = HashSet<Int>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy)
        paint.style = Paint.Style.FILL
        paint.color = 0x33FFFFFF
        canvas.drawCircle(cx, cy, r, paint)
        val d = r * 0.62f
        val dot = r * 0.16f
        fun dotAt(x: Float, y: Float, key: Int) {
            paint.color = if (active.contains(key)) 0xDDFFFFFF.toInt() else 0x77FFFFFF
            canvas.drawCircle(x, y, dot, paint)
        }
        dotAt(cx, cy - d, KeyEvent.KEYCODE_DPAD_UP)
        dotAt(cx, cy + d, KeyEvent.KEYCODE_DPAD_DOWN)
        dotAt(cx - d, cy, KeyEvent.KEYCODE_DPAD_LEFT)
        dotAt(cx + d, cy, KeyEvent.KEYCODE_DPAD_RIGHT)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> update(e.x, e.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> release()
        }
        return true
    }

    private fun update(x: Float, y: Float) {
        val dx = (x - width / 2f) / (width / 2f)
        val dy = (y - height / 2f) / (height / 2f)
        val want = HashSet<Int>()
        if (dx > 0.3f) want.add(KeyEvent.KEYCODE_DPAD_RIGHT)
        if (dx < -0.3f) want.add(KeyEvent.KEYCODE_DPAD_LEFT)
        if (dy > 0.3f) want.add(KeyEvent.KEYCODE_DPAD_DOWN)
        if (dy < -0.3f) want.add(KeyEvent.KEYCODE_DPAD_UP)
        for (k in active.toList()) if (!want.contains(k)) { active.remove(k); onKey?.invoke(k, false) }
        for (k in want) if (!active.contains(k)) { active.add(k); onKey?.invoke(k, true) }
        invalidate()
    }

    private fun release() {
        for (k in active.toList()) onKey?.invoke(k, false)
        active.clear()
        invalidate()
    }
}
