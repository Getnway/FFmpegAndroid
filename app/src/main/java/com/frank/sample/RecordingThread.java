/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.frank.sample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import static android.media.AudioManager.MODE_IN_CALL;
import static android.media.AudioManager.MODE_IN_COMMUNICATION;
import static android.media.AudioManager.MODE_NORMAL;
import static android.media.AudioManager.MODE_RINGTONE;

public class RecordingThread {
    private static final String TAG = RecordingThread.class.getSimpleName();
    private static final int SAMPLE_RATE = 44100;
    private Context context;
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            Log.d(TAG, String.format("call onReceive(): state = [%s], intent = [%s]", state, intent));
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                mAudioManager.setBluetoothScoOn(true);  //打开SCO
                Log.d(TAG, String.format("call onReceive():%s, %s %s", isBluetoothHeadsetConnected(), mAudioManager.isBluetoothScoOn(), mAudioManager.isBluetoothScoAvailableOffCall()));
                start();//开始录音
            } else {
                if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state && recording()) {
                    stopRecording();
                } else {
                    //等待一秒后再尝试启动SCO
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAudioManager.startBluetoothSco();
                }
            }
        }
    };

    public RecordingThread(Context context, AudioDataReceivedListener listener) {
        this.context = context;
        mListener = listener;
        Log.d(TAG, String.format("call RecordingThread(): isBluetoothHeadsetConnected() = [%s], listener = [%s]", isBluetoothHeadsetConnected(), listener));
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;
    private AudioManager mAudioManager = null;

    public boolean recording() {
        return mThread != null;
    }

    public void startRecording() {
        Log.d(TAG, String.format("call startRecording():%s, %s %s", isBluetoothHeadsetConnected(), mAudioManager.isBluetoothScoOn(), mAudioManager.isBluetoothScoAvailableOffCall()));
        Log.d(TAG, String.format("call getMode():%s", getMode()));

        if (!isBluetoothHeadsetConnected()) {
            start();
        } else {
            context.registerReceiver(bluetoothReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            mAudioManager.startBluetoothSco();
        }
        Log.d(TAG, String.format("call getMode():%s", getMode()));
    }

    private String getMode() {
        String mode;
        switch (mAudioManager.getMode()) {
            case MODE_NORMAL:
                mode = "MODE_NORMAL";
                break;
            case MODE_RINGTONE:
                mode = "MODE_RINGTONE";
                break;
            case MODE_IN_CALL:
                mode = "MODE_IN_CALL";
                break;
            case MODE_IN_COMMUNICATION:
                mode = "MODE_IN_COMMUNICATION";
                break;
            default:
                mode = "unknown";
                break;
        }
        return mode + " " + mAudioManager.isBluetoothScoOn();
    }

    private void start() {
        if (mThread != null)
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        mThread.start();
    }

    public void stopRecording() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;

        Log.d(TAG, String.format("call getMode():%s", getMode()));
        if (mAudioManager != null && mAudioManager.isBluetoothScoOn()) {
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();

            context.unregisterReceiver(bluetoothReceiver);  //别遗漏
        }
        Log.d(TAG, String.format("call getMode():%s", getMode()));
    }

    private void record() {
        Log.v(TAG, "Start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(TAG, "Start recording");

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            // 如果读取音频数据没有出现错误，就将数据写入到文件
            if (AudioRecord.ERROR_INVALID_OPERATION != numberOfShort) {
                shortsRead += numberOfShort;

                // Notify waveform
                mListener.onAudioDataReceived(audioBuffer);
            }
        }

        mListener.onAudioDataEnd();
        record.stop();
        record.release();

        Log.v(TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }
}
