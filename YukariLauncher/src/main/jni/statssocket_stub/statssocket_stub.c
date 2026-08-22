// Stub for libstatssocket.so
// On some Adreno devices the vendor GLES driver has a DT_NEEDED on
// libstatssocket.so, but the library is not exposed to apps in the
// classloader namespace (it is an LLNDK / vendor-private lib).  When the
// app dlopen()s libGLESv2.so -> vendor driver, the linker fails with
//   dlopen failed: library "libstatssocket.so" not found
// Bundling a tiny stub with SONAME libstatssocket.so in the app's
// native-lib dir satisfies the DT_NEEDED and lets the driver load.
// The driver typically links it only for optional statsd telemetry; if it
// never calls a symbol the empty library is enough.  For the symbols it
// might call we provide weak no-ops that forward to the real system
// library when present.

#include <stddef.h>
#include <stdint.h>

// Generic no-op that matches most statssocket symbols (they return int).
int statsd_write(int32_t code, const void* data, size_t len) { (void)code; (void)data; (void)len; return 0; }
int stats_write(int32_t code, const void* data, size_t len) { return statsd_write(code, data, len); }

// StatsLog symbols used via libstatslog
int android_util_StatsLog_write(int32_t code, const void* data, size_t len) { return 0; }
int android_util_StatsLog_writeRaw(int32_t id, const void* buf, size_t len) { return 0; }

// StatsSocket symbols (C++ mangled names are not needed for most drivers;
// they use the C wrappers above).  Provide a few common mangled aliases as
// weak symbols so the linker does not fail if the driver references them.
__attribute__((weak)) void _ZN7android4util11StatsSocket5writeEiPKvm(void) {}
__attribute__((weak)) void _ZN7android4util11StatsSocketC1Ev(void) {}
__attribute__((weak)) void _ZN7android4util11StatsSocketD1Ev(void) {}

