//#include <jni.h>
//#include <stdio.h>
//#include "ffmpeg/android_log.h"
//#include "audio_player.c"
//#include "ffmpeg_cmd.c"
//#include "ffmpeg_pusher.cpp"
//#include "media_player.c"
//#include "openSL_audio_player.c"
//#include "video_filter.c"
//#include "video_player.c"
//
////#ifdef __cplusplus
////extern "C" {
////#endif
//
//static JNINativeMethod sMethods[] =
//        {
//                {"audioPlay",        "(Ljava/lang/String;)V",                                         (void *) Java_com_frank_ffmpeg_MediaPlayer_release},
//                {"openSLPlay",       "(Ljava/lang/String;)V",                                         (void *) Java_com_frank_ffmpeg_AudioPlayer_playAudio},
//                {"openSLStop",       "()I",                                                           (void *) Java_com_frank_ffmpeg_AudioPlayer_play},
//                {"ffmpegHandle",     "([Ljava/lang/String;)I",                                        (void *) Java_com_frank_ffmpeg_FFmpegCmd_handle},
//                {"pusherPushStream", "(Ljava/lang/String;Ljava/lang/String;)I",                       (void *) Java_com_frank_ffmpeg_Pusher_pushStream},
//                {"mediaSetup",       "(Ljava/lang/String;Landroid/view/Surface;)I",                   (void *) Java_com_frank_ffmpeg_MediaPlayer_setup},
//                {"mediaPlay",        "()I",                                                           (void *) Java_com_frank_ffmpeg_MediaPlayer_play},
//                {"mediaRelease",     "()V",                                                           (void *) Java_com_frank_ffmpeg_AudioPlayer_stop},
//                {"videoFilter",      "(Ljava/lang/String;Landroid/view/Surface;Ljava/lang/String;)I", (void *) Java_com_frank_ffmpeg_VideoPlayer_filter},
//                {"videoAgain",       "()V",                                                           (void *) Java_com_frank_ffmpeg_VideoPlayer_again},
//                {"videoRelease",     "()V",                                                           (void *) Java_com_frank_ffmpeg_VideoPlayer_release},
//                {"videoPlayAudio",   "(Z)V",                                                          (void *) Java_com_frank_ffmpeg_VideoPlayer_playAudio},
//                {"videoPlay",        "(Ljava/lang/String;Landroid/view/Surface;)I",                   (void *) Java_com_frank_ffmpeg_VideoPlayer_play},
//                {"videoSetPlayRate", "(F)I",                                                          (void *) Java_com_frank_ffmpeg_VideoPlayer_setPlayRate},
//                {"videoGetDuration", "()I",                                                           (void *) Java_com_frank_ffmpeg_VideoPlayer_getDuration},
//        };
//
//#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
//
//// 包名改变时，修改这里
//#define PACKAGE_CLASS "com/frank/ffmpeg/FFmpeg"
//
//jint JNI_OnLoad(JavaVM *vm, void *reserved) {
//    JNIEnv *env;
//    if (vm->GetEnv((void **) (&env), JNI_VERSION_1_6) != JNI_OK) {
//        return -1;
//    }
//    set_vm(vm);
//
//    jclass clazz = env->FindClass(PACKAGE_CLASS);
//    if (!clazz) {
//        LOGE("OnLoad Can't find class %s", PACKAGE_CLASS);
//        return 0;
//    }
//    env->RegisterNatives(clazz, sMethods, NELEM(sMethods));
//    LOGI("OnLoad Success %s", PACKAGE_CLASS);
//    return JNI_VERSION_1_6;
//}
//
//void JNI_OnUnload(JavaVM *vm, void *reserved) {
//    JNIEnv *env;
//    if (vm->GetEnv((void **) (&env), JNI_VERSION_1_6) != JNI_OK)
//        return;
//    jclass clazz = env->FindClass(PACKAGE_CLASS);
//    if (!clazz) {
//        LOGI("OnUnload class success %s", PACKAGE_CLASS);
//        return;
//    }
//    jint r = env->UnregisterNatives(clazz);
//    if (r == 0) {
//        LOGI("OnUnload class success %s", PACKAGE_CLASS);
//    } else {
//        LOGI("OnUnload class failed %s", PACKAGE_CLASS);
//    }
//}
//
////#ifdef __cplusplus
////}
////#endif