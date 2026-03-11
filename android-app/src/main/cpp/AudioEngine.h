#pragma once

#include <atomic>
#include <memory>
#include <mutex>
#include <condition_variable>
#include <thread>
#include <chrono>
#include <vector>
#include <algorithm>
#include <cstring>
#include <queue>
#include <functional>
#include <unordered_map>
#include <array>
#include <cmath>

#include "oboe/Oboe.h"
#include "../../../dsp-core/lsp_android_bridge.h"
#include "ParameterQueue.h"

/**
 * Audio routing mode enumeration for comprehensive audio routing configuration.
 */
enum class AudioRoutingMode {
    Mono,           // Single channel routing
    Stereo,         // Stereo (left/right) routing
    MidSide,        // Mid-side encoding/decoding
    Surround5_1,    // 5.1 surround sound
    Surround7_1,    // 7.1 surround sound
    Binaural,       // Binaural processing for headphones
    Ambisonics      // Ambisonic spatial audio
};

/**
 * Audio quality profiles for different use cases
 */
enum class AudioQualityProfile {
    LowLatency,     // Optimized for minimal latency
    HighQuality,    // Optimized for audio quality
    Balanced,       // Balance between latency and quality
    PowerSaving,    // Optimized for battery life
    Professional    // Professional audio production
};

/**
 * Audio processing effects chain
 */
enum class AudioEffect {
    None = 0,
    Reverb = 1 << 0,
    Delay = 1 << 1,
    Chorus = 1 << 2,
    Compressor = 1 << 3,
    EQ = 1 << 4,
    Limiter = 1 << 5,
    NoiseGate = 1 << 6,
    Distortion = 1 << 7
};

/**
 * Comprehensive audio performance metrics for monitoring and optimization
 */
struct AudioPerformanceMetrics {
    std::atomic<uint64_t> totalCallbacks{0};
    std::atomic<uint64_t> underruns{0};
    std::atomic<uint64_t> overruns{0};
    std::atomic<uint64_t> glitches{0};
    std::atomic<double> averageLatency{0.0};
    std::atomic<double> minLatency{std::numeric_limits<double>::max()};
    std::atomic<double> maxLatency{0.0};
    std::atomic<double> cpuLoad{0.0};
    std::atomic<double> memoryUsage{0.0};
    std::atomic<double> thermalState{0.0};
    std::atomic<uint32_t> sampleRate{0};
    std::atomic<uint32_t> bufferSize{0};
    std::atomic<uint32_t> activeChannels{0};
    std::chrono::steady_clock::time_point lastMetricsUpdate;
    std::chrono::steady_clock::time_point startTime;
    
    // Real-time statistics
    std::array<std::atomic<double>, 10> latencyHistory{};
    std::atomic<size_t> historyIndex{0};
    
    void reset() {
        totalCallbacks = 0;
        underruns = 0;
        overruns = 0;
        glitches = 0;
        averageLatency = 0.0;
        minLatency = std::numeric_limits<double>::max();
        maxLatency = 0.0;
        cpuLoad = 0.0;
        memoryUsage = 0.0;
        thermalState = 0.0;
        historyIndex = 0;
        for (auto& val : latencyHistory) {
            val = 0.0;
        }
        startTime = std::chrono::steady_clock::now();
    }
};
/**
 * Advanced audio buffer configuration for different routing modes and quality profiles
 */
struct AudioBufferConfig {
    int32_t inputChannels{2};
    int32_t outputChannels{2};
    int32_t sampleRate{48000};
    int32_t framesPerBurst{192};
    int32_t bufferCapacityInFrames{0};
    oboe::AudioFormat format{oboe::AudioFormat::Float};
    oboe::PerformanceMode performanceMode{oboe::PerformanceMode::LowLatency};
    oboe::SharingMode sharingMode{oboe::SharingMode::Exclusive};
    oboe::Usage usage{oboe::Usage::Media};
    oboe::ContentType contentType{oboe::ContentType::Music};
    oboe::InputPreset inputPreset{oboe::InputPreset::Generic};
    AudioQualityProfile qualityProfile{AudioQualityProfile::Balanced};
    bool enableEffects{false};
    uint32_t effectsMask{0};
    
    bool isValid() const {
        return inputChannels > 0 && outputChannels > 0 && 
               sampleRate > 0 && framesPerBurst > 0;
    }
};

