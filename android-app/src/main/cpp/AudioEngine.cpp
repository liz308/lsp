#include "AudioEngine.h"

#include <android/log.h>
#include <cmath>
#include <sched.h>
#include <unistd.h>
#include <sys/resource.h>
#include <thread>
#include <chrono>
#include <algorithm>
#include <fstream>
#include <sstream>
#include <memory>
#include <random>

namespace {

constexpr const char *LOG_TAG = "LspAudioEngine";
constexpr int32_t MAX_BUFFER_SIZE_FRAMES = 256;
constexpr int32_t PREFERRED_SAMPLE_RATE = 48000;
constexpr int32_t FALLBACK_SAMPLE_RATE = 44100;
constexpr int32_t MAX_RESTART_ATTEMPTS = 5;
constexpr int32_t UNDERRUN_THRESHOLD = 10;
constexpr float SINE_WAVE_AMPLITUDE = 0.3f;
constexpr float TEST_FREQUENCY_HZ = 440.0f;
constexpr int32_t EQ_BAND_COUNT = 10;
constexpr int32_t PARAMS_PER_BAND = 3;
constexpr int32_t MASTER_GAIN_PARAM_INDEX = 30;
constexpr int32_t BYPASS_PARAM_INDEX = 31;
constexpr float MIN_FREQUENCY = 20.0f;
constexpr float MAX_FREQUENCY = 20000.0f;
constexpr float MIN_GAIN_DB = -60.0f;
constexpr float MAX_GAIN_DB = 20.0f;
constexpr float MIN_Q_FACTOR = 0.1f;
constexpr float MAX_Q_FACTOR = 30.0f;
constexpr int32_t PERFORMANCE_MONITOR_INTERVAL = 1000; // frames
constexpr float CLIP_THRESHOLD = 0.95f;
constexpr float NOISE_FLOOR_DB = -96.0f;

void loge(const char *msg) {
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s", msg);
}

void logw(const char *msg) {
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "%s", msg);
}

void logi(const char *msg) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%s", msg);
}

float dbToLinear(float db) {
    return std::pow(10.0f, db / 20.0f);
}

float linearToDb(float linear) {
    return 20.0f * std::log10(std::max(linear, 1e-10f));
}

} // namespace

AudioEngine::AudioEngine() 
    : mPlugin(nullptr)
    , mStream(nullptr)
    , mIsRunning(false)
    , mRoutingMode(AudioRoutingMode::Stereo)
    , mThreadPriorityLogged(false)
    , mRestartRequested(false)
    , mRestartAttempts(0)
    , mUnderrunCount(0)
    , mLastUnderrunCount(0)
    , mSinePhase(0.0f)
    , mCurrentSampleRate(PREFERRED_SAMPLE_RATE)
    , mCurrentChannelCount(2)
    , mParameterQueue(1024)
    , mMasterGainLinear(1.0f)
    , mIsBypassed(false)
    , mPerformanceFrameCounter(0)
    , mPeakLevelLeft(0.0f)
    , mPeakLevelRight(0.0f)
    , mRmsLevelLeft(0.0f)
    , mRmsLevelRight(0.0f)
    , mClipCount(0)
    , mProcessingTimeUs(0)
    , mCpuLoadPercent(0.0f)
    , mDitherGenerator(std::random_device{}())
    , mDitherDistribution(-1.0f / (1 << 23), 1.0f / (1 << 23)) {
    
    // Initialize EQ band parameters with musical defaults
    initializeEQBands();
    
    // Initialize RMS calculation buffers
    mRmsBufferLeft.resize(mCurrentSampleRate / 10, 0.0f); // 100ms buffer
    mRmsBufferRight.resize(mCurrentSampleRate / 10, 0.0f);
    mRmsBufferIndex = 0;
    
    mPlugin = lsp_android_create_parametric_eq();
    if (!mPlugin) {
        loge("Failed to create LSP parametric EQ plugin");
    } else {
        logi("LSP parametric EQ plugin created successfully");
        initializePluginDefaults();
        
        // Validate plugin parameter count
        int32_t paramCount = get_param_count(mPlugin);
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG,
                           "Plugin has %d parameters", paramCount);
        
        if (paramCount < (EQ_BAND_COUNT * PARAMS_PER_BAND + 2)) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                               "Plugin parameter count (%d) less than expected (%d)",
                               paramCount, EQ_BAND_COUNT * PARAMS_PER_BAND + 2);
    }
    }
}
    
AudioEngine::~AudioEngine() {
    stop();
    if (mPlugin) {
        lsp_android_destroy_plugin(mPlugin);
        mPlugin = nullptr;
    }
}

void AudioEngine::initializeEQBands() {
    mEQBands.clear();
    mEQBands.reserve(EQ_BAND_COUNT);
    
    // Initialize with standard 10-band EQ frequencies
    const std::vector<float> standardFrequencies = {
        31.25f, 62.5f, 125.0f, 250.0f, 500.0f,
        1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
    };
    
    for (int i = 0; i < EQ_BAND_COUNT; ++i) {
        EQBand band;
        band.frequency = (i < standardFrequencies.size()) ? 
                        standardFrequencies[i] : 
                        MIN_FREQUENCY * std::pow(2.0f, i * 1.5f);
        band.gainDb = 0.0f;
        band.qFactor = 1.0f;
        band.isEnabled = true;
        band.filterType = EQFilterType::Bell;
        
        mEQBands.push_back(band);
}
}

void AudioEngine::initializePluginDefaults() {
    if (!mPlugin) return;
    
    // Set all EQ bands to flat response
    for (int band = 0; band < EQ_BAND_COUNT; ++band) {
        const EQBand& eqBand = mEQBands[band];
        
        // Set frequency parameter
        lsp_android_error_code result = set_param(mPlugin, band * PARAMS_PER_BAND + 2, eqBand.frequency);
        if (result != LSP_ANDROID_SUCCESS) {
                __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                               "Failed to set frequency for band %d: %d", band, result);
            }
        
        // Set gain parameter (0 dB = unity gain)
        result = set_param(mPlugin, band * PARAMS_PER_BAND + 0, eqBand.gainDb);
        if (result != LSP_ANDROID_SUCCESS) {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                               "Failed to set gain for band %d: %d", band, result);
        }
        
        // Set Q factor parameter
        result = set_param(mPlugin, band * PARAMS_PER_BAND + 1, eqBand.qFactor);
        if (result != LSP_ANDROID_SUCCESS) {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                               "Failed to set Q for band %d: %d", band, result);
}
    }

    // Set master gain to unity (0 dB)
    set_param(mPlugin, MASTER_GAIN_PARAM_INDEX, 0.0f);
    
    // Disable bypass (enable processing)
    set_param(mPlugin, BYPASS_PARAM_INDEX, 0.0f);
    
    logi("Plugin initialized with default flat EQ curve");
    }
    
void AudioEngine::setRoutingMode(AudioRoutingMode mode) {
    if (mRoutingMode == mode) {
        return;
    }
    
    bool wasRunning = mIsRunning.load();
    
    if (wasRunning) {
        stop();
    }
    
    mRoutingMode = mode;
    mCurrentChannelCount = getChannelCountForRoutingMode();
    
    // Resize RMS buffers for new channel configuration
    resizeRmsBuffers();
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG,
                       "Routing mode changed to: %s (%d channels)",
                       mode == AudioRoutingMode::Mono ? "Mono" :
                       mode == AudioRoutingMode::Stereo ? "Stereo" : "MidSide",
                       mCurrentChannelCount);
    
    if (wasRunning) {
        start();
    }
}

AudioRoutingMode AudioEngine::getRoutingMode() const {
    return mRoutingMode;
}

int32_t AudioEngine::getChannelCountForRoutingMode() const {
    switch (mRoutingMode) {
        case AudioRoutingMode::Mono:
            return 1;
        case AudioRoutingMode::Stereo:
            return 2;
        case AudioRoutingMode::MidSide:
            return 2;
        default:
            return 2;
    }
}

void AudioEngine::resizeRmsBuffers() {
    int32_t bufferSize = mCurrentSampleRate / 10; // 100ms
    mRmsBufferLeft.resize(bufferSize, 0.0f);
    mRmsBufferRight.resize(bufferSize, 0.0f);
    mRmsBufferIndex = 0;
}

void AudioEngine::setEQParameter(int bandIndex, float frequency, float gain, float q) {
    if (!mPlugin || bandIndex < 0 || bandIndex >= EQ_BAND_COUNT) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                           "Invalid EQ parameter: band=%d", bandIndex);
        return;
    }
    
    // Validate and clamp parameters
    frequency = std::clamp(frequency, MIN_FREQUENCY, MAX_FREQUENCY);
    gain = std::clamp(gain, MIN_GAIN_DB, MAX_GAIN_DB);
    q = std::clamp(q, MIN_Q_FACTOR, MAX_Q_FACTOR);
    
    // Update internal state
    mEQBands[bandIndex].frequency = frequency;
    mEQBands[bandIndex].gainDb = gain;
    mEQBands[bandIndex].qFactor = q;
    
    // Queue parameter updates for thread-safe processing in audio callback
    ParameterUpdate freqUpdate = {bandIndex * PARAMS_PER_BAND + 2, frequency};
    ParameterUpdate gainUpdate = {bandIndex * PARAMS_PER_BAND + 0, gain};
    ParameterUpdate qUpdate = {bandIndex * PARAMS_PER_BAND + 1, q};
    
    if (!mParameterQueue.enqueue(freqUpdate) ||
        !mParameterQueue.enqueue(gainUpdate) ||
        !mParameterQueue.enqueue(qUpdate)) {
        logw("Parameter queue full, dropping EQ updates");
    }
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,
                       "EQ Band %d: freq=%.1fHz, gain=%.1fdB, Q=%.2f",
                       bandIndex, frequency, gain, q);
}

void AudioEngine::setEQBandEnabled(int bandIndex, bool enabled) {
    if (bandIndex < 0 || bandIndex >= EQ_BAND_COUNT) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                           "Invalid EQ band index: %d", bandIndex);
        return;
    }
    
    mEQBands[bandIndex].isEnabled = enabled;
    
    // When disabling a band, set its gain to 0 dB
    float effectiveGain = enabled ? mEQBands[bandIndex].gainDb : 0.0f;
    
    ParameterUpdate gainUpdate = {bandIndex * PARAMS_PER_BAND + 0, effectiveGain};
    if (!mParameterQueue.enqueue(gainUpdate)) {
        logw("Parameter queue full, dropping EQ band enable/disable update");
    }
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,
                       "EQ Band %d %s", bandIndex, enabled ? "enabled" : "disabled");
}

void AudioEngine::setEQFilterType(int bandIndex, EQFilterType filterType) {
    if (bandIndex < 0 || bandIndex >= EQ_BAND_COUNT) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                           "Invalid EQ band index: %d", bandIndex);
        return;
    }
    
    mEQBands[bandIndex].filterType = filterType;
    
    // Note
