cd ffmpeg_ijk

make clean

archbit=32
 
#===========================
if [ $archbit -eq 32 ];then
echo "build for 32bit"
#32bit
abi='armeabi'
cpu='arm'
arch='arm'
android='androideabi'
else
#64bit
echo "build for 64bit"
abi='arm64-v8a'
cpu='aarch64'
arch='arm64'
android='android'
fi

export NDK=/usr/android/ndk/android-ndk-r10e
export PREBUILT=$NDK/toolchains/$cpu-linux-$android-4.9/prebuilt
export PLATFORM=$NDK/platforms/android-21/arch-$cpu
export PREFIX=/mnt/d/GithubProject/FFmpegAndroid/ff-onelib

build_one(){
  ./configure \
    --prefix=$PREFIX \
    --target-os=android \
    --arch=$cpu \
    --cc=$PREBUILT/linux-x86_64/bin/$cpu-linux-$android-gcc \
    --cross-prefix=$PREBUILT/linux-x86_64/bin/$cpu-linux-$android- \
    --nm=$PREBUILT/linux-x86_64/bin/$cpu-linux-$android-nm \
    --enable-cross-compile \
    --enable-runtime-cpudetect \
    --sysroot=$PLATFORM \
    --disable-stripping \
    --enable-static \
    --disable-shared \
    --enable-gpl \
    --enable-nonfree \
    --enable-version3 \
    --enable-small \
    --enable-neon \
    --enable-mediacodec \
    --enable-asm \
    --enable-zlib \
    --disable-ffprobe \
    --disable-ffplay \
    --enable-ffmpeg \
    --disable-debug \
    --enable-jni \
    --enable-encoder=libfdk_aac \
    --enable-decoder=libfdk_aac \
    --enable-libfdk-aac \
    --extra-cflags="-Os -fpic $ADDI_CFLAGS -mfpu=neon" \
    --extra-ldflags="$ADDI_LDFLAGS"
}

#  -mcpu=cortex-a8 -mfloat-abi=softfp -marm -march=armv7-a

ADDI_CFLAGS="-marm -I/mnt/d/GithubProject/fdk-aac/ffmpeg_build/include/ -DANDROID"
ADDI_LDFLAGS="-L/mnt/d/GithubProject/fdk-aac/ffmpeg_build/lib/"

build_one

make
make install

$PREBUILT/linux-x86_64/bin/$cpu-linux-$android-ld \
-rpath-link=$PLATFORM/usr/lib \
-L$PLATFORM/usr/lib \
-L$PREFIX/lib \
-soname libffmpeg.so \
-shared -nostdlib -Bsymbolic \
--whole-archive \
--no-undefined \
-o $PREFIX/libffmpeg.so \
    libavcodec/libavcodec.a \
    libavfilter/libavfilter.a \
    libswresample/libswresample.a \
    libavformat/libavformat.a \
    libavutil/libavutil.a \
    libswscale/libswscale.a \
    libpostproc/libpostproc.a \
    libavdevice/libavdevice.a \
-lc -lm -lz -ldl -llog \
--dynamic-linker=/system/bin/linker $PREBUILT/linux-x86_64/lib/gcc/$cpu-linux-$android/4.9/libgcc.a

cd ..
