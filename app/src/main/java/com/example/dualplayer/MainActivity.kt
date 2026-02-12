package com.example.dualplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.concurrent.TimeUnit

@UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var playerA: ExoPlayer; private lateinit var playerB: ExoPlayer
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPlayA: ImageButton; private lateinit var btnPlayB: ImageButton
    private lateinit var seekA: SeekBar; private lateinit var seekB: SeekBar
    private lateinit var trackNameA: TextView; private lateinit var trackNameB: TextView
    private lateinit var tAStart: TextView; private lateinit var tAEnd: TextView
    private lateinit var tBStart: TextView; private lateinit var tBEnd: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Player A = Hard Left, Player B = Hard Right
        playerA = createPlayer(isLeft = true)
        playerB = createPlayer(isLeft = false)

        initViews()
        setupListeners()
        updateProgressLoop()
    }

    private fun initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPlayA = findViewById(R.id.btnPlayA)
        btnPlayB = findViewById(R.id.btnPlayB)
        seekA = findViewById(R.id.seekA); seekB = findViewById(R.id.seekB)
        tAStart = findViewById(R.id.timeA_start); tAEnd = findViewById(R.id.timeA_end)
        tBStart = findViewById(R.id.timeB_start); tBEnd = findViewById(R.id.timeB_end)
        trackNameA = findViewById(R.id.trackNameA); trackNameB = findViewById(R.id.trackNameB)
    }

    private fun createPlayer(isLeft: Boolean): ExoPlayer {
        val rf = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(c: Context, f: Boolean, p: Boolean): DefaultAudioSink {
                return DefaultAudioSink.Builder(c)
                    .setAudioProcessors(arrayOf(ChannelMixer(isLeft)))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this, rf).build()
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_NEVER)
            .build()

        player.setAudioAttributes(attrs, false)
        return player
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnLoadA).setOnClickListener { checkPermissionAndLoad(101) }
        findViewById<ImageButton>(R.id.btnLoadB).setOnClickListener { checkPermissionAndLoad(102) }

        btnPlayPause.setOnClickListener {
            val isAnyPlaying = playerA.isPlaying || playerB.isPlaying
            if (isAnyPlaying) {
                playerA.pause(); playerB.pause()
            } else {
                playerA.play(); playerB.play()
            }
        }

        btnPlayA.setOnClickListener { if (playerA.isPlaying) playerA.pause() else playerA.play() }
        btnPlayB.setOnClickListener { if (playerB.isPlaying) playerB.pause() else playerB.play() }

        // Sync Individual Play Buttons
        playerA.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                btnPlayA.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                updateMasterIcon()
            }
        })

        playerB.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                btnPlayB.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                updateMasterIcon()
            }
        })

        val seekListener = { p: ExoPlayer -> object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, prg: Int, f: Boolean) { if (f) p.seekTo(prg.toLong()) }
            override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {}
        }}
        seekA.setOnSeekBarChangeListener(seekListener(playerA))
        seekB.setOnSeekBarChangeListener(seekListener(playerB))
    }

    private fun updateMasterIcon() {
        val masterPlaying = playerA.isPlaying || playerB.isPlaying
        btnPlayPause.setImageResource(if (masterPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
    }

    private fun checkPermissionAndLoad(req: Int) {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) showInternalMusicSheet(req)
        else ActivityCompat.requestPermissions(this, arrayOf(perm), req)
    }

    private fun showInternalMusicSheet(req: Int) {
        val dialog = BottomSheetDialog(this)
        val viewSheet = layoutInflater.inflate(R.layout.music_list_sheet, null)
        val listView = viewSheet.findViewById<ListView>(R.id.music_list_view)

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    isDraggable = false
                }
            }
        }

        val musicList = scanMusic()
        listView.adapter = object : ArrayAdapter<AudioFile>(this, R.layout.list_item_audio, musicList) {
            override fun getView(pos: Int, conv: View?, parent: ViewGroup): View {
                val v = conv ?: layoutInflater.inflate(R.layout.list_item_audio, parent, false)
                v.findViewById<TextView>(R.id.song_title).text = getItem(pos)?.title
                v.findViewById<TextView>(R.id.song_artist).text = getItem(pos)?.artist
                return v
            }
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val item = MediaItem.fromUri(musicList[pos].uri)
            if (req == 101) {
                playerA.setMediaItem(item); playerA.prepare(); trackNameA.text = musicList[pos].title
            } else {
                playerB.setMediaItem(item); playerB.prepare(); trackNameB.text = musicList[pos].title
            }
            dialog.dismiss()
        }
        dialog.setContentView(viewSheet); dialog.show()
    }

    private fun scanMusic(): List<AudioFile> {
        val list = mutableListOf<AudioFile>()
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val path = c.getString(3) ?: ""
                if (!path.endsWith(".aac", true) && !path.endsWith(".m4a", true)) {
                    list.add(AudioFile(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0).toString()), c.getString(1), c.getString(2)))
                }
            }
        }
        return list
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

    private fun formatTime(ms: Long) = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(ms), TimeUnit.MILLISECONDS.toSeconds(ms) % 60)
    data class AudioFile(val uri: Uri, val title: String, val artist: String)
}