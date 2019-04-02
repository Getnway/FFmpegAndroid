//package com.frank.ffmpeg;
//
///**
// * @author Getnway
// * @Email Getnway@gmail.com
// * @since 2019-03-04
// */
//public class FFmpeg {
//    static {
//        System.loadLibrary("media-handle");
//    }
//
//    //调用AudioTrack播放
//    public native void audioPlay(String audioPath);
//    //调用OpenSL ES播放
//    public native void openSLPlay(String audioPath);
//    //调用OpenSL ES播放
//    public native void openSLStop(String audioPath);
//
//    private native static int ffmpegHandle(String[] commands);
//
//    //选择本地文件推流到指定平台直播
//    public native int pusherPushStream(String filePath, String liveUrl);
//
//    public native int mediaSetup(String filePath, Object surface);
//    public native int mediaPlay();
//    public native void mediaRelease();
//
//    public native int videoFilter(String filePath, Object surface, String filterType);
//    public native void videoAgain();
//    public native void videoRelease();
//    public native void videoPlayAudio(boolean play);
//
//    public native int videoPlay(String filePath, Object surface);
//    public native int videoSetPlayRate(float playRate);
//    public native int videoGetDuration();
//}
