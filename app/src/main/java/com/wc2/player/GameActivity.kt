package com.wc2.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import java.io.File

class GameActivity : AppCompatActivity() {
    private var retroView: GLRetroView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cue = File(filesDir, "game").listFiles()?.firstOrNull { it.extension.equals("cue", true) }
        if (cue == null) {
            finish()
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContentView(R.layout.activity_game)

        val system = File(filesDir, "system").apply { mkdirs() }
        val saves = File(filesDir, "saves").apply { mkdirs() }

        val data = GLRetroViewData(this).apply {
            coreFilePath = "libpcsx_rearmed_libretro_android.so"
            gameFilePath = cue.absolutePath
            systemDirectory = system.absolutePath
            savesDirectory = saves.absolutePath
        }
        val view = GLRetroView(this, data)
        lifecycle.addObserver(view)
        retroView = view
        findViewById<FrameLayout>(R.id.videoContainer).addView(view)

        bind(R.id.btnCross, KeyEvent.KEYCODE_BUTTON_A)
        bind(R.id.btnCircle, KeyEvent.KEYCODE_BUTTON_B)
        bind(R.id.btnSquare, KeyEvent.KEYCODE_BUTTON_X)
        bind(R.id.btnTriangle, KeyEvent.KEYCODE_BUTTON_Y)
        bind(R.id.btnL1, KeyEvent.KEYCODE_BUTTON_L1)
        bind(R.id.btnL2, KeyEvent.KEYCODE_BUTTON_L2)
        bind(R.id.btnR1, KeyEvent.KEYCODE_BUTTON_R1)
        bind(R.id.btnR2, KeyEvent.KEYCODE_BUTTON_R2)
        bind(R.id.btnStart, KeyEvent.KEYCODE_BUTTON_START)
        bind(R.id.btnSelect, KeyEvent.KEYCODE_BUTTON_SELECT)

        findViewById<PadView>(R.id.pad).onKey = { code, down -> send(code, down) }
    }

    private fun send(code: Int, down: Boolean) {
        retroView?.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code)
    }

    private fun bind(id: Int, code: Int) {
        findViewById<View>(id).setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.isPressed = true; send(code, true) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; send(code, false) }
            }
            true
        }
    }
}
