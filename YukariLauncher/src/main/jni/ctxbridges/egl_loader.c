//
// Created by maks on 21.09.2022.
//
#include <stddef.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <string.h>
#include "br_loader.h"
#include "egl_loader.h"

EGLBoolean (*eglMakeCurrent_p) (EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx);
EGLBoolean (*eglDestroyContext_p) (EGLDisplay dpy, EGLContext ctx);
EGLBoolean (*eglDestroySurface_p) (EGLDisplay dpy, EGLSurface surface);
EGLBoolean (*eglTerminate_p) (EGLDisplay dpy);
EGLBoolean (*eglReleaseThread_p) (void);
EGLContext (*eglGetCurrentContext_p) (void);
EGLDisplay (*eglGetDisplay_p) (NativeDisplayType display);
EGLBoolean (*eglInitialize_p) (EGLDisplay dpy, EGLint *major, EGLint *minor);
EGLBoolean (*eglChooseConfig_p) (EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config);
EGLBoolean (*eglGetConfigAttrib_p) (EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value);
EGLBoolean (*eglBindAPI_p) (EGLenum api);
EGLSurface (*eglCreatePbufferSurface_p) (EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list);
EGLSurface (*eglCreateWindowSurface_p) (EGLDisplay dpy, EGLConfig config, NativeWindowType window, const EGLint *attrib_list);
EGLBoolean (*eglSwapBuffers_p) (EGLDisplay dpy, EGLSurface draw);
EGLint (*eglGetError_p) (void);
EGLContext (*eglCreateContext_p) (EGLDisplay dpy, EGLConfig config, EGLContext share_list, const EGLint *attrib_list);
EGLBoolean (*eglSwapInterval_p) (EGLDisplay dpy, EGLint interval);
EGLSurface (*eglGetCurrentSurface_p) (EGLint readdraw);
EGLBoolean (*eglQuerySurface_p)(EGLDisplay display, EGLSurface surface, EGLint attribute, EGLint * value);

void dlsym_EGL() {
    void* dl_handle = NULL;
    void* fallback_handle = NULL;
    char* eglName = NULL;
    char* gles = getenv("LIBGL_GLES");

    if (gles && !strncmp(gles, "libGLESv2_angle.so", 18))
    {
        eglName = "libEGL_angle.so";
    } else {
        eglName = getenv("POJAVEXEC_EGL");
    }

    if (eglName)
        dl_handle = dlopen(eglName, RTLD_LOCAL | RTLD_LAZY);

    if (dl_handle == NULL)
        dl_handle = dlopen("libEGL.so", RTLD_LOCAL | RTLD_LAZY);
    else
        fallback_handle = dlopen("libEGL.so", RTLD_LOCAL | RTLD_LAZY);

    if (dl_handle == NULL) abort();

    #define LOAD_EGL_SYM(name) do { \
        name##_p = GLGetProcAddress(dl_handle, #name); \
        if (name##_p == NULL && fallback_handle != NULL) \
            name##_p = GLGetProcAddress(fallback_handle, #name); \
    } while(0)

    LOAD_EGL_SYM(eglBindAPI);
    LOAD_EGL_SYM(eglChooseConfig);
    LOAD_EGL_SYM(eglCreateContext);
    LOAD_EGL_SYM(eglCreatePbufferSurface);
    LOAD_EGL_SYM(eglCreateWindowSurface);
    LOAD_EGL_SYM(eglDestroyContext);
    LOAD_EGL_SYM(eglDestroySurface);
    LOAD_EGL_SYM(eglGetConfigAttrib);
    LOAD_EGL_SYM(eglGetCurrentContext);
    LOAD_EGL_SYM(eglGetDisplay);
    LOAD_EGL_SYM(eglGetError);
    LOAD_EGL_SYM(eglInitialize);
    LOAD_EGL_SYM(eglMakeCurrent);
    LOAD_EGL_SYM(eglSwapBuffers);
    LOAD_EGL_SYM(eglReleaseThread);
    LOAD_EGL_SYM(eglSwapInterval);
    LOAD_EGL_SYM(eglTerminate);
    LOAD_EGL_SYM(eglGetCurrentSurface);
    LOAD_EGL_SYM(eglQuerySurface);
}
