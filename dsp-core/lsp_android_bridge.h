#ifndef LSP_ANDROID_BRIDGE_H
#define LSP_ANDROID_BRIDGE_H

// C-linkage boundary between the Android host (JNI / Oboe) and the
// upstream LSP DSP engines.
//
// RULE 1: Never modify the DSP core. All porting work happens here and in
// associated glue code. The DSP algorithms are compiled directly from
// upstream lsp-plugins sources.

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// Version tag for the bridge ABI. Increment when the C API changes.
#define LSP_ANDROID_BRIDGE_API_VERSION 1

// Maximum number of audio channels supported
#define LSP_ANDROID_MAX_CHANNELS 8

// Maximum number of ports per plugin
#define LSP_ANDROID_MAX_PORTS 256

// Maximum buffer size for audio processing
#define LSP_ANDROID_MAX_BUFFER_SIZE 8192

// Minimum buffer size for audio processing
#define LSP_ANDROID_MIN_BUFFER_SIZE 16

// Maximum plugin name length
#define LSP_ANDROID_MAX_NAME_LENGTH 128

// Maximum unit string length
#define LSP_ANDROID_MAX_UNIT_LENGTH 32

// Maximum section string length
#define LSP_ANDROID_MAX_SECTION_LENGTH 64

// Maximum error message length
#define LSP_ANDROID_MAX_ERROR_MESSAGE_LENGTH 512

// Opaque handle to a plugin instance.
typedef void* lsp_android_plugin_handle;

// Plugin type identifiers for different DSP algorithms
typedef enum {
    LSP_ANDROID_PLUGIN_PARAMETRIC_EQ = 0,
    LSP_ANDROID_PLUGIN_COMPRESSOR = 1,
    LSP_ANDROID_PLUGIN_REVERB = 2,
    LSP_ANDROID_PLUGIN_DELAY = 3,
    LSP_ANDROID_PLUGIN_LIMITER = 4,
    LSP_ANDROID_PLUGIN_GATE = 5,
    LSP_ANDROID_PLUGIN_CHORUS = 6,
    LSP_ANDROID_PLUGIN_FLANGER = 7,
    LSP_ANDROID_PLUGIN_PHASER = 8,
    LSP_ANDROID_PLUGIN_DISTORTION = 9,
    LSP_ANDROID_PLUGIN_FILTER = 10,
    LSP_ANDROID_PLUGIN_OSCILLATOR = 11,
    LSP_ANDROID_PLUGIN_ENVELOPE = 12,
    LSP_ANDROID_PLUGIN_ANALYZER = 13,
    LSP_ANDROID_PLUGIN_MULTIBAND_COMPRESSOR = 14,
    LSP_ANDROID_PLUGIN_CROSSOVER = 15,
    LSP_ANDROID_PLUGIN_CONVOLUTION = 16,
    LSP_ANDROID_PLUGIN_SAMPLER = 17,
    LSP_ANDROID_PLUGIN_SYNTHESIZER = 18,
    LSP_ANDROID_PLUGIN_VOCODER = 19,
    LSP_ANDROID_PLUGIN_PITCH_SHIFTER = 20,
    LSP_ANDROID_PLUGIN_TIME_STRETCHER = 21,
    LSP_ANDROID_PLUGIN_GRANULAR = 22,
    LSP_ANDROID_PLUGIN_SPECTRAL_PROCESSOR = 23,
    LSP_ANDROID_PLUGIN_DYNAMICS_PROCESSOR = 24,
    LSP_ANDROID_PLUGIN_SPATIAL_PROCESSOR = 25,
    LSP_ANDROID_PLUGIN_MODULATION_MATRIX = 26,
    LSP_ANDROID_PLUGIN_MIDI_PROCESSOR = 27,
    LSP_ANDROID_PLUGIN_UTILITY = 28,
    LSP_ANDROID_PLUGIN_CUSTOM = 29,
    LSP_ANDROID_PLUGIN_COUNT = 30
} lsp_android_plugin_type;

// Configuration flags for plugin initialization
typedef enum {
    LSP_ANDROID_CONFIG_NONE = 0,
    LSP_ANDROID_CONFIG_STEREO = 1 << 0,
    LSP_ANDROID_CONFIG_HIGH_QUALITY = 1 << 1,
    LSP_ANDROID_CONFIG_LOW_LATENCY = 1 << 2,
    LSP_ANDROID_CONFIG_OVERSAMPLING = 1 << 3,
    LSP_ANDROID_CONFIG_SURROUND = 1 << 4,
    LSP_ANDROID_CONFIG_MULTICHANNEL = 1 << 5,
    LSP_ANDROID_CONFIG_REALTIME = 1 << 6,
    LSP_ANDROID_CONFIG_OFFLINE = 1 << 7,
    LSP_ANDROID_CONFIG_SIDECHAIN = 1 << 8,
    LSP_ANDROID_CONFIG_MIDI_INPUT = 1 << 9,
    LSP_ANDROID_CONFIG_MIDI_OUTPUT = 1 << 10,
    LSP_ANDROID_CONFIG_AUTOMATION = 1 << 11,
    LSP_ANDROID_CONFIG_PRESET_SUPPORT = 1 << 12,
    LSP_ANDROID_CONFIG_STATE_SAVE = 1 << 13,
    LSP_ANDROID_CONFIG_THREAD_SAFE = 1 << 14,
    LSP_ANDROID_CONFIG_SAMPLE_ACCURATE = 1 << 15,
    LSP_ANDROID_CONFIG_VARIABLE_BUFFER = 1 << 16,
    LSP_ANDROID_CONFIG_ZERO_LATENCY = 1 << 17,
    LSP_ANDROID_CONFIG_ADAPTIVE_QUALITY = 1 << 18,
    LSP_ANDROID_CONFIG_GPU_ACCELERATION = 1 << 19,
    LSP_ANDROID_CONFIG_VECTORIZED = 1 << 20,
    LSP_ANDROID_CONFIG_DENORMAL_PROTECTION = 1 << 21,
    LSP_ANDROID_CONFIG_SOFT_BYPASS = 1 << 22,
    LSP_ANDROID_CONFIG_HARD_BYPASS = 1 << 23,
    LSP_ANDROID_CONFIG_TAIL_PROCESSING = 1 << 24,
    LSP_ANDROID_CONFIG_LOOK_AHEAD = 1 << 25,
    LSP_ANDROID_CONFIG_EXTERNAL_SIDECHAIN = 1 << 26,
    LSP_ANDROID_CONFIG_INTERNAL_SIDECHAIN = 1 << 27,
    LSP_ANDROID_CONFIG_PARAMETER_SMOOTHING = 1 << 28,
    LSP_ANDROID_CONFIG_TEMPO_SYNC = 1 << 29,
    LSP_ANDROID_CONFIG_HOST_TRANSPORT = 1 << 30
} lsp_android_config_flags;

// Port type identifiers (mirroring the concepts in lsp-plugins: control,
// audio, CV, etc.). The exact mapping will be aligned with upstream metadata.
typedef enum {
    LSP_ANDROID_PORT_CONTROL = 0,
    LSP_ANDROID_PORT_AUDIO = 1,
    LSP_ANDROID_PORT_CV = 2,
    LSP_ANDROID_PORT_MIDI = 3,
    LSP_ANDROID_PORT_ATOM = 4,
    LSP_ANDROID_PORT_EVENT = 5,
    LSP_ANDROID_PORT_PATCH = 6,
    LSP_ANDROID_PORT_TIME = 7,
    LSP_ANDROID_PORT_OPTIONS = 8,
    LSP_ANDROID_PORT_WORKER = 9,
    LSP_ANDROID_PORT_LOG = 10,
    LSP_ANDROID_PORT_RESIZE = 11,
    LSP_ANDROID_PORT_UI = 12,
    LSP_ANDROID_PORT_INSTANCE_ACCESS = 13,
    LSP_ANDROID_PORT_DATA_ACCESS = 14,
    LSP_ANDROID_PORT_STATE = 15,
    LSP_ANDROID_PORT_PRESETS = 16,
    LSP_ANDROID_PORT_PROGRAMS = 17,
    LSP_ANDROID_PORT_BUFFER_SIZE = 18,
    LSP_ANDROID_PORT_SAMPLE_RATE = 19,
    LSP_ANDROID_PORT_LATENCY = 20,
    LSP_ANDROID_PORT_BYPASS = 21,
    LSP_ANDROID_PORT_ENABLED = 22,
    LSP_ANDROID_PORT_GAIN = 23,
    LSP_ANDROID_PORT_PAN = 24,
    LSP_ANDROID_PORT_VOLUME = 25,
    LSP_ANDROID_PORT_MUTE = 26,
    LSP_ANDROID_PORT_SOLO = 27,
    LSP_ANDROID_PORT_PHASE = 28,
    LSP_ANDROID_PORT_POLARITY = 29,
    LSP_ANDROID_PORT_METER = 30
} lsp_android_port_type;

// Port direction flags
typedef enum {
    LSP_ANDROID_PORT_INPUT = 1 << 0,
    LSP_ANDROID_PORT_OUTPUT = 1 << 1,
    LSP_ANDROID_PORT_BIDIRECTIONAL = LSP_ANDROID_PORT_INPUT | LSP_ANDROID_PORT_OUTPUT
} lsp_android_port_direction;

// Port property flags
typedef enum {
    LSP_ANDROID_PORT_PROPERTY_NONE = 0,
    LSP_ANDROID_PORT_PROPERTY_OPTIONAL = 1 << 0,
    LSP_ANDROID_PORT_PROPERTY_ENUMERATION = 1 << 1,
    LSP_ANDROID_PORT_PROPERTY_INTEGER = 1 << 2,
    LSP_ANDROID_PORT_PROPERTY_SAMPLE_RATE = 1 << 3,
    LSP_ANDROID_PORT_PROPERTY_TOGGLED = 1 << 4,
    LSP_ANDROID_PORT_PROPERTY_TRIGGER = 1 << 5,
    LSP_ANDROID_PORT_PROPERTY_NOT_AUTOMATED = 1 << 6,
    LSP_ANDROID_PORT_PROPERTY_NOT_ON_GUI = 1 << 7,
    LSP_ANDROID_PORT_PROPERTY_LOGARITHMIC = 1 << 8,
    LSP_ANDROID_PORT_PROPERTY_EXPENSIVE = 1 << 9,
    LSP_ANDROID_PORT_PROPERTY_STRICT_BOUNDS = 1 << 10,
    LSP_ANDROID_PORT_PROPERTY_CAUSES_ARTIFACTS = 1 << 11,
    LSP_ANDROID_PORT_PROPERTY_CONTINUOUS_CV = 1 << 12,
    LSP_ANDROID_PORT_PROPERTY_DISCRETE_CV = 1 << 13,
    LSP_ANDROID_PORT_PROPERTY_MORPH = 1 << 14,
    LSP_ANDROID_PORT_PROPERTY_NON_REALTIME = 1 << 15,
    LSP_ANDROID_PORT_PROPERTY_DISPLAY_PRIORITY = 1 << 16,
    LSP_ANDROID_PORT_PROPERTY_SUPPORTS_NULL = 1 << 17,
    LSP_ANDROID_PORT_PROPERTY_HIDDEN = 1 << 18,
    LSP_ANDROID_PORT_PROPERTY_ADVANCED = 1 << 19,
    LSP_ANDROID_PORT_PROPERTY_DEPRECATED = 1 << 20
} lsp_android_port_properties;

// Scale point for enumerated parameters
typedef struct {
    float       value;
    const char* label;
} lsp_android_scale_point;

// Port metadata descriptor exposed to the host/UI.
typedef struct {
    int32_t                       index;           // Port index in the plugin.
    lsp_android_port_type         type;            // Control, audio, CV...
    lsp_android_port_direction    direction;       // Input, output, or bidirectional
    lsp_android_port_properties   properties;      // Port property flags
    float                         min_value;       // Normalised or real units (per port).
    float                         max_value;
    float                         default_value;
    bool                          is_log_scale;    // true if logarithmic scaling is suggested.
    const char*                   name;            // Human-readable name ("Gain", "Freq").
    const char*                   symbol;          // Machine-readable symbol
    const char*                   unit;            // Optional unit string ("dB", "Hz", "ms").
    const char*                   section;         // Optional grouping/section tag.
    const char*                   comment;         // Optional description/help text
    int32_t                       scale_point_count; // Number of scale points
    const lsp_android_scale_point* scale_points;   // Array of scale points for enums
    float                         step_size;       // Suggested step size for UI
    int32_t                       display_priority; // Display priority (0 = highest)
    const char*                   group_symbol;    // Group symbol for related ports
    const char*                   designation;     // Port designation (e.g., "left", "right")
    bool                          supports_midi_cc; // Whether port supports MIDI CC
    int32_t                       midi_cc_number;  // Default MIDI CC number
    float                         range_steps;     // Number of steps in range
} lsp_android_port_descriptor;

// Plugin information structure
typedef struct {
    const char*                   name;            // Plugin name
    const char*                   uri;             // Unique plugin URI
    const char*                   author;          // Plugin author
    const char*                   email;           // Author email
    const char*                   homepage;        // Plugin homepage
    const char*                   license;         // License information
    const char*                   version;         // Plugin version
    const char*                   description;     // Plugin description
    lsp_android_plugin_type       type;            // Plugin type
    int32_t                       port_count;      // Total number of ports
    int32_t                       audio_inputs;    // Number of audio input ports
    int32_t                       audio_outputs;   // Number of audio output ports
    int32_t                       control_inputs;  // Number of control input ports
    int32_t                       control_outputs; // Number of control output ports
    int32_t                       midi_inputs;     // Number of MIDI input ports
    int32_t                       midi_outputs;    // Number of MIDI output ports
    bool                          has_ui;          // Whether plugin has UI
    bool                          is_realtime_safe; // Whether plugin is realtime safe
    bool                          is_hard_rt_capable; // Whether plugin is hard realtime capable
    int32_t                       latency;         // Plugin latency in samples
    lsp_android_config_flags      supported_configs; // Supported configuration flags
    const char*                   category;        // Plugin category
    const char*                   subcategory;     // Plugin subcategory
    int32_t                       preset_count;    // Number of factory presets
    bool                          supports_state_save; // Whether plugin supports state save/restore
    bool                          supports_programs; // Whether plugin supports programs
    int32_t                       max_block_size;  // Maximum supported block size
    int32_t                       min_block_size;  // Minimum supported block size
    float                         sample_rate_min; // Minimum supported sample rate
    float                         sample_rate_max; // Maximum supported sample rate
} lsp_android_plugin_info;

// Audio buffer format
typedef struct {
    float**     channels;       // Array of channel pointers
    int32_t     channel_count;  // Number of channels
    int32_t     frame_count;    // Number of frames per channel
    float       sample_rate;    // Sample rate in Hz
    int32_t     buffer_size;    // Buffer size in frames
    bool        interleaved;    // Whether audio is interleaved
} lsp_android_audio_buffer;

// MIDI event structure
typedef struct {
    uint32_t    time;           // Time offset in frames
    uint8_t     data[4];        // MIDI data bytes
    uint8_t     size;           // Number of valid data bytes
    uint8_t     channel;        // MIDI channel (0-15)
    uint8_t     type;           // MIDI message type
    uint8_t     reserved;       // Reserved for alignment
} lsp_android_midi_event;

// MIDI buffer structure
typedef struct {
    lsp_android_midi_event* events;     // Array of MIDI events
    int32_t                 event_count; // Number of events
    int32_t                 capacity;    // Buffer capacity
} lsp_android_midi_buffer;

// Transport information
typedef struct {
    double      bpm;            // Beats per minute
    double      beat;           // Current beat position
    double      bar;            // Current bar position
    int32_t     time_signature_numerator;   // Time signature numerator
    int32_t     time_signature_denominator; // Time signature denominator
    bool        playing;        // Whether transport is playing
    bool        recording;      // Whether transport is recording
    bool        looping;        // Whether transport is looping
    double      loop_start;     // Loop start position in beats
    double      loop_end;       // Loop end position in beats
    uint64_t    frame;          // Current frame position
    double      speed;          // Playback speed multiplier
} lsp_android_transport_info;

// Processing context
typedef struct {
    lsp_android_audio_buffer*    audio_in;      // Input audio buffer
    lsp_android_audio_buffer*    audio_out;     // Output audio buffer
    lsp_android_midi_buffer*     midi_in;       // Input MIDI buffer
    lsp_android_midi_buffer*     midi_out;      // Output MIDI buffer
    lsp_android_transport_info*  transport;     // Transport information
    float                        sample_rate;   // Current sample rate
    int32_t                      buffer_size;   // Current buffer size
    uint64_t                     time_frame;    // Current time frame
    bool                         bypass;        // Bypass processing
    float                        gain;          // Global gain multiplier
    void*                        user_data;     // User-defined data
} lsp_android_process_context;

// Plugin state structure
typedef struct {
    void*       data;           // State data
    size_t      size;           // State data size
    int32_t     version;        // State format version
    const char* format;         // State format identifier
    bool        is_dirty;       // Whether state has been modified
    uint32_t    checksum;       // State data checksum
} lsp_android_plugin_state;

// Preset information
typedef struct {
    int32_t     index;          // Preset index
    const char* name;           // Preset name
    const char* description;    // Preset description
    const char* author;         // Preset author
    const char* category;       // Preset category
    const char* tags;           // Preset tags (comma-separated)
    bool        is_factory;     // Whether preset is factory preset
    bool        is_read_only;   // Whether preset is read-only
} lsp_android_preset_info;

// Error codes for plugin operations
typedef enum {
    LSP_ANDROID_SUCCESS = 0,
    LSP_ANDROID_ERROR_INVALID_HANDLE = 1,
    LSP_ANDROID_ERROR_INVALID_PARAMETER = 2,
    LSP_ANDROID_ERROR_PLUGIN_CREATION_FAILED = 3,
    LSP_ANDROID_ERROR_MEMORY_ALLOCATION = 4,
    LSP_ANDROID_ERROR_INVALID_PORT_INDEX = 5,
    LSP_ANDROID_ERROR_NULL_POINTER = 6,
    LSP_ANDROID_ERROR_INVALID_BUFFER_SIZE = 7,
    LSP_ANDROID_ERROR_UNSUPPORTED_PLUGIN_TYPE = 8,
    LSP_ANDROID_ERROR_INVALID_CONFIG_FLAGS = 9,
    LSP_ANDROID_ERROR_INITIALIZATION_FAILED = 10,
    LSP_ANDROID_ERROR_NOT_INITIALIZED = 11,
    LSP_ANDROID_ERROR_ALREADY_INITIALIZED = 12,
    LSP_ANDROID_ERROR_INVALID_SAMPLE_RATE = 13,
    LSP_ANDROID_ERROR_INVALID_CHANNEL_COUNT = 14,
    LSP_ANDROID_ERROR_BUFFER_OVERFLOW = 15,
    LSP_ANDROID_ERROR_BUFFER_UNDERFLOW = 16,
    LSP_ANDROID_ERROR_TIMEOUT = 17,
    LSP_ANDROID_ERROR_THREAD_ERROR = 18,
    LSP_ANDROID_ERROR_LOCK_ERROR = 19,
    LSP_ANDROID_ERROR_IO_ERROR = 20,
    LSP_ANDROID_ERROR_FILE_NOT_FOUND = 21,
    LSP_ANDROID_ERROR_FILE_READ_ERROR = 22,
    LSP_ANDROID_ERROR_FILE_WRITE_ERROR = 23,
    LSP_ANDROID_ERROR_INVALID_FORMAT = 24,
    LSP_ANDROID_ERROR_UNSUPPORTED_FORMAT = 25,
    LSP_ANDROID_ERROR_CODEC_ERROR = 26,
    LSP_ANDROID_ERROR_NETWORK_ERROR = 27,
    LSP_ANDROID_ERROR_PERMISSION_DENIED = 28,
    LSP_ANDROID_ERROR_RESOURCE_BUSY = 29,
    LSP_ANDROID_ERROR_RESOURCE_UNAVAILABLE = 30,
    LSP_ANDROID_ERROR_OPERATION_CANCELLED = 31,
    LSP_ANDROID_ERROR_OPERATION_NOT_SUPPORTED = 32,
    LSP_ANDROID_ERROR_INVALID_STATE = 33,
    LSP_ANDROID_ERROR_STATE_SAVE_FAILED = 34,
    LSP_ANDROID_ERROR_STATE_RESTORE_FAILED = 35,
    LSP_ANDROID_ERROR_PRESET_LOAD_FAILED = 36,
    LSP_ANDROID_ERROR_PRESET_SAVE_FAILED = 37,
    LSP_ANDROID_ERROR_MIDI_ERROR = 38,
    LSP_ANDROID_ERROR_AUDIO_ERROR = 39,
    LSP_ANDROID_ERROR_DSP_ERROR = 40,
    LSP_ANDROID_ERROR_PLUGIN_ERROR = 41,
    LSP_ANDROID_ERROR_HOST_ERROR = 42,
    LSP_ANDROID_ERROR_SYSTEM_ERROR = 43,
    LSP_ANDROID_ERROR_UNKNOWN = 44
} lsp_android_error_code;

// Callback function types
typedef void (*lsp_android_parameter_changed_callback)(
        lsp_android_plugin_handle handle,
    int32_t port_index,
    float value,
    void* user_data);

typedef void (*lsp_android_state_changed_callback)(
        lsp_android_plugin_handle handle,
    void* user_data);

typedef void (*lsp_android_preset_changed_callback)(
        lsp_android_plugin_handle handle,
    int32_t preset_index,
    void* user_data);

typedef void (*lsp_android_error_callback)(
        lsp_android_plugin_handle handle,
    lsp_android_error_code error_code,
    const char* error_message,
    void* user_data);

typedef void (*lsp_android_log_callback)(
    int32_t level,
    const char* message,
    void* user_data);

// Callback registration structure
typedef struct {
    lsp_android_parameter_changed_callback  parameter_changed;
    lsp_android_state_changed_callback      state_changed;
    lsp_android_preset_changed_callback     preset_changed;
    lsp_android_error_callback              error;
    lsp_android_log_callback                log;
    void*                                   user_data;
} lsp_android_callbacks;

// Plugin creation parameters
typedef struct {
    lsp_android_plugin_type     plugin_type;
    lsp_android_config_flags    config_flags;
    float                       sample_rate;
    int32_t                     max_buffer_size;
    int32_t                     channel_count;
    lsp_android_callbacks*      callbacks;
    void*                       user_data;
    const char*                 plugin_path;
    const char*                 preset_path;
    const char*                 state_path;
    bool                        enable_threading;
    int32_t                     thread_priority;
    size_t                      memory_limit;
    bool                        enable_denormal_protection;
    bool                        enable_parameter_smoothing;
    float                       parameter_smoothing_time;
    bool                        enable_tail_processing;
    int32_t                     look_ahead_samples;
    bool                        enable_oversampling;
    int32_t                     oversampling_factor;
} lsp_android_plugin_params;

// Library initialization and cleanup
lsp_android_error_code lsp_android_initialize(void);
void lsp_android_cleanup(void);

// Returns the bridge API version implemented by this library.
int32_t lsp_android_bridge_get_api_version(void);

// Plugin enumeration and information
int32_t lsp_android_get_plugin_count(void);
lsp_android_error_code lsp_android_get_plugin_info(
    lsp_android_plugin_type plugin_type,
    lsp_android_plugin_info* out_info);

lsp_android_error_code lsp_android_enumerate_plugins(
    lsp_android_plugin_info* out_plugins,
    int32_t max_plugins,
    int32_t* out_count);

// Creates a new instance of a plugin with specified type and configuration.
lsp_android_plugin_handle lsp_android_create_plugin(
    lsp_android_plugin_type plugin_type,
    lsp_android_config_flags config_flags);

// Creates a plugin with detailed parameters
lsp_android_error_code lsp_android_create_plugin_ex(
    const lsp_android_plugin_params* params,
    lsp_android_plugin_handle* out_handle);

// Creates a new instance of the parametric equalizer plugin.
lsp_android_plugin_handle lsp_android_create_parametric_eq(void);

// Creates a new instance of the compressor plugin.
lsp_android_plugin_handle lsp_android_create_compressor(void);

// Creates a new instance of the limiter plugin.
lsp_android_plugin_handle lsp_android_create_limiter(void);

// Creates a new instance of the gate plugin.
lsp_android_plugin_handle lsp_android_create_gate(void);

// Creates a new instance of the spectrum analyzer plugin.
lsp_android_plugin_handle lsp_android_create_analyzer(void);

// Destroys a plugin instance
void lsp_android_destroy_plugin(lsp_android_plugin_handle handle);

// Plugin initialization and configuration
lsp_android_error_code lsp_android_initialize_plugin(
        lsp_android_plugin_handle handle,
    float sample_rate,
    int32_t max_buffer_size);

lsp_android_error_code lsp_android_configure_plugin(
    lsp_android_plugin_handle handle,
    lsp_android_config_flags config_flags);

lsp_android_error_code lsp_android_activate_plugin(
    lsp_android_plugin_handle handle);

lsp_android_error_code lsp_android_deactivate_plugin(
    lsp_android_plugin_handle handle);

// Port information and management
int32_t lsp_android_get_port_count(lsp_android_plugin_handle handle);

void lsp_android_get_port_descriptors(
    lsp_android_plugin_handle handle,
    lsp_android_port_descriptor* out_descriptors,
    int32_t max_descriptors);

lsp_android_error_code lsp_android_get_port_descriptor(
    lsp_android_plugin_handle handle,
    int32_t port_index,
    lsp_android_port_descriptor* out_descriptor);

lsp_android_error_code lsp_android_find_port_by_symbol(
    lsp_android_plugin_handle handle,
    const char* symbol,
    int32_t* out_port_index);

lsp_android_error_code lsp_android_get_port_range(
    lsp_android_plugin_handle handle,
    int32_t port_index,
    float* out_min,
    float* out_max,
    float* out_default);

// Parameter control
void lsp_android_set_param(
    lsp_android_plugin_handle handle,
    int32_t port_index,
    float value);

float lsp_android_get_param(
    lsp_android_plugin_handle handle,
    int32_t port_index);

lsp_android_error_code lsp_android_set_param_ex(
    lsp_android_plugin_handle handle,
    int32_t port_index,
    float value,
    bool notify_host);

lsp_android_error_code lsp_android_get_param_ex(
    lsp_android_plugin_handle handle,
    int32_t port_index,
    float* out_value);

lsp_android_error_code lsp_android_set_param_by_symbol(
    lsp_android_plugin_handle handle,
    const char* symbol,
    float value);

lsp_android_error_code lsp_android_get_param_by_symbol(
    lsp_android_plugin_handle handle,
    const char* symbol,
    float* out_value);

lsp_android_error_code lsp_android_reset_param(
    lsp_android_plugin_handle handle,
    int32_t port_index);

lsp_android_error_code lsp_android_reset_all_params(
    lsp_android_plugin_handle handle);

// Audio processing
void lsp_android_process(
    lsp_android_plugin_handle handle,
    const float* in_buffer,
    float* out_buffer,
    int32_t num_frames);

lsp_android_error_code lsp_android_process_ex(
    lsp_android_plugin_handle handle,
    lsp_android_process_context* context);

lsp_android_error_code lsp_android_process_interleaved(
    lsp_android_plugin_handle handle,
    const float* in_buffer,
    float* out_buffer,
    int32_t num_frames,
    int32_t channel_count);

lsp_android_error_code lsp_android_process_planar(
    lsp_android_plugin_handle handle,
    const float* const* in_buffers,
    float* const* out_buffers,
    int32_t num_frames,
    int32_t channel_count);

lsp_android_error_code lsp_android_process_midi(
    lsp_android_plugin_handle handle,
    const lsp_android_midi_buffer* midi_in,
    lsp_android_midi_buffer* midi_out);

lsp_android_error_code lsp_android_flush_plugin(
    lsp_android_plugin_handle handle);

lsp_android_error_code lsp_android_get_latency(
    lsp_android_plugin_handle handle,
    int32_t* out_latency);

lsp_android_error_code lsp_android_get_tail_time(
    lsp_android_plugin_handle handle,
    float* out_tail_time);

// State management
lsp_android_error_code lsp_android_save_state(
    lsp_android_plugin_handle handle,
    lsp_android_plugin_state* out_state);

lsp_android_error_code lsp_android_restore_state(
    lsp_android_plugin_handle handle,
    const lsp_android_plugin_state* state);

lsp_android_error_code lsp_android_save_state_to_file(
    lsp_android_plugin_handle handle,
    const char* file_path);

lsp_android_error_code lsp_android_restore_state_from_file(
    lsp_android_plugin_handle handle,
    const char* file_path);

lsp_android_error_code lsp_android_get_state_size(
    lsp_android_plugin_handle handle,
    size_t* out_size);

lsp_android_error_code lsp_android_serialize_state(
    lsp_android_plugin_handle handle,
    void* buffer,
    size_t buffer_size,
    size_t* out_bytes_written);

lsp_android_error_code lsp_android_deserialize_state(
    lsp_android_plugin_handle handle,
    const void* buffer,
    size_t buffer_size);

// Preset management
lsp_android_error_code lsp_android_get_preset_count(
    lsp_android_plugin_handle handle,
    int32_t* out_count);

lsp_android_error_code lsp_android_get_preset_info(
    lsp_android_plugin_handle handle,
    int32_t preset_index,
    lsp_android_preset_info* out_info);

lsp_android_error_code lsp_android_load_preset(
    lsp_android_plugin_handle handle,
    int32_t preset_index);

lsp_android_error_code lsp_android_save_preset(
    lsp_android_plugin_handle handle,
    const char* name,
    const char* description);

lsp_android_error_code lsp_android_delete_preset(
    lsp_android_plugin_handle handle,
    int32_t preset_index);

lsp_android_error_code lsp_android_load_preset_from_file(
    lsp_android_plugin_handle handle,
    const char* file_path);

lsp_android_error_code lsp_android_save_preset_to_file(
    lsp_android_plugin_handle handle,
    const char* file_path,
    const char* name,
    const char* description);

// Callback management
lsp_android_error_code lsp_android_set_callbacks(
    lsp_android_plugin_handle handle,
    const lsp_android_callbacks* callbacks);

lsp_android_error_code lsp_android_set_parameter_callback(
    lsp_android_plugin_handle handle,
    lsp_android_parameter_changed_callback callback,
    void* user_data);

lsp_android_error_code lsp_android_set_state_callback(
    lsp_android_plugin_handle handle,
    lsp_android_state_changed_callback callback,
    void* user_data);

lsp_android_error_code lsp_android_set_error_callback(
    lsp_android_plugin_handle handle,
    lsp_android_error_callback callback,
    void* user_data);

// Transport and timing
lsp_android_error_code lsp_android_set_transport_info(
    lsp_android_plugin_handle handle,
    const lsp_android_transport_info* transport);

lsp_android_error_code lsp_android_get_transport_info(
    lsp_android_plugin_handle handle,
    lsp_android_transport_info* out_transport);

lsp_android_error_code lsp_android_set_sample_rate(
    lsp_android_plugin_handle handle,
    float sample_rate);

lsp_android_error_code lsp_android_get_sample_rate(
    lsp_android_plugin_handle handle,
    float* out_sample_rate);

lsp_android_error_code lsp_android_set_buffer_size(
    lsp_android_plugin_handle handle,
    int32_t buffer_size);

lsp_android_error_code lsp_android_get_buffer_size(
    lsp_android_plugin_handle handle,
    int32_t* out_buffer_size);

// Performance and monitoring
lsp_android_error_code lsp_android_get_cpu_usage(
    lsp_android_plugin_handle handle,
    float* out_cpu_usage);

lsp_android_error_code lsp_android_get_memory_usage(
    lsp_android_plugin_handle handle,
    size_t* out_memory_usage);

lsp_android_error_code lsp_android_reset_performance_counters(
    lsp_android_plugin_handle handle);

lsp_android_error_code lsp_android_get_processing_time(
    lsp_android_plugin_handle handle,
    double* out_processing_time);

// Utility functions
lsp_android_error_code lsp_android_bypass_plugin(
    lsp_android_plugin_handle handle,
    bool bypass);

lsp_android_error_code lsp_android_is_bypassed(
    lsp_android_plugin_handle handle,
    bool* out_bypassed);

lsp_android_error_code lsp_android_mute_plugin(
    lsp_android_plugin_handle handle,
    bool mute);

lsp_android_error_code lsp_android_is_muted(
    lsp_android_plugin_handle handle,
    bool* out_muted);

lsp_android_error_code lsp_android_set_gain(
    lsp_android_plugin_handle handle,
    float gain);

lsp_android_error_code lsp_android_get_gain(
    lsp_android_plugin_handle handle,
    float* out_gain);

// Task 2.1: Required functions with exact names specified in task
lsp_android_error_code plugin_create(lsp_android_plugin_handle* out_handle);

lsp_android_error_code plugin_destroy(lsp_android_plugin_handle handle);

lsp_android_error_code set_param(
    lsp_android_plugin_handle handle,
    int32_t port_index,
    float value);

lsp_android_error_code get_param(
    lsp_android_plugin_handle handle,
    int32_t port_index,
    float* out_value);

lsp_android_error_code process(
    lsp_android_plugin_handle handle,
    const float* in_buffer,
    float* out_buffer,
    int32_t num_frames);

// Error handling and debugging
const char* lsp_android_get_error_message(lsp_android_error_code error_code);

lsp_android_error_code lsp_android_get_last_error(void);

void lsp_android_clear_last_error(void);

lsp_android_error_code lsp_android_set_log_level(int32_t level);

lsp_android_error_code lsp_android_get_log_level(int32_t* out_level);

lsp_android_error_code lsp_android_set_log_callback(
    lsp_android_log_callback callback,
    void* user_data);

// Version and build information
const char* lsp_android_get_version_string(void);

const char* lsp_android_get_build_date(void);

const char* lsp_android_get_build_time(void);

const char* lsp_android_get_compiler_info(void);

const char* lsp_android_get_platform_info(void);

lsp_android_error_code lsp_android_get_build_info(
    const char** out_version,
    const char** out_build_date,
    const char** out_build_time,
    const char** out_compiler,
    const char** out_platform);

// Thread safety and concurrency
lsp_android_error_code lsp_android_lock_plugin(
    lsp_android_plugin_handle handle);

lsp_android_error_code lsp_android_unlock_plugin(
    lsp_android_plugin_handle handle);

lsp_android_error_code lsp_android_try_lock_plugin(
    lsp_android_plugin_handle handle,
    bool* out_acquired);

lsp_android_error_code lsp_android_is_thread_safe(
    lsp_android_plugin_handle handle,
    bool* out_thread_safe);

// Memory management
lsp_android_error_code lsp_android_allocate_buffer(
    size_t size,
    void** out_buffer);

void lsp_android_free_buffer(void* buffer);

lsp_android_error_code lsp_android_reallocate_buffer(
    void** buffer,
    size_t old_size,
    size_t new_size);

lsp_android_error_code lsp_android_get_memory_stats(
    size_t* out_allocated,
    size_t* out_peak,
    size_t* out_current);

// Plugin validation and testing
lsp_android_error_code lsp_android_validate_plugin(
    lsp_android_plugin_handle handle,
    bool* out_valid);

lsp_android_error_code lsp_android_run_self_test(
    lsp_android_plugin_handle handle,
    bool* out_passed);

lsp_android_error_code lsp_android_benchmark_plugin(
    lsp_android_plugin_handle handle,
    int32_t iterations,
    double* out_avg_time,
    double* out_min_time,
    double* out_max_time);

// Advanced features
lsp_android_error_code lsp_android_set_oversampling(
    lsp_android_plugin_handle handle,
    int32_t factor);

lsp_android_error_code lsp_android_get_oversampling(
    lsp_android_plugin_handle handle,
    int32_t* out_factor);

lsp_android_error_code lsp_android_set_look_ahead(
    lsp_android_plugin_handle handle,
    int32_t samples);

lsp_android_error_code lsp_android_get_look_ahead(
    lsp_android_plugin_handle handle,
    int32_t* out_samples);

lsp_android_error_code lsp_android_enable_denormal_protection(
    lsp_android_plugin_handle handle,
    bool enable);

lsp_android_error_code lsp_android_is_denormal_protection_enabled(
    lsp_android_plugin_handle handle,
    bool* out_enabled);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // LSP_ANDROID_BRIDGE_H
