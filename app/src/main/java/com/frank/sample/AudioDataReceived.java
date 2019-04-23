package com.frank.sample;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Getnway
 * @Email Getnway@gmail.com
 * @since 2019-03-06
 */
public class AudioDataReceived implements IAudioDataReceivedListener {
    private static final String TAG = "AudioDataReceived";
    private FileOutputStream os = null;

    public void setFile(String filePath) {
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioDataStart() {

    }

    @Override
    public void onAudioDataReceived(short[] data, long durationMs) {
        Log.d(TAG, String.format("call onAudioDataReceived(): data = [%s]", data == null ? -1 : data.length));
        if (os == null || data == null) return;
        try {
            byte[] bytes = new byte[data.length * 2];
            ByteBuffer.wrap(bytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .put(data);
            os.write(bytes);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioDataEnd(long durationMs) {
        if (os == null) return;
        try {
            Log.d(TAG, "call onAudioDataEnd()");
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