/**
 * Audio device information with extended capabilities
 */
struct ExtendedAudioDeviceInfo {
    int32_t id;
    std::string name;
    std::string address;
    oboe::AudioDeviceInfo::Type type;
    oboe::AudioDeviceInfo::Direction direction;
    std::vector<int32_t> supportedSampleRates;
    std::vector<oboe::AudioFormat> supportedFormats;
    std::vector<oboe::ChannelMask> supportedChannelMasks;
    int32_t maxChannels;
    bool isLowLatencySupported;
    bool isProAudioSupported;
    double estimatedLatencyMs;
};

/**
 * Real-time audio processing context
 */
struct AudioProcessingContext {
    float* inputBuffer;
    float* outputBuffer;
    int32_t numFrames;
    int32_t inputChannels;
    int32_t outputChannels;
    int32_t sampleRate;
    AudioRoutingMode routingMode;
    uint32_t effectsMask;
    std::chrono::steady_clock::time_point timestamp;
    
    AudioProcessingContext(float* in, float* out, int32_t frames, 
                          int32_t inCh, int32_t outCh, int32_t sr, 
                          AudioRoutingMode mode, uint32_t effects)
        : inputBuffer(in), outputBuffer(out), numFrames(frames),
          inputChannels(inCh), outputChannels(outCh), sampleRate(sr),
          routingMode(mode), effectsMask(effects),
          timestamp(std::chrono::steady_clock::now()) {}
};

/**
 * Thread-safe circular buffer for audio data
 */
template<typename T>
class CircularBuffer {
private:
    std::vector<T> mBuffer;
    std::atomic<size_t> mWriteIndex{0};
    std::atomic<size_t> mReadIndex{0};
    size_t mCapacity;
    
public:
    explicit CircularBuffer(size_t capacity) 
        : mBuffer(capacity), mCapacity(capacity) {}
    
    bool write(const T* data, size_t count) {
        size_t writeIdx = mWriteIndex.load();
        size_t readIdx = mReadIndex.load();
        size_t available = (readIdx + mCapacity - writeIdx - 1) % mCapacity;
        
        if (count > available) return false;
        
        for (size_t i = 0; i < count; ++i) {
            mBuffer[writeIdx] = data[i];
            writeIdx = (writeIdx + 1) % mCapacity;
        }
        
        mWriteIndex.store(writeIdx);
        return true;
    }
    
    bool read(T* data, size_t count) {
        size_t readIdx = mReadIndex.load();
        size_t writeIdx = mWriteIndex.load();
        size_t available = (writeIdx + mCapacity - readIdx) % mCapacity;
        
        if (count > available) return false;
        
        for (size_t i = 0; i < count; ++i) {
            data[i] = mBuffer[readIdx];
            readIdx = (readIdx + 1) % mCapacity;
        }
        
        mReadIndex.store(readIdx);
        return true;
    }
    
    size_t availableToRead() const {
        size_t writeIdx = mWriteIndex.load();
        size_t readIdx = mReadIndex.load();
        return (writeIdx + mCapacity - readIdx) % mCapacity;
    }
    
    size_t availableToWrite() const {
        size_t writeIdx = mWriteIndex.load();
        size_t readIdx = mReadIndex.load();
        return (readIdx + mCapacity - writeIdx - 1) % mCapacity;
    }
    
    void clear() {
        mReadIndex.store(0);
        mWriteIndex.store(0);
    }
};

/**
 * Advanced audio effects processor
 */
class AudioEffectsProcessor {
public:
    struct ReverbParams {
        float roomSize{0.5f};
        float damping{0.5f};
        float wetLevel{0.3f};
        float dryLevel{0.7f};
        float width{1.0f};
    };
    
    struct DelayParams {
        float delayTimeMs{250.0f};
        float feedback{0.3f};
        float wetLevel{0.3f};
        float dryLevel{0.7f};
    };
    
    struct CompressorParams {
        float threshold{-12.0f};
        float ratio{4.0f};
        float attackMs{5.0f};
        float releaseMs{100.0f};
        float makeupGain{0.0f};
    };
    
