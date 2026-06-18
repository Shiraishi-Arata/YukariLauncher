//
// Created by maks on 15.01.2025.
//

#include <jni.h>
#include <stdbool.h>
#include <bytehook.h>
#include <dlfcn.h>
#include <android/dlext.h>
#include <android/log.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include "stdio_is.h"

static _Atomic bool exit_tripped = false;

typedef void (*exit_func)(int);

typedef void* (*dlopen_func)(const char*, int);
typedef void* (*android_dlopen_ext_func)(const char*, int, const android_dlextinfo*);

static void* hooked_dlopen(const char* filename, int flags);
static void* hooked_android_dlopen_ext(const char* filename, int flags, const android_dlextinfo* extinfo);

static bool should_retry_from_private_dir(const char* filename) {
    if(filename == NULL || filename[0] != '/') return false;
    // Android linker namespaces reject app-external storage paths on recent Android
    // versions. App-private data/cache paths are still part of the app namespace.
    if(strncmp(filename, "/data/", 6) == 0 || strncmp(filename, "/system/", 8) == 0 ||
       strncmp(filename, "/vendor/", 8) == 0 || strncmp(filename, "/apex/", 6) == 0) {
        return false;
    }
    const char* suffix = strrchr(filename, '.');
    return suffix != NULL && strcmp(suffix, ".so") == 0;
}

static unsigned long hash_library_path(const char* path) {
    unsigned long hash = 5381;
    int c;
    while((c = *path++) != 0) {
        hash = ((hash << 5) + hash) + (unsigned char)c;
    }
    return hash;
}

static bool build_library_path(const char* dir, const char* basename, char* out_path, size_t out_path_size) {
    if(dir == NULL || dir[0] == '\0' || basename == NULL || basename[0] == '\0') return false;
    if(snprintf(out_path, out_path_size, "%s/%s", dir, basename) >= (int)out_path_size) return false;
    return access(out_path, R_OK) == 0;
}

static bool find_library_in_path_list(const char* path_list, const char* basename, char* out_path, size_t out_path_size) {
    if(path_list == NULL || basename == NULL) return false;

    const char* entry_start = path_list;
    while(*entry_start != '\0') {
        const char* entry_end = strchr(entry_start, ':');
        size_t entry_length = entry_end == NULL ? strlen(entry_start) : (size_t)(entry_end - entry_start);
        if(entry_length > 0 && entry_length < PATH_MAX) {
            char dir[PATH_MAX];
            memcpy(dir, entry_start, entry_length);
            dir[entry_length] = '\0';
            if(build_library_path(dir, basename, out_path, out_path_size)) return true;
        }
        if(entry_end == NULL) break;
        entry_start = entry_end + 1;
    }
    return false;
}

static bool find_override_library(const char* filename, char* out_path, size_t out_path_size) {
    if(!should_retry_from_private_dir(filename)) return false;

    const char* basename = strrchr(filename, '/');
    basename = basename == NULL ? filename : basename + 1;

    const char* mod_library_dir = getenv("YUKARI_MOD_LIBRARY_DIR");
    if(build_library_path(mod_library_dir, basename, out_path, out_path_size)) return true;

    return find_library_in_path_list(getenv("LD_LIBRARY_PATH"), basename, out_path, out_path_size);
}

static void* try_override_library(const char* filename, int flags) {
    char override_path[PATH_MAX];
    if(!find_override_library(filename, override_path, sizeof(override_path))) return NULL;

    __android_log_print(ANDROID_LOG_INFO, "native_load_hook", "Redirecting native load from %s to override %s", filename, override_path);
    return BYTEHOOK_CALL_PREV(hooked_dlopen, dlopen_func, override_path, flags);
}

static void* try_override_library_ext(const char* filename, int flags, const android_dlextinfo* extinfo) {
    char override_path[PATH_MAX];
    if(!find_override_library(filename, override_path, sizeof(override_path))) return NULL;

    __android_log_print(ANDROID_LOG_INFO, "native_load_hook", "Redirecting extended native load from %s to override %s", filename, override_path);
    return BYTEHOOK_CALL_PREV(hooked_android_dlopen_ext, android_dlopen_ext_func, override_path, flags, extinfo);
}

static bool copy_file_contents(int source_fd, int destination_fd) {
    char buffer[32768];
    ssize_t read_count;
    while((read_count = read(source_fd, buffer, sizeof(buffer))) > 0) {
        char* cursor = buffer;
        while(read_count > 0) {
            ssize_t write_count = write(destination_fd, cursor, read_count);
            if(write_count < 0) return false;
            cursor += write_count;
            read_count -= write_count;
        }
    }
    return read_count == 0;
}

static bool prepare_private_library_copy(const char* filename, char* out_path, size_t out_path_size) {
    const char* tmpdir = getenv("TMPDIR");
    if(tmpdir == NULL || tmpdir[0] == '\0') return false;

    char native_load_dir[PATH_MAX];
    if(snprintf(native_load_dir, sizeof(native_load_dir), "%s/native_load", tmpdir) >= (int)sizeof(native_load_dir)) {
        return false;
    }
    if(mkdir(native_load_dir, 0700) != 0 && errno != EEXIST) return false;

    const char* basename = strrchr(filename, '/');
    basename = basename == NULL ? filename : basename + 1;
    if(snprintf(out_path, out_path_size, "%s/%lx_%s", native_load_dir, hash_library_path(filename), basename) >= (int)out_path_size) {
        return false;
    }

    struct stat source_stat;
    if(stat(filename, &source_stat) != 0) return false;

    struct stat destination_stat;
    if(stat(out_path, &destination_stat) == 0 &&
       destination_stat.st_size == source_stat.st_size &&
       destination_stat.st_mtime >= source_stat.st_mtime) {
        return true;
    }

    int source_fd = open(filename, O_RDONLY | O_CLOEXEC);
    if(source_fd < 0) return false;

    int destination_fd = open(out_path, O_CREAT | O_TRUNC | O_WRONLY | O_CLOEXEC, 0700);
    if(destination_fd < 0) {
        close(source_fd);
        return false;
    }

    bool copied = copy_file_contents(source_fd, destination_fd);
    if(copied) {
        fchmod(destination_fd, 0700);
        struct timespec times[2] = { source_stat.st_atim, source_stat.st_mtim };
        futimens(destination_fd, times);
    }

    close(destination_fd);
    close(source_fd);
    if(!copied) unlink(out_path);
    return copied;
}

static void* retry_dlopen_from_private_dir(const char* filename, int flags) {
    if(!should_retry_from_private_dir(filename)) return NULL;

    char private_path[PATH_MAX];
    if(!prepare_private_library_copy(filename, private_path, sizeof(private_path))) return NULL;

    __android_log_print(ANDROID_LOG_INFO, "native_load_hook", "Retrying native load from app-private path: %s", private_path);
    return BYTEHOOK_CALL_PREV(hooked_dlopen, dlopen_func, private_path, flags);
}

static void* hooked_dlopen(const char* filename, int flags) {
    void* handle = try_override_library(filename, flags);
    if(handle == NULL) {
        handle = BYTEHOOK_CALL_PREV(hooked_dlopen, dlopen_func, filename, flags);
    }
    if(handle == NULL) {
        handle = retry_dlopen_from_private_dir(filename, flags);
    }
    BYTEHOOK_POP_STACK();
    return handle;
}

static void* hooked_android_dlopen_ext(const char* filename, int flags, const android_dlextinfo* extinfo) {
    void* handle = try_override_library_ext(filename, flags, extinfo);
    if(handle == NULL) {
        handle = BYTEHOOK_CALL_PREV(hooked_android_dlopen_ext, android_dlopen_ext_func, filename, flags, extinfo);
    }
    if(handle == NULL && should_retry_from_private_dir(filename)) {
        char private_path[PATH_MAX];
        if(prepare_private_library_copy(filename, private_path, sizeof(private_path))) {
            __android_log_print(ANDROID_LOG_INFO, "native_load_hook", "Retrying extended native load from app-private path: %s", private_path);
            handle = BYTEHOOK_CALL_PREV(hooked_android_dlopen_ext, android_dlopen_ext_func, private_path, flags, extinfo);
        }
    }
    BYTEHOOK_POP_STACK();
    return handle;
}


static void custom_exit(int code) {
    // If the exit was already done (meaning it is recursive or from a different thread), pass the call through
    if(exit_tripped) {
        BYTEHOOK_CALL_PREV(custom_exit, exit_func, code);
        BYTEHOOK_POP_STACK();
        return;
    }
    exit_tripped = true;
    // Perform a nominal exit, as we expect.
    nominal_exit(code, false);
    BYTEHOOK_POP_STACK();
}

static void custom_atexit() {
    // Same as custom_exit, but without the code or the exit passthrough.
    if(exit_tripped) {
        return;
    }
    exit_tripped = true;
    nominal_exit(0, false);
}

static bool init_exit_hook() {
    void* bytehook_handle = dlopen("libbytehook.so", RTLD_NOW);
    if(bytehook_handle == NULL) {
        goto dlerror;
    }

    bytehook_stub_t (*bytehook_hook_all_p)(const char *callee_path_name, const char *sym_name, void *new_func,
                                           bytehook_hooked_t hooked, void *hooked_arg);
    int (*bytehook_init_p)(int mode, bool debug);

    bytehook_hook_all_p = dlsym(bytehook_handle, "bytehook_hook_all");
    bytehook_init_p = dlsym(bytehook_handle, "bytehook_init");

    if(bytehook_hook_all_p == NULL || bytehook_init_p == NULL) {
        goto dlerror;
    }
    int bhook_status = bytehook_init_p(BYTEHOOK_MODE_AUTOMATIC, false);
    if(bhook_status == BYTEHOOK_STATUS_CODE_OK) {
        bytehook_stub_t stub = bytehook_hook_all_p(NULL, "exit", &custom_exit, NULL, NULL);
        bytehook_stub_t dlopen_stub = bytehook_hook_all_p(NULL, "dlopen", &hooked_dlopen, NULL, NULL);
        bytehook_stub_t android_dlopen_ext_stub = bytehook_hook_all_p(NULL, "android_dlopen_ext", &hooked_android_dlopen_ext, NULL, NULL);
        __android_log_print(ANDROID_LOG_INFO, "exit_hook", "Successfully initialized hooks, exit=%p dlopen=%p android_dlopen_ext=%p", stub, dlopen_stub, android_dlopen_ext_stub);
        return true;
    } else {
        __android_log_print(ANDROID_LOG_INFO, "exit_hook", "bytehook_init failed (%i)", bhook_status);
        dlclose(bytehook_handle);
        return false;
    }

    dlerror:
    if(bytehook_handle != NULL) dlclose(bytehook_handle);
    __android_log_print(ANDROID_LOG_ERROR, "exit_hook", "Failed to load hook library: %s", dlerror());
    return false;
}

JNIEXPORT void JNICALL
Java_com_arata_yukarilauncher_utils_runtime_JREUtils_initializeGameExitHook(JNIEnv *env, jclass clazz) {
    bool hookReady = init_exit_hook();
    if(!hookReady){
        // If we can't hook, register atexit(). This won't report a proper error code,
        // but it will prevent a SIGSEGV or a SIGABRT from the depths of Dalvik that happens
        // on exit().
        atexit(custom_atexit);
    }
}