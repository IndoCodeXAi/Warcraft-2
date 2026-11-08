package com.wc2.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var importBtn: Button
    private lateinit var playBtn: Button

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) importFolder(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        importBtn = findViewById(R.id.btnImport)
        playBtn = findViewById(R.id.btnPlay)
        importBtn.setOnClickListener { picker.launch(null) }
        playBtn.setOnClickListener { startActivity(Intent(this, GameActivity::class.java)) }
        status.text = if (isReady()) "Game siap dimainkan." else
            "Belum ada game.\nPilih folder yang berisi file .cue dan .bin WarCraft II."
        refreshButtons()
    }

    private fun gameDir() = File(filesDir, "game")

    private fun isReady() = gameDir().listFiles()?.any { it.extension.equals("cue", true) } == true

    private fun refreshButtons() {
        playBtn.isEnabled = isReady()
        importBtn.isEnabled = true
    }

    private fun setStatus(t: String) = runOnUiThread { status.text = t }

    private fun isWc2(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("warcraft ii") && n.contains("tides of darkness")
    }

    private fun importFolder(uri: Uri) {
        importBtn.isEnabled = false
        playBtn.isEnabled = false
        Thread {
            try {
                doImport(uri)
            } catch (e: Exception) {
                setStatus("Gagal: ${e.message}")
            }
            runOnUiThread { refreshButtons() }
        }.start()
    }

    private fun doImport(uri: Uri) {
        val dir = DocumentFile.fromTreeUri(this, uri) ?: throw Exception("Folder tidak bisa dibuka")
        val files = dir.listFiles()
        val cues = files.filter { it.name?.endsWith(".cue", true) == true }
        if (cues.isEmpty()) throw Exception("Tidak ada file .cue di folder ini")
        val cue = cues.firstOrNull { isWc2(it.name!!) }
            ?: throw Exception("Aplikasi ini hanya untuk WarCraft II - Tides of Darkness")

        val text = contentResolver.openInputStream(cue.uri)!!.use { it.bufferedReader().readText() }
        val names = Regex("FILE\\s+\"([^\"]+)\"", RegexOption.IGNORE_CASE)
            .findAll(text).map { it.groupValues[1] }.distinct().toList()
        if (names.isEmpty() || !names.all { isWc2(it) }) throw Exception("Isi .cue bukan WarCraft II")

        val missing = names.filter { n -> files.none { it.name == n } }
        if (missing.isNotEmpty()) throw Exception("File .bin kurang (${missing.size}), contoh: ${missing.first()}")

        val tmp = File(filesDir, "game_tmp")
        tmp.deleteRecursively()
        tmp.mkdirs()
        val all = listOf(cue.name!! to cue) + names.map { n -> n to files.first { it.name == n } }
        all.forEachIndexed { i, (name, doc) ->
            setStatus("Menyalin ${i + 1}/${all.size}\n$name")
            contentResolver.openInputStream(doc.uri)!!.use { ins ->
                File(tmp, File(name).name).outputStream().use { out -> ins.copyTo(out, 1 shl 20) }
            }
        }
        gameDir().deleteRecursively()
        if (!tmp.renameTo(gameDir())) throw Exception("Gagal menyimpan game")
        setStatus("Selesai. Game siap dimainkan.")
    }
}
