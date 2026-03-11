#include <cmath>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <vector>

#include "../lsp_android_bridge.h"

namespace {

// Simple 16-bit PCM WAV writer for test purposes.
void write_wav_mono_16bit(
        const char*        path,
        const float*       samples,
        std::size_t        num_samples,
        int                sample_rate) {
    const uint32_t byte_rate   = static_cast<uint32_t>(sample_rate * 2); // mono, 16-bit
    const uint16_t block_align = 2;
    const uint16_t bits_per_sample = 16;
    const uint32_t data_size  = static_cast<uint32_t>(num_samples * 2);
    const uint32_t riff_size  = 36 + data_size;

    std::ofstream out(path, std::ios::binary);
    if (!out) {
        std::cerr << "Failed to open WAV file for writing: " << path << "\n";
        return;
    }

    // RIFF header
    out.write("RIFF", 4);
    out.write(reinterpret_cast<const char*>(&riff_size), 4);
    out.write("WAVE", 4);

    // fmt chunk
    out.write("fmt ", 4);
    uint32_t fmt_size = 16;
    uint16_t audio_format = 1; // PCM
    uint16_t num_channels = 1;
    out.write(reinterpret_cast<const char*>(&fmt_size), 4);
    out.write(reinterpret_cast<const char*>(&audio_format), 2);
    out.write(reinterpret_cast<const char*>(&num_channels), 2);
    out.write(reinterpret_cast<const char*>(&sample_rate), 4);
    out.write(reinterpret_cast<const char*>(&byte_rate), 4);
    out.write(reinterpret_cast<const char*>(&block_align), 2);
    out.write(reinterpret_cast<const char*>(&bits_per_sample), 2);

    // data chunk
    out.write("data", 4);
    out.write(reinterpret_cast<const char*>(&data_size), 4);

    for (std::size_t i = 0; i < num_samples; ++i) {
        float s = samples[i];
        if (s > 1.0f) s = 1.0f;
        if (s < -1.0f) s = -1.0f;
        int16_t v = static_cast<int16_t>(std::lrintf(s * 32767.0f));
        out.write(reinterpret_cast<const char*>(&v), 2);
    }

    out.close();
}

} // namespace

int main() {
    const int sample_rate = 48000;
    const float duration_seconds = 2.0f;
    const std::size_t num_frames = static_cast<std::size_t>(sample_rate * duration_seconds);

    std::vector<float> input(num_frames);
    std::vector<float> output(num_frames);

    // Generate a test sine wave at 1 kHz.
    const float freq = 1000.0f;
    const float two_pi_f_over_fs = 2.0f * static_cast<float>(M_PI) * freq / static_cast<float>(sample_rate);
    for (std::size_t n = 0; n < num_frames; ++n) {
        input[n] = std::sinf(two_pi_f_over_fs * static_cast<float>(n));
    }

    // Instantiate the parametric EQ plugin via the bridge.
    lsp_android_plugin_handle plugin = lsp_android_create_parametric_eq();
    if (!plugin) {
        std::cerr << "Failed to create parametric EQ plugin instance.\n";
        return 1;
    }

    // For the stub implementation we simply leave parameters at their defaults.
    // In the real implementation this is where we would set a known parameter
    // set (gain, frequency, Q, etc.) to match the Linux reference harness.

    lsp_android_process(plugin, input.data(), output.data(), static_cast<int32_t>(num_frames));

    lsp_android_destroy_plugin(plugin);

    write_wav_mono_16bit("eq_test_output.wav", output.data(), output.size(), sample_rate);
    std::cout << "Wrote eq_test_output.wav with " << output.size() << " samples.\n";

    return 0;
}