    struct EQParams {
        std::array<float, 10> gains{0.0f}; // 10-band EQ
        std::array<float, 10> frequencies{31.25f, 62.5f, 125.0f, 250.0f, 500.0f, 
                                         1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f};
        std::array<float, 10> qFactors{0.707f}; // Default Q for each band
    };
    
private:
    ReverbParams mReverbParams;
    DelayParams mDelayParams;
    CompressorParams mCompressorParams;
    EQParams mEQParams;
    
    // Effect state variables
    std::vector<std::vector<float>> mDelayBuffers;
    std::vector<size_t> mDelayIndices;
    std::vector<float> mCompressorEnvelopes;
    std::vector<std::array<float, 10>> mEQStates; // Biquad filter states
    
    int32_t mSampleRate{48000};
    int32_t mChannels{2};
    
public:
    AudioEffectsProcessor(int32_t sampleRate, int32_t channels);
    ~AudioEffectsProcessor() = default;
    
    void processEffects(AudioProcessingContext& context);
    void setReverbParams(const ReverbParams& params) { mReverbParams = params; }
    void setDelayParams(const DelayParams& params) { mDelayParams = params; }
    void setCompressorParams(const CompressorParams& params) { mCompressorParams = params; }
    void setEQParams(const EQParams& params) { mEQParams = params; }
    
    const ReverbParams& getReverbParams() const { return mReverbParams; }
    const DelayParams& getDelayParams() const { return mDelayParams; }
    const CompressorParams& getCompressorParams() const { return mCompressorParams; }
    const EQParams& getEQParams() const { return mEQParams; }
    
private:
    void processReverb(float* buffer, int32_t numFrames, int32_t channels);
    void processDelay(float* buffer, int32_t numFrames, int32_t channels);
    void processCompressor(float* buffer, int32_t numFrames, int32_t channels);
    void processEQ(float* buffer, int32_t numFrames, int32_t channels);
    void processLimiter(float* buffer, int32_t numFrames, int32_t channels);
    void processNoiseGate(float* buffer, int32_t numFrames, int32_t channels);
    
    float biquadFilter(float input, int channel, int band, float freq, float gain, float q);
    void updateDelayBufferSize(int32_t sampleRate);
};

class AudioEngine : public oboe::AudioStreamCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    AudioEngine();
    ~AudioEngine() override;

    // Core engine control
    bool start();
    void stop();
    bool pause();
    bool resume();
    bool isRunning() const { return mIsRunning.load(); }
    bool isPaused() const { return mIsPaused.load(); }

    // Audio routing configuration
    void setRoutingMode(AudioRoutingMode mode);
    AudioRoutingMode getRoutingMode() const { return mRoutingMode; }
    
    // Quality profile management
    void setQualityProfile(AudioQualityProfile profile);
    AudioQualityProfile getQualityProfile() const;
    
    // Audio configuration
    bool setAudioConfiguration(const AudioBufferConfig& config);
    bool setAudioConfiguration(int32_t sampleRate, int32_t framesPerBurst);
    AudioBufferConfig getAudioConfiguration() const;
    void setPerformanceMode(oboe::PerformanceMode mode);
    void setSharingMode(oboe::SharingMode mode);
    void setUsage(oboe::Usage usage);
    void setContentType(oboe::ContentType contentType);
    void setInputPreset(oboe::InputPreset preset);
    
    // Advanced buffer management
    bool setBufferSizeInFrames(int32_t requestedSize);
    bool setBufferCapacityInFrames(int32_t requestedCapacity);
    int32_t getCurrentBufferSize() const;
    int32_t getBufferCapacity() const;
    int32_t getOptimalBufferSize() const;
    int32_t getMinimumBufferSize() const;
    int32_t getMaximumBufferSize() const;
    
    // Latency management and monitoring
    double getCurrentLatencyMs() const;
    double getMinimumLatencyMs() const;
    double getAverageLatencyMs() const;
    double getMaximumLatencyMs() const;
    bool isLowLatencySupported() const;
    
    // Performance monitoring and analytics
    const AudioPerformanceMetrics& getPerformanceMetrics() const { return mMetrics; }
    void resetPerformanceMetrics();
    void enablePerformanceMonitoring(bool enable);
    bool isPerformanceMonitoringEnabled() const { return mPerformanceMonitoringEnabled.load(); }
    
    // Device management and discovery
    bool setAudioDevice(int32_t deviceId);
    int32_t getCurrentInputDevice() const;
    int32_t getCurrentOutputDevice() const;
    std::vector<ExtendedAudioDeviceInfo> getAvailableInputDevices() const;
    std::vector<ExtendedAudioDeviceInfo> getAvailableOutputDevices() const;
    bool isDeviceConnected(int32_t deviceId) const;
    void refreshDeviceList();
    
    // Volume and gain control
    void setMasterGain(float gain);
    float getMasterGain() const { return mMasterGain.load(); }
    void setInputGain(float gain);
    float getInputGain() const { return mInputGain.load(); }
    void setChannelGain(int channel, float gain);
    float getChannelGain(int channel) const;
    void setMute(bool muted);
    bool isMuted() const { return mIsMuted.load(); }
    
    // Audio effects management
    void enableEffect(AudioEffect effect, bool enable);
    bool isEffectEnabled(AudioEffect effect) const;
    void setEffectParameter(AudioEffect effect, const std::string& param, float value);
    float getEffectParameter(AudioEffect effect, const std::string& param) const;
    AudioEffectsProcessor& getEffectsProcessor() { return *mEffectsProcessor; }
    
    // Real-time parameter control
    ParameterQueue& getParameterQueue() { return mParameterQueue; }
    void setParameter(const std::string& name, float value);
    float getParameter(const std::string& name) const;
    
    // Audio format and sample rate conversion
    bool setSampleRateConversion(bool enable);
    bool isSampleRateConversionEnabled() const { return mSampleRateConversionEnabled.load(); }
    void setTargetSampleRate(int32_t sampleRate);
    int32_t getTargetSampleRate() const { return mTargetSampleRate.load(); }
    
    // Thread priority and CPU affinity
    void setAudioThreadPriority(int priority);
    int getAudioThreadPriority() const { return mAudioThreadPriority.load(); }
    void setAudioThreadCpuAffinity(const std::vector<int>& cpuIds);
    std::vector<int> getAudioThreadCpuAffinity() const;
    
    // Callback registration for events
    using DeviceChangeCallback = std::function<void(int32_t deviceId, bool connected)>;
    using ErrorCallback = std::function<void(oboe::Result error, const std::string& message)>;
    using PerformanceCallback = std::function<void(const AudioPerformanceMetrics& metrics)>;
    
    void setDeviceChangeCallback(DeviceChangeCallback callback);
    void setErrorCallback(ErrorCallback callback);
    void setPerformanceCallback(PerformanceCallback callback);
    
    // AudioStreamCallback implementation
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream,
                                         void *audioData,
                                         int32_t numFrames) override;

    // AudioStreamErrorCallback implementation
    void onErrorBeforeClose(oboe::AudioStream *audioStream,
                           oboe::Result error) override;
    void onErrorAfterClose(oboe::AudioStream *audioStream,
                          oboe::Result error) override;

    // Advanced diagnostics and debugging
    void enableAudioLogging(bool enable);
    bool isAudioLoggingEnabled() const { return mAudioLoggingEnabled.load(); }
    void dumpAudioConfiguration() const;
    void dumpPerformanceMetrics() const;
    std::string getEngineStatusReport() const;
    
    // Memory management
    void optimizeMemoryUsage();
    size_t getMemoryUsage() const;
    void setMemoryPoolSize(size_t sizeBytes);
    
    // Thermal management
    void enableThermalThrottling(bool enable);
    bool isThermalThrottlingEnabled() const { return mThermalThrottlingEnabled.load(); }
    float getCurrentThermalState() const;

private:
    // Stream management
    bool openInputStream();
    bool openOutputStream();
    bool openDuplexStream();
    void closeStreams();
    bool configureStream(oboe::AudioStreamBuilder& builder, bool isInput);
    bool validateStreamConfiguration(const oboe::AudioStreamBuilder& builder) const;
    
    // Audio processing pipeline
    void processAudioMono(AudioProcessingContext& context);
    void processAudioStereo(AudioProcessingContext& context);
    void processAudioMidSide(AudioProcessingContext& context);
    void processAudioSurround5_1(AudioProcessingContext& context);
    void processAudioSurround7_1(AudioProcessingContext& context);
    void processAudioBinaural(AudioProcessingContext& context);
    void processAudioAmbisonics(AudioProcessingContext& context);
    
    void applyGainAndLimiting(float* buffer, int32_t numFrames, int32_t channels);
    void applySampleRateConversion(float* inputBuffer, float* outputBuffer, 
                                  int32_t inputFrames, int32_t outputFrames, int32_t channels);
    
    // Mid-side processing utilities
    void encodeToMidSide(float* leftRight, float* midSide, int32_t numFrames);
    void decodeFromMidSide(float* midSide, float* leftRight, int32_t numFrames);
    
    // Surround sound processing
    void mixToSurround5_1(float* stereoInput, float* surroundOutput, int32_t numFrames);
    void mixToSurround7_1(float* stereoInput, float* surroundOutput, int32_t numFrames);
    void downmixFromSurround(float* surroundInput, float* stereoOutput, 
                            int32_t numFrames, int32_t inputChannels);
    
    // Binaural processing
    void applyHRTF(float* inputBuffer, float* outputBuffer, int32_t numFrames, 
                   float azimuth, float elevation);
    void processBinauralSpatializer(float* buffer, int32_t numFrames);
    
    // Ambisonics processing
    void encodeAmbisonics(float* monoInput, float* ambisonicOutput, 
                         int32_t numFrames, float azimuth, float elevation);
    void decodeAmbisonics(float* ambisonicInput, float* stereoOutput, int32_t numFrames);
    
    // Buffer management
    void resizeInternalBuffers(int32_t numFrames, int32_t channels);
    void clearInternalBuffers();
    bool allocateProcessingBuffers(int32_t maxFrames, int32_t maxChannels);
    void deallocateProcessingBuffers();
    
    // Performance monitoring and optimization
    void updatePerformanceMetrics(std::chrono::steady_clock::time_point callbackStart,
                                 std::chrono::steady_clock::time_point callbackEnd,
                                 int32_t numFrames);
    void detectUnderruns(oboe::AudioStream* stream);
    void detectOverruns(oboe::AudioStream* stream);
    void updateCpuLoad(double processingTimeMs, double availableTimeMs);
    void updateMemoryUsage();
    void updateThermalState();
    void optimizeForThermalState(float thermalState);
    
    // Error handling and recovery
    void logThreadPriority();
    bool attemptStreamRestart(int maxRetries = 3);
    void logDeviceInfo(oboe::AudioStream *audioStream);
    void handleStreamDisconnection();
    void handleBufferSizeChange();
    void handleSampleRateChange();
    void handleDeviceChange(int32_t newDeviceId);
    void recoverFromError(oboe::Result error);
    
    // Device management
    void updateDeviceList();
    ExtendedAudioDeviceInfo getDeviceInfo(int32_t deviceId) const;
    bool isDeviceOptimalForConfiguration(int32_t deviceId, const AudioBufferConfig& config) const;
    int32_t findOptimalDevice(const AudioBufferConfig& config) const;
    
    // Quality profile optimization
    AudioBufferConfig getConfigForQualityProfile(AudioQualityProfile profile) const;
    void applyQualityProfileOptimizations(AudioQualityProfile profile);
    
    // Utility functions
    int32_t getChannelCountForRoutingMode() const;
    AudioBufferConfig getOptimalBufferConfig() const;
    bool validateAudioConfiguration() const;
    void logAudioConfiguration() const;
    float calculateLatencyMs(oboe::AudioStream* stream) const;
    bool isConfigurationSupported(const AudioBufferConfig& config) const;
    
    // Threading and synchronization
    std::mutex mStreamMutex;
    std::mutex mConfigMutex;
    std::mutex mDeviceMutex;
    std::mutex mMetricsMutex;
    std::mutex mEffectsMutex;
    std::condition_variable mRestartCondition;
    std::thread mMonitoringThread;
    std::thread mPerformanceThread;
    
    // Stream state
    std::atomic<bool> mIsRunning{false};
    std::atomic<bool> mIsPaused{false};
    std::atomic<bool> mRestartRequested{false};
    std::atomic<bool> mThreadPriorityLogged{false};
    std::atomic<int> mRestartAttempts{0};
    std::atomic<bool> mStreamDisconnected{false};
    std::atomic<bool> mConfigurationChanged{false};
    
    // Audio streams
    std::shared_ptr<oboe::AudioStream> mInputStream;
    std::shared_ptr<oboe::AudioStream> mOutputStream;
    std::shared_ptr<oboe::AudioStream> mDuplexStream;
    
    // DSP plugin integration
    lsp_android_plugin_handle mPlugin{nullptr};
    std::mutex mPluginMutex;
    std::atomic<bool> mPluginEnabled{false};
    
    // Audio configuration
    AudioRoutingMode mRoutingMode{AudioRoutingMode::Stereo};
    AudioBufferConfig mBufferConfig;
    AudioQualityProfile mQualityProfile{AudioQualityProfile::Balanced};
    std::atomic<int32_t> mCurrentInputDeviceId{oboe::kUnspecified};
    std::atomic<int32_t> mCurrentOutputDeviceId{oboe::kUnspecified};
    
    // Gain and volume control
    std::atomic<float> mMasterGain{1.0f};
    std::atomic<float> mInputGain{1.0f};
    std::atomic<bool> mIsMuted{false};
    std::vector<std::atomic<float>> mChannelGains;
    
    // Effects processing
    std::unique_ptr<AudioEffectsProcessor> mEffectsProcessor;
    std::atomic<uint32_t> mEnabledEffects{0};
    std::unordered_map<std::string, float> mEffectParameters;
    
    // Internal audio buffers
    std::vector<float> mInputBuffer;
    std::vector<float> mOutputBuffer;
    std::vector<float> mProcessingBuffer;
    std::vector<float> mMidSideBuffer;
    std::vector<float> mSurroundBuffer;
    std::vector<float> mAmbisonicsBuffer;
    std::vector<float> mConversionBuffer;
    
    // Circular buffers for real-time processing
    std::unique_ptr<CircularBuffer<float>> mInputRingBuffer;
    std::unique_ptr<CircularBuffer<float>> mOutputRingBuffer;
    
    // Performance monitoring
    AudioPerformanceMetrics mMetrics;
    std::chrono::steady_clock::time_point mLastCallbackTime;
    std::atomic<double> mCurrentLatencyMs{0.0};
    std::atomic<bool> mPerformanceMonitoringEnabled{true};
    std::atomic<bool> mAudioLoggingEnabled{false};
    
    // Sample rate conversion
    std::atomic<bool> mSampleRateConversionEnabled{false};
    std::atomic<int32_t> mTargetSampleRate{48000};
    std::vector<float> mSrcState; // Sample rate converter state
    
    // Thread management
    std::atomic<int> mAudioThreadPriority{-19}; // High priority
    std::vector<int> mCpuAffinity;
    std::atomic<bool> mThermalThrottlingEnabled{true};
    
    // Device management
    std::vector<ExtendedAudioDeviceInfo> mAvailableInputDevices;
    std::vector<ExtendedAudioDeviceInfo> mAvailableOutputDevices;
    std::chrono::steady_clock::time_point mLastDeviceListUpdate;
    
    // Parameter management
    ParameterQueue mParameterQueue;
    std::unordered_map<std::string, float> mParameters;
    
    // Callback functions
    DeviceChangeCallback mDeviceChangeCallback;
    ErrorCallback mErrorCallback;
    PerformanceCallback mPerformanceCallback;
    
    // Memory management
    size_t mMemoryPoolSize{1024 * 1024}; // 1MB default
    std::atomic<size_t> mCurrentMemoryUsage{0};
    
    // Constants
    static constexpr int32_t kDefaultSampleRate = 48000;
    static constexpr int32_t kDefaultFramesPerBurst = 192;
    static constexpr int32_t kMaxSampleRate = 192000;
    static constexpr int32_t kMinSampleRate = 8000;
    static constexpr float kMaxGain = 4.0f;
    static constexpr float kMinGain = 0.0f;
    static constexpr int kMaxRestartAttempts = 5;
    static constexpr std::chrono::milliseconds kRestartDelay{100};
    static constexpr std::chrono::seconds kMetricsUpdateInterval{1};
    static constexpr std::chrono::seconds kDeviceListUpdateInterval{5};
    static constexpr int32_t kMaxChannels = 32;
    static constexpr int32_t kMaxBufferSizeFrames = 8192;
    static constexpr int32_t kMinBufferSizeFrames = 16;
    static constexpr double kMaxLatencyMs = 1000.0;
    static constexpr double kMinLatencyMs = 0.1;
    static constexpr float kThermalThrottleThreshold = 0.8f;
    static constexpr size_t kMaxMemoryUsage = 100 * 1024 * 1024; // 100MB
};
