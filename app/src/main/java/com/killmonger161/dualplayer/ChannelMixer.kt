package com.killmonger161.dualplayer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
class ChannelMixer(private val isLeftChannel: Boolean) : AudioProcessor {

    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        if (inputAudioFormat.channelCount < 1 || inputAudioFormat.channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        return AudioFormat(inputAudioFormat.sampleRate, 2, inputAudioFormat.encoding)
    }

    override fun isActive(): Boolean = inputFormat.channelCount == 1 || inputFormat.channelCount == 2

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }

        val bytesPerSample = if (inputFormat.encoding == C.ENCODING_PCM_16BIT) 2 else 4
        val outputBytesPerFrame = 2 * bytesPerSample // 2 channels for stereo output

        val frameCount = inputBuffer.remaining() / (inputFormat.channelCount * bytesPerSample)
        val outputSize = frameCount * outputBytesPerFrame

        if (outputBuffer.capacity() < outputSize) {
            outputBuffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        while (inputBuffer.hasRemaining()) {
            if (inputFormat.channelCount == 1) { // Mono input
                if (inputFormat.encoding == C.ENCODING_PCM_16BIT) {
                    val sample = inputBuffer.short
                    if (isLeftChannel) {
                        outputBuffer.putShort(sample)
                        outputBuffer.putShort(0)
                    } else {
                        outputBuffer.putShort(0)
                        outputBuffer.putShort(sample)
                    }
                } else { // PCM_FLOAT
                    val sample = inputBuffer.float
                    if (isLeftChannel) {
                        outputBuffer.putFloat(sample)
                        outputBuffer.putFloat(0f)
                    } else {
                        outputBuffer.putFloat(0f)
                        outputBuffer.putFloat(sample)
                    }
                }
            } else { // Stereo input
                if (inputFormat.encoding == C.ENCODING_PCM_16BIT) {
                    val left = inputBuffer.short
                    val right = inputBuffer.short
                    if (isLeftChannel) {
                        outputBuffer.putShort(left)
                        outputBuffer.putShort(0)
                    } else {
                        outputBuffer.putShort(0)
                        outputBuffer.putShort(right)
                    }
                } else { // PCM_FLOAT
                    val left = inputBuffer.float
                    val right = inputBuffer.float
                    if (isLeftChannel) {
                        outputBuffer.putFloat(left)
                        outputBuffer.putFloat(0f)
                    } else {
                        outputBuffer.putFloat(0f)
                        outputBuffer.putFloat(right)
                    }
                }
            }
        }
        outputBuffer.flip()
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun isEnded(): Boolean {
        return inputEnded && !outputBuffer.hasRemaining()
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
        inputFormat = AudioFormat.NOT_SET
    }
}