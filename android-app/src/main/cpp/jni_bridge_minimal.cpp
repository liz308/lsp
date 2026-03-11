#include <jni.h>
#include <android/log.h>

#define LOG_TAG "AudioEngineJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state variables
static bool gEngineInitialized = false;
static bool gEngineRunning = false;
static float gMasterVolume = 1.0f;
static bool gBypassEnabled = false;
static float gEqualizerBands[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

// JNI Functions
extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeInitializeEngine(JNIEnv *env, jobject thiz,
                                                                jint sampleRate, jint bufferSize) {
    LOGI("nativeInitializeEngine called: sampleRate=%d, bufferSize=%d", sampleRate, bufferSize);
    gEngineInitialized = true;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeStartEqTest(JNIEnv *env, jobject thiz) {
    LOGI("nativeStartEqTest called");
    if (!gEngineInitialized) {
        LOGE("Engine not initialized");
        return JNI_FALSE;
    }
    gEngineRunning = true;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeStopEqTest(JNIEnv *env, jobject thiz) {
    LOGI("nativeStopEqTest called");
    gEngineRunning = false;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeSetEqualizerBand(JNIEnv *env, jobject thiz,
                                                                jint bandIndex, jfloat gainDb) {
    if (bandIndex < 0 || bandIndex >= 10) {
        LOGE("Invalid band index: %d", bandIndex);
        return JNI_FALSE;
    }
    LOGI("nativeSetEqualizerBand: band=%d, gain=%.2f dB", bandIndex, gainDb);
    gEqualizerBands[bandIndex] = gainDb;
    return JNI_TRUE;
}

JNIEXPORT jfloat JNICALL
Java_com_example_lspandroid_MainActivity_nativeGetEqualizerBand(JNIEnv *env, jobject thiz,
                                                                jint bandIndex) {
    if (bandIndex < 0 || bandIndex >= 10) {
        LOGE("Invalid band index: %d", bandIndex);
        return 0.0f;
    }
    return gEqualizerBands[bandIndex];
}

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeSetMasterVolume(JNIEnv *env, jobject thiz,
                                                               jfloat volume) {
    LOGI("nativeSetMasterVolume: volume=%.2f", volume);
    gMasterVolume = volume;
    return JNI_TRUE;
}

JNIEXPORT jfloat JNICALL
Java_com_example_lspandroid_MainActivity_nativeGetMasterVolume(JNIEnv *env, jobject thiz) {
    return gMasterVolume;
}

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeSetBypass(JNIEnv *env, jobject thiz,
                                                         jboolean bypass) {
    LOGI("nativeSetBypass: bypass=%d", bypass);
    gBypassEnabled = bypass;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeIsEngineRunning(JNIEnv *env, jobject thiz) {
    return gEngineRunning ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_lspandroid_MainActivity_nativeCleanup(JNIEnv *env, jobject thiz) {
    LOGI("nativeCleanup called");
    gEngineRunning = false;
    gEngineInitialized = false;
}

} // extern "C"