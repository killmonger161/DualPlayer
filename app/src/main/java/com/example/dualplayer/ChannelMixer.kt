package com.example.dualplayer

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
class ChannelMixer(private val channelToKeep: Int) : AudioProcessor {

    private var inputFormat: AudioFormat? = null
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    override fun configure(inputFormat: AudioFormat): AudioFormat {
        if (inputFormat.channelCount != 2) throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        this.inputFormat = inputFormat
        return inputFormat
    }

    override fun isActive(): Boolean = inputFormat != null

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (outputBuffer.capacity() < remaining) {
            outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        while (inputBuffer.hasRemaining()) {
            val left = inputBuffer.short
            val right = inputBuffer.short
            if (channelToKeep == 0) {
                outputBuffer.putShort(left)
                outputBuffer.putShort(0)
            } else {
                outputBuffer.putShort(0)
                outputBuffer.putShort(right)
            }
        }
        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    override fun queueEndOfStream() {}
    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }
    override fun isEnded(): Boolean = false
    override fun flush() { outputBuffer = AudioProcessor.EMPTY_BUFFER }
    override fun reset() {
        flush()
        inputFormat = null
    }
}