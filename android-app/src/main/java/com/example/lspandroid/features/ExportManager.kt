package com.example.lspandroid.features

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Professional audio export manager for WAV and FLAC formats.
 * 
 * Requirement 6.4: Additional Features - Export to WAV/FLAC
 * 
 * Features:
 * - High-quality WAV export with 16/24/32-bit depth support
 * - FLAC compression with configurable quality levels
 * - Real-time progress tracking with accurate byte counting
 * - Robust error handling and recovery
 * - Memory-efficient streaming processing
 * - Proper file header management and metadata
 * - Thread-safe cancellation support
 */
class ExportManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ExportManager"
        private const val DEFAULT_BUFFER_SIZE = 8192
        private const val WAV_HEADER_SIZE = 44
        private const val FLAC_TIMEOUT_MS = 10000L
        private const val MAX_RETRIES = 3
    }
    
    enum class ExportFormat {
        WAV, FLAC
    }
    
    enum class FlacCompressionLevel(val value: Int) {
        FASTEST(0),
        FAST(2),
        NORMAL(5),
        HIGH(7),
        HIGHEST(8)
    }
    
    data class ExportConfig(
        val format: ExportFormat,
        val sampleRate: Int = 48000,
        val bitDepth: Int = 24,
        val channels: Int = 2,
        val flacCompressionLevel: FlacCompressionLevel = FlacCompressionLevel.NORMAL,
        val bufferSize: Int = DEFAULT_BUFFER_SIZE,
        val enableDithering: Boolean = true,
        val normalizeAudio: Boolean = false,
        val targetLufs: Float = -23.0f
    ) {
        init {
            require(sampleRate in 8000..192000) { "Sample rate must be between 8kHz and 192kHz" }
            require(bitDepth in listOf(16, 24, 32)) { "Bit depth must be 16, 24, or 32" }
            require(channels in 1..8) { "Channels must be between 1 and 8" }
            require(bufferSize > 0) { "Buffer size must be positive" }
        }
    
        val bytesPerSample: Int get() = bitDepth / 8
        val blockAlign: Int get() = channels * bytesPerSample
        val byteRate: Int get() = sampleRate * blockAlign
    }
    
    interface ExportProgressListener {
        fun onProgress(progress: Float, bytesWritten: Long, estimatedTotalBytes: Long)
        fun onComplete(file: File, actualBytes: Long, durationMs: Long)
        fun onError(error: Exception)
        fun onCancelled()
    }
    
    data class ExportResult(
        val success: Boolean,
        val outputFile: File?,
        val bytesWritten: Long,
        val durationMs: Long,
        val error: Exception? = null
        )
    
    private val activeExports = mutableSetOf<String>()
    private val cancellationTokens = mutableMapOf<String, AtomicBoolean>()
    /**
     * Export processed audio to file with comprehensive error handling and progress tracking.
     */
    suspend fun exportAudio(
        outputFile: File,
        config: ExportConfig,
        audioProvider: suspend (FloatArray, Long) -> AudioChunk,
        progressListener: ExportProgressListener? = null,
        exportId: String = outputFile.name
    ): ExportResult = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        val cancellationToken = AtomicBoolean(false)
        
        synchronized(activeExports) {
            if (activeExports.contains(exportId)) {
                return@withContext ExportResult(
                    false, null, 0, 0,
                    IllegalStateException("Export with ID $exportId is already in progress")
                )
            }
            activeExports.add(exportId)
            cancellationTokens[exportId] = cancellationToken
        }
        
        try {
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()
            
            // Validate audio provider
            val testBuffer = FloatArray(config.bufferSize * config.channels)
            val testChunk = audioProvider(testBuffer, 0)
            if (testChunk.samples.isEmpty()) {
                throw IllegalArgumentException("Audio provider returned empty data")
            }
            
            val result = when (config.format) {
                ExportFormat.WAV -> exportWavAdvanced(
                    outputFile, config, audioProvider, progressListener, cancellationToken
                )
                ExportFormat.FLAC -> exportFlacAdvanced(
                    outputFile, config, audioProvider, progressListener, cancellationToken
                )
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            if (cancellationToken.get()) {
                outputFile.delete()
                progressListener?.onCancelled()
                ExportResult(false, null, 0, duration)
            } else {
                progressListener?.onComplete(outputFile, result.bytesWritten, duration)
                ExportResult(true, outputFile, result.bytesWritten, duration)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Export failed for $exportId", e)
            outputFile.delete()
            progressListener?.onError(e)
            ExportResult(false, null, 0, System.currentTimeMillis() - startTime, e)
        } finally {
            synchronized(activeExports) {
                activeExports.remove(exportId)
                cancellationTokens.remove(exportId)
            }
        }
    }
    /**
     * Cancel an active export operation.
     */
    fun cancelExport(exportId: String): Boolean {
        return cancellationTokens[exportId]?.let { token ->
            token.set(true)
            true
        } ?: false
    }
    /**
     * Advanced WAV export with proper header management and high-quality audio processing.
     */
    private suspend fun exportWavAdvanced(
        outputFile: File,
        config: ExportConfig,
        audioProvider: suspend (FloatArray, Long) -> AudioChunk,
        progressListener: ExportProgressListener?,
        cancellationToken: AtomicBoolean
    ): ExportInternalResult {
        
        val audioBuffer = FloatArray(config.bufferSize * config.channels)
        val totalBytesWritten = AtomicLong(0)
        var frameIndex = 0L
        var peakLevel = 0f
        var rmsSum = 0.0
        var sampleCount = 0L
        
        RandomAccessFile(outputFile, "rw").use { raf ->
            // Write placeholder header
            raf.seek(0)
            writeWavHeader(raf, 0, config)
            totalBytesWritten.addAndGet(WAV_HEADER_SIZE.toLong())
            
            var hasMore = true
            var retryCount = 0
            
            while (hasMore && !cancellationToken.get()) {
                try {
                    val chunk = audioProvider(audioBuffer, frameIndex)
                    hasMore = chunk.hasMore
                    
                    if (chunk.samples.isNotEmpty()) {
                        // Audio processing
                        val processedSamples = if (config.normalizeAudio) {
                            normalizeAudio(chunk.samples, config.targetLufs)
                        } else {
                            chunk.samples
                        }
                        
                        // Update audio statistics
                        for (sample in processedSamples) {
                            val abs = kotlin.math.abs(sample)
                            if (abs > peakLevel) peakLevel = abs
                            rmsSum += (sample * sample).toDouble()
                            sampleCount++
                        }
                        
                        // Convert to PCM with dithering if enabled
                        val pcmData = if (config.enableDithering) {
                            floatToPcmWithDithering(processedSamples, config.bitDepth)
                        } else {
                            floatToPcm(processedSamples, config.bitDepth)
                        }
                        
                        raf.write(pcmData)
                        totalBytesWritten.addAndGet(pcmData.size.toLong())
                        
                        frameIndex += chunk.samples.size / config.channels
                        
                        // Progress reporting
                        val estimatedTotal = if (chunk.estimatedTotalFrames > 0) {
                            chunk.estimatedTotalFrames * config.blockAlign + WAV_HEADER_SIZE
                        } else {
                            totalBytesWritten.get() * 2 // Rough estimate
                        }
                        
                        val progress = if (chunk.estimatedTotalFrames > 0) {
                            (frameIndex.toFloat() / chunk.estimatedTotalFrames).coerceIn(0f, 1f)
                        } else {
                            0.5f // Unknown progress
                        }
                        
                        progressListener?.onProgress(progress, totalBytesWritten.get(), estimatedTotal)
                    }
                    
                    retryCount = 0 // Reset on success
                    
                } catch (e: Exception) {
                    if (++retryCount >= MAX_RETRIES) {
                        throw e
                    }
                    Log.w(TAG, "Retry $retryCount for frame $frameIndex", e)
                    delay(100 * retryCount) // Exponential backoff
                }
            }
            
            // Update header with final size
            val dataSize = totalBytesWritten.get() - WAV_HEADER_SIZE
            raf.seek(0)
            writeWavHeader(raf, dataSize, config)
            
            Log.i(TAG, "WAV export completed: ${totalBytesWritten.get()} bytes, peak: $peakLevel, RMS: ${kotlin.math.sqrt(rmsSum / sampleCount)}")
        }
        
        return ExportInternalResult(totalBytesWritten.get())
    }
    /**
     * Advanced FLAC export using MediaCodec with proper configuration and error handling.
     */
    private suspend fun exportFlacAdvanced(
        outputFile: File,
        config: ExportConfig,
        audioProvider: suspend (FloatArray, Long) -> AudioChunk,
        progressListener: ExportProgressListener?,
        cancellationToken: AtomicBoolean
    ): ExportInternalResult {
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw UnsupportedOperationException("FLAC encoding requires Android API 21+")
        }
        
        val mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_FLAC,
            config.sampleRate,
            config.channels
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, calculateFlacBitrate(config))
            setInteger(MediaFormat.KEY_PCM_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT)
            setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, config.flacCompressionLevel.value)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, config.bufferSize * config.channels * 2)
        }
        
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_FLAC)
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val totalBytesWritten = AtomicLong(0)
        
        try {
            val audioBuffer = FloatArray(config.bufferSize * config.channels)
            var frameIndex = 0L
            var hasMore = true
            var inputEOS = false
            var outputEOS = false
            
            while ((!outputEOS || !inputEOS) && !cancellationToken.get()) {
                
                // Feed input data
                if (!inputEOS && hasMore) {
                    val inputBufferIndex = codec.dequeueInputBuffer(FLAC_TIMEOUT_MS)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        
                        val chunk = audioProvider(audioBuffer, frameIndex)
                        hasMore = chunk.hasMore
                        
                        if (chunk.samples.isNotEmpty()) {
                            val processedSamples = if (config.normalizeAudio) {
                                normalizeAudio(chunk.samples, config.targetLufs)
                            } else {
                                chunk.samples
                            }
                            
                            val pcmData = floatToPcm16(processedSamples)
                            inputBuffer?.put(pcmData)
                            
                            val presentationTimeUs = frameIndex * 1000000L / config.sampleRate
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                pcmData.size,
                                presentationTimeUs,
                                if (!hasMore) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                            )
                            
                            frameIndex += chunk.samples.size / config.channels
                            
                            if (!hasMore) {
                                inputEOS = true
                            }
                        } else if (!hasMore) {
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEOS = true
                        }
                    }
                }
                
                // Process output data
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, FLAC_TIMEOUT_MS)
                
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        trackIndex = muxer.addTrack(outputFormat)
                        muxer.start()
                        muxerStarted = true
                        Log.d(TAG, "FLAC output format: $outputFormat")
                    }
                    
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        
                        if (outputBuffer != null && muxerStarted && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            
                            muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                            totalBytesWritten.addAndGet(bufferInfo.size.toLong())
                        }
                        
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEOS = true
                        }
                        
                        // Progress reporting
                        val progress = if (inputEOS) 1f else 0.8f // Encoding progress estimate
                        progressListener?.onProgress(progress, totalBytesWritten.get(), totalBytesWritten.get() * 2)
                    }
                }
            }
            
        } finally {
            try {
                codec.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping codec", e)
            }
            
            try {
                codec.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing codec", e)
            }
            
            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing muxer", e)
            }
        }
        
        Log.i(TAG, "FLAC export completed: ${totalBytesWritten.get()} bytes")
        return ExportInternalResult(totalBytesWritten.get())
    }
    
    /**
     * Write comprehensive WAV file header with proper chunk structure.
     */
    private fun writeWavHeader(raf: RandomAccessFile, dataSize: Long, config: ExportConfig) {
        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        
        // RIFF chunk descriptor
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt((36 + dataSize).toInt()) // File size - 8
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        
        // fmt sub-chunk
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Sub-chunk size
        header.putShort(1) // Audio format (PCM)
        header.putShort(config.channels.toShort())
        header.putInt(config.sampleRate)
        header.putInt(config.byteRate)
        header.putShort(config.blockAlign.toShort())
        header.putShort(config.bitDepth.toShort())
        
        // data sub-chunk
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize.toInt())
        
        raf.write(header.array())
    }
    
    /**
     * Convert float samples to PCM with specified bit depth.
     */
    private fun floatToPcm(samples: FloatArray, bitDepth: Int): ByteArray {
        return when (bitDepth) {
            16 -> floatToPcm16(samples)
            24 -> floatToPcm24(samples)
            32 -> floatToPcm32(samples)
            else -> throw IllegalArgumentException("Unsupported bit depth: $bitDepth")
        }
    }
    
    /**
     * Convert float samples to 16-bit PCM with proper clamping.
     */
    private fun floatToPcm16(samples: FloatArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            val clamped = sample.coerceIn(-1f, 1f)
            val pcm = (clamped * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(pcm)
        }
        
        return bytes
    }
    
    /**
     * Convert float samples to 24-bit PCM with proper scaling.
     */
    private fun floatToPcm24(samples: FloatArray): ByteArray {
        val bytes = ByteArray(samples.size * 3)
        var offset = 0
        
        for (sample in samples) {
            val clamped = sample.coerceIn(-1f, 1f)
            val pcm = (clamped * 8388607f).toInt().coerceIn(-8388608, 8388607)
            
            bytes[offset++] = (pcm and 0xFF).toByte()
            bytes[offset++] = ((pcm shr 8) and 0xFF).toByte()
            bytes[offset++] = ((pcm shr 16) and 0xFF).toByte()
}
        return bytes
    }
    
    /**
     * Convert float samples to 32-bit PCM (float format).
     */
    private fun floatToPcm32(samples: FloatArray): ByteArray {
        val bytes = ByteArray(samples.size * 4)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        
        for (sample in samples) {
            buffer.putFloat(sample.coerceIn(-1f, 1f))
        }
        
        return bytes
    }
    
    /**
     * Convert float samples to PCM with triangular dithering for better quality.
     */
    private fun floatToPcmWithDithering(samples: FloatArray, bitDepth: Int): ByteArray {
        val ditheredSamples = FloatArray(samples.size)
        val ditherAmount = 1f / (1 shl bitDepth)
        
        for (i in samples.indices) {
            val dither = (Math.random().toFloat() - Math.random().toFloat()) * ditherAmount
            ditheredSamples[i] = samples[i] + dither
        }
        
        return floatToPcm(ditheredSamples, bitDepth)
    }
    
    /**
     * Normalize audio to target LUFS level.
     */
    private fun normalizeAudio(samples: FloatArray, targetLufs: Float): FloatArray {
        // Simple peak normalization (real LUFS would require more complex analysis)
        var peak = 0f
        for (sample in samples) {
            val abs = kotlin.math.abs(sample)
            if (abs > peak) peak = abs
        }
        
        if (peak > 0f) {
            val targetPeak = kotlin.math.pow(10.0, targetLufs / 20.0).toFloat()
            val gain = min(targetPeak / peak, 1f)
            
            return FloatArray(samples.size) { i -> samples[i] * gain }
        }
        
        return samples
    }
    
    /**
     * Calculate appropriate bitrate for FLAC encoding.
     */
    private fun calculateFlacBitrate(config: ExportConfig): Int {
        val baseBitrate = config.sampleRate * config.channels * config.bitDepth
        return when (config.flacCompressionLevel) {
            FlacCompressionLevel.FASTEST -> (baseBitrate * 0.8).toInt()
            FlacCompressionLevel.FAST -> (baseBitrate * 0.6).toInt()
            FlacCompressionLevel.NORMAL -> (baseBitrate * 0.5).toInt()
            FlacCompressionLevel.HIGH -> (baseBitrate * 0.4).toInt()
            FlacCompressionLevel.HIGHEST -> (baseBitrate * 0.3).toInt()
        }
    }
    
    /**
     * Data class representing an audio chunk from the provider.
     */
    data class AudioChunk(
        val samples: FloatArray,
        val hasMore: Boolean,
        val estimatedTotalFrames: Long = 0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as AudioChunk
            
            if (!samples.contentEquals(other.samples)) return false
            if (hasMore != other.hasMore) return false
            if (estimatedTotalFrames != other.estimatedTotalFrames) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = 31 * result + hasMore.hashCode()
            result = 31 * result + estimatedTotalFrames.hashCode()
            return result
        }
    }
    
    /**
     * Internal result for tracking export progress.
     */
    private data class ExportInternalResult(
        val bytesWritten: Long
    )
}
