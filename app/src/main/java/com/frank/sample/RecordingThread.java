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
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

public class RecordingThread {
    private static final String TAG = "RecordingThread";

    private static final int SAMPLE_RATE = 44100;
    private Context context;
    private AudioManager mAudioManager = null;
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            Log.d(TAG, String.format("call onReceive(): state=%s, intent=%s", state, intent));
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                mAudioManager.setBluetoothScoOn(true);  //打开SCO
                mAudioManager.setSpeakerphoneOn(false);
                Log.d(TAG, String.format("call onReceive(): %s, %s, %s %s", recording(), isBluetoothHeadsetConnected(), mAudioManager.isBluetoothScoOn(), mAudioManager.isBluetoothScoAvailableOffCall()));
                start();//开始录音
//                RecordingThread.this.context.unregisterReceiver(this);
            } else if (AudioManager.SCO_AUDIO_STATE_CONNECTING == state) {
                Log.d(TAG, "SCO_AUDIO_STATE_CONNECTING");
            } else {
                if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state && recording()) {
                    stopRecording();
                } else {
                    //等待后再尝试启动SCO
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAudioManager.startBluetoothSco();
                }
            }
        }
    };
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;

    public RecordingThread(@NonNull Context context, @NonNull IAudioDataReceivedListener listener) {
        mListener = listener;
        this.context = context.getApplicationContext();
        mAudioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                Log.d(TAG, String.format("call onAudioFocusChange(): focusChange = [%s]", focusChange));
            }
        };
    }

    private boolean mShouldContinue;
    private IAudioDataReceivedListener mListener;
    private Thread mThread;

    public boolean recording() {
        return mThread != null;
    }

    private boolean isUseAEC, isUseNS, isUseAGC;

    public void startRecording(boolean isUseAEC, boolean isUseNS, boolean isUseAGC) {
        this.isUseAEC = isUseAEC;
        this.isUseNS = isUseNS;
        this.isUseAGC = isUseAGC;
        startRecording();
    }

    public void startRecording() {
//        startRecording2();
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        Log.d(TAG, String.format("call startRecording():%s, %s %s", isBluetoothHeadsetConnected(), mAudioManager.isBluetoothScoOn(), mAudioManager.isBluetoothScoAvailableOffCall()));
        if (!isBluetoothHeadsetConnected()) {
            start();
        } else {
//            mAudioManager.stopBluetoothSco();
            mAudioManager.startBluetoothSco();
            context.registerReceiver(bluetoothReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
        }
    }

    public void stopRecording() {
//        stopRecording2();
        Log.d(TAG, String.format("call stopRecording():%s, %s %s %s", isBluetoothHeadsetConnected(), mAudioManager.isBluetoothScoOn(), mAudioManager.isBluetoothScoAvailableOffCall(), mAudioManager.isBluetoothA2dpOn()));
        stop();
        if (isBluetoothHeadsetConnected()) {
            if (mAudioManager.isBluetoothScoOn()) {
                mAudioManager.setBluetoothScoOn(false);
                mAudioManager.stopBluetoothSco();
                mAudioManager.setSpeakerphoneOn(false);
            } else {
                mAudioManager.setSpeakerphoneOn(true);
            }
            RecordingThread.this.context.unregisterReceiver(bluetoothReceiver);
        }
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
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

    private void stop() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private volatile long start;

    private void record() {
        Log.v(TAG, "call record():");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            return;
        }
        if (isUseAEC)
            initAEC(record.getAudioSessionId());
        if (isUseNS)
            initNS(record.getAudioSessionId());
        if (isUseAGC)
            initAGC(record.getAudioSessionId());
        start = SystemClock.elapsedRealtime();
        record.startRecording();

        Log.d(TAG, String.format("call record(): Start recording"));
        mListener.onAudioDataStart();

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            // 如果读取音频数据没有出现错误，就将数据写入到文件
            if (AudioRecord.ERROR_INVALID_OPERATION != numberOfShort) {
                shortsRead += numberOfShort;

                // Notify waveform
                mListener.onAudioDataReceived(audioBuffer, SystemClock.elapsedRealtime() - start);
            }
        }

        mListener.onAudioDataEnd(SystemClock.elapsedRealtime() - start);
        record.stop();
        releaseAEC();
        releaseNS();
        releaseAGC();
        record.release();

        Log.d(TAG, String.format("call record(): Recording stopped. Samples read: %d", shortsRead));
    }

    private static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    private AcousticEchoCanceler canceler;

    private static boolean isSupportAEC() {
        if (AcousticEchoCanceler.isAvailable()) {
            return true;
        } else {
            Log.d(TAG, String.format("call isSupportAEC(): false"));
            return false;
        }
    }

    private boolean initAEC(int audioSession) {
        if (canceler != null || !isSupportAEC()) {
            return false;
        }
        canceler = AcousticEchoCanceler.create(audioSession);
        canceler.setEnabled(true);
        return canceler.getEnabled();
    }

    public boolean setAECEnabled(boolean enable) {
        if (null == canceler || !isSupportAEC()) {
            return false;
        }
        canceler.setEnabled(enable);
        return canceler.getEnabled();
    }

    private boolean releaseAEC() {
        if (null == canceler || !isSupportAEC()) {
            return false;
        }
        canceler.setEnabled(false);
        canceler.release();
        canceler = null;
        return true;
    }

    private NoiseSuppressor noiseSuppressor;

    private boolean isSupportNS() {
        if (NoiseSuppressor.isAvailable()) {
            return true;
        } else {
            Log.d(TAG, String.format("call isSupportNS(): false"));
            return false;
        }
    }

    private boolean initNS(int audioSession) {
        if (!isSupportNS() || noiseSuppressor != null) {
            return false;
        }
        noiseSuppressor = NoiseSuppressor.create(audioSession);
        noiseSuppressor.setEnabled(true);
        return noiseSuppressor.getEnabled();
    }

    public boolean setNSEnable(boolean enable) {
        if (!isSupportNS() || noiseSuppressor == null) {
            return false;
        }
        noiseSuppressor.setEnabled(enable);
        return noiseSuppressor.getEnabled();
    }

    private boolean releaseNS() {
        if (!isSupportNS() || noiseSuppressor == null) {
            return false;
        }
        noiseSuppressor.setEnabled(false);
        noiseSuppressor.release();
        noiseSuppressor = null;
        return true;
    }

    private AutomaticGainControl gainControl;

    private boolean isSupportAGC() {
        if (AutomaticGainControl.isAvailable()) {
            return true;
        } else {
            Log.d(TAG, String.format("call isSupportAGC(): false"));
            return false;
        }
    }

    private boolean initAGC(int audioSession) {
        if (!isSupportAGC() || gainControl != null) {
            return false;
        }
        gainControl = AutomaticGainControl.create(audioSession);
        gainControl.setEnabled(true);
        return gainControl.getEnabled();
    }

    private boolean setAGCEnable(boolean enable) {
        if (!isSupportAGC() || gainControl == null) {
            return false;
        }
        gainControl.setEnabled(enable);
        return gainControl.getEnabled();
    }

    private boolean releaseAGC() {
        if (!isSupportAGC() || gainControl == null) {
            return false;
        }
        gainControl.setEnabled(false);
        gainControl.release();
        gainControl = null;
        return true;
    }

    private void startRecording2() {
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
            Log.i(TAG, "系统不支持蓝牙录音");
            return;
        }
        Log.i(TAG, "系统支持蓝牙录音");
        mAudioManager.stopBluetoothSco();
        mAudioManager.startBluetoothSco();//蓝牙录音的关键，启动SCO连接，耳机话筒才起作用

        RecordingThread.this.context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                Log.d(TAG, String.format("call onReceive(): state=%s, intent=%s", state, intent));

                if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                    Log.i(TAG, "AudioManager.SCO_AUDIO_STATE_CONNECTED");
                    mAudioManager.setBluetoothScoOn(true);  //打开SCO
                    Log.i(TAG, "Routing:" + mAudioManager.isBluetoothScoOn());
                    mAudioManager.setMode(AudioManager.STREAM_MUSIC);
                    Log.d(TAG, "启动录音");
                    RecordingThread.this.context.unregisterReceiver(this);  //别遗漏
                } else if (AudioManager.SCO_AUDIO_STATE_CONNECTING == state) {
                    Log.d(TAG, "SCO_AUDIO_STATE_CONNECTING");
                } else {//等待一秒后再尝试启动SCO
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAudioManager.startBluetoothSco();
                    Log.i(TAG, "再次startBluetoothSco()");
                }

				/*Log.i(TAG, "AudioManager.SCO_AUDIO_STATE_CONNECTED");
				mAudioManager.setBluetoothScoOn(true);  //打开SCO
				Log.i(TAG, "Routing:" + mAudioManager.isBluetoothScoOn());
				mAudioManager.setMode(AudioManager.STREAM_MUSIC);
				mRecorder.start();//开始录音
				Log.d(TAG,"启动录音");
				unregisterReceiver(this);  //别遗漏*/
            }
//		}, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));
        }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
    }

    private void stopRecording2() {
        //mAudioManager.stopBluetoothSco();
        if (mAudioManager.isBluetoothScoOn()) {
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
        }
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
    }
}
