package com.example.dualplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.util.concurrent.TimeUnit

@UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var playerA: ExoPlayer
    private lateinit var playerB: ExoPlayer
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekA: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var tAStart: TextView; private lateinit var tAEnd: TextView
    private lateinit var tBStart: TextView; private lateinit var tBEnd: TextView

    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerA = createPlayer(0)
        playerB = createPlayer(1)

        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekA = findViewById(R.id.seekA); seekB = findViewById(R.id.seekB)
        tAStart = findViewById(R.id.timeA_start); tAEnd = findViewById(R.id.timeA_end)
        tBStart = findViewById(R.id.timeB_start); tBEnd = findViewById(R.id.timeB_end)

        findViewById<ImageButton>(R.id.btnLoadA).setOnClickListener { pickFile(101) }
        findViewById<ImageButton>(R.id.btnLoadB).setOnClickListener { pickFile(102) }

        btnPlayPause.setOnClickListener { toggleMasterPlayback() }
        findViewById<ImageButton>(R.id.btnPlayA).setOnClickListener { togglePlayer(playerA, it as ImageButton) }
        findViewById<ImageButton>(R.id.btnPlayB).setOnClickListener { togglePlayer(playerB, it as ImageButton) }

        setupVolumeControls()
        setupSeekListeners()
        updateProgressLoop()
    }

    private fun createPlayer(channel: Int): ExoPlayer {
        val rf = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(c: Context, f: Boolean, p: Boolean): DefaultAudioSink {
                return DefaultAudioSink.Builder(c).setAudioProcessors(arrayOf(ChannelMixer(channel))).build()
            }
        }
        return ExoPlayer.Builder(this, rf).build()
    }

    private fun setupVolumeControls() {
        val volumeA = findViewById<SeekBar>(R.id.volumeA)
        val volumeB = findViewById<SeekBar>(R.id.volumeB)

        val volumeUpdate = { p: ExoPlayer -> object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, prog: Int, fromUser: Boolean) {
                if (fromUser) p.volume = prog / 100f
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }}

        volumeA.setOnSeekBarChangeListener(volumeUpdate(playerA))
        volumeB.setOnSeekBarChangeListener(volumeUpdate(playerB))
    }

    private fun setupSeekListeners() {
        val seekListener = { p: ExoPlayer -> object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, prg: Int, f: Boolean) { if (f) p.seekTo(prg.toLong()) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }}
        seekA.setOnSeekBarChangeListener(seekListener(playerA))
        seekB.setOnSeekBarChangeListener(seekListener(playerB))
    }

    private fun toggleMasterPlayback() {
        if (playerA.mediaItemCount == 0 && playerB.mediaItemCount == 0) return
        if (isPlaying) {
            playerA.pause(); playerB.pause()
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        } else {
            playerA.play(); playerB.play()
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        }
        isPlaying = !isPlaying
    }

    private fun togglePlayer(p: ExoPlayer, btn: ImageButton) {
        if (p.mediaItemCount == 0) return
        if (p.isPlaying) {
            p.pause(); btn.setImageResource(android.R.drawable.ic_media_play)
        } else {
            p.play(); btn.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun formatTime(ms: Long): String {
        val min = TimeUnit.MILLISECONDS.toMinutes(ms)
        val sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", min, sec)
    }

    private fun updateProgressLoop() {
        if (playerA.duration > 0) {
            seekA.max = playerA.duration.toInt()
            seekA.progress = playerA.currentPosition.toInt()
            tAStart.text = formatTime(playerA.currentPosition)
            tAEnd.text = formatTime(playerA.duration)
        }
        if (playerB.duration > 0) {
            seekB.max = playerB.duration.toInt()
            seekB.progress = playerB.currentPosition.toInt()
            tBStart.text = formatTime(playerB.currentPosition)
            tBEnd.text = formatTime(playerB.duration)
        }
        handler.postDelayed({ updateProgressLoop() }, 500)
    }

    private fun pickFile(req: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }
        startActivityForResult(intent, req)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res == RESULT_OK && data?.data != null) {
            val item = MediaItem.fromUri(data.data!!)
            if (req == 101) {
                playerA.setMediaItem(item); playerA.prepare()
            } else {
                playerB.setMediaItem(item); playerB.prepare()
            }
        }
    }
}
