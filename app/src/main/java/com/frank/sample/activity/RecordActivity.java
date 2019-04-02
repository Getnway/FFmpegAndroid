package com.frank.sample.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.baidu.aip.asrwakeup3.core.recog.RecogResult;
import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;
import com.frank.sample.AudioDataReceived;
import com.frank.ffmpeg.BuildConfig;
import com.frank.ffmpeg.FFmpegCmd;
import com.frank.sample.R;
import com.frank.sample.Recognizer;
import com.frank.sample.RecordingThread;
import com.frank.sample.WaveView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Getnway
 * @Email Getnway@gmail.com
 * @since 2019-02-26
 */
public class RecordActivity extends AppCompatActivity implements View.OnClickListener, EventListener {
    private static final String TAG = "RecordActivity";
    private String fileName = "";
    private String transformFile = "";
    private MediaRecorder recorder;
    private Recognizer recognizer;
    private RecordingThread recordingThread;
    private AudioDataReceived audioDataReceived;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private boolean hasRecordPermission = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 567;

    private Button btnRecord;
    private Button btnTransform;
    private Button btnRecognize;
    private Button btnRecordStream;
    private Button btnTransformM4A;
    private Button btnTransformPCM;
    private Button btnRecognizeStream;
    private WaveView waveView;
    private TextView tvLog;
    private ScrollView scrollView;
    private long start;
    private List<Short> dataCache = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        btnRecord = findViewById(R.id.btn_record);
        btnTransform = findViewById(R.id.btn_transform);
        btnRecognize = findViewById(R.id.btn_recognize);
        btnRecordStream = findViewById(R.id.btn_record_stream);
        btnTransformM4A = findViewById(R.id.btn_transform_stream_m4a);
        btnTransformPCM = findViewById(R.id.btn_transform_stream_16pcm);
        btnRecognizeStream = findViewById(R.id.btn_recognize_stream);
        waveView = findViewById(R.id.wave_view);
        scrollView = findViewById(R.id.scroll_view);
        tvLog = findViewById(R.id.tv_log);

        btnRecord.setOnClickListener(this);
        btnTransform.setOnClickListener(this);
        btnRecognize.setOnClickListener(this);
        btnRecordStream.setOnClickListener(this);
        btnTransformM4A.setOnClickListener(this);
        btnTransformPCM.setOnClickListener(this);
        btnRecognizeStream.setOnClickListener(this);

        recognizer = new Recognizer();
        recognizer.init(this, this);

        audioDataReceived = new AudioDataReceived() {

            @Override
            public void onAudioDataReceived(short[] data) {
                if (data != null) {
                    for (int i = 0; i < data.length - 300; i = i + 400) {
                        dataCache.add((short) ((data[i] + data[i + 100] + data[i + 200] + data[i + 300]) / 4));
                    }
                    waveView.setData(dataCache);
                }
                super.onAudioDataReceived(data);
            }
        };
        recordingThread = new RecordingThread(this, audioDataReceived);
    }

    @Override
    protected void onPause() {
        recognizer.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        recognizer.destroy(this);
        super.onDestroy();
    }

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String logTxt = "name: " + name;

        if (params != null && !params.isEmpty()) {
            logTxt += " ;params :" + params;
        }
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            if (params != null && params.contains("\"nlu_result\"")) {
                if (length > 0 && data.length > 0) {
                    logTxt += ", 语义解析结果：" + new String(data, offset, length);
                }
            }
        } else if (data != null) {
            logTxt += " ;data length=" + data.length;
        }
        Log.d(TAG, String.format("call onEvent():%s", logTxt));
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)) {
            // 引擎准备就绪，可以开始说话
            appendLog("引擎准备就绪，可以开始说话");
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)) {
            // 检测到用户的已经开始说话
            appendLog("检测到用户的已经开始说话");
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)) {
            // 检测到用户的已经停止说话
            appendLog("检测到用户的已经停止说话");
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            RecogResult recogResult = RecogResult.parseJson(params);
            // 临时识别结果, 长语音模式需要从此消息中取出结果
            String[] results = recogResult.getResultsRecognition();
            if (recogResult.isFinalResult()) {
                appendLog(results[0]);
            } else if (recogResult.isPartialResult()) {
                appendLog(results[0]);
            } else if (recogResult.isNluResult()) {
                appendLog(", 语义解析结果：" + new String(data, offset, length));
            }

        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)) {
            // 识别结束， 最终识别结果或可能的错误
            RecogResult recogResult = RecogResult.parseJson(params);
            if (recogResult.hasError()) {
                int errorCode = recogResult.getError();
                int subErrorCode = recogResult.getSubError();
                Log.e(TAG, "asr error:" + params);
                String error = String.format("errorCode=%s, subErrorCode=%s, recogResult.getDesc()=%s, recogResult=%s", errorCode, subErrorCode, recogResult.getDesc(), recogResult);
                appendLog(error);
            } else {
                appendLog("识别结束，耗时：" + (System.currentTimeMillis() - start) + "\n");
            }
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_EXIT)) {
            appendLog("识别退出");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record:
                record(btnRecord);
                break;
            case R.id.btn_transform:
                transformFile = transform(fileName);
                break;
            case R.id.btn_recognize:
                recognize(btnRecognize, transformFile);
                break;
            case R.id.btn_record_stream:
                record(btnRecordStream);
                break;
            case R.id.btn_transform_stream_m4a:
                pcm2m4a(fileName);
                break;
            case R.id.btn_transform_stream_16pcm:
                transformFile = pcm2pcmLow(fileName);
                break;
            case R.id.btn_recognize_stream:
                recognize(btnRecordStream, transformFile);
        }
    }

    private void record(Button btn) {
        dataCache.clear();
        if (btn.isSelected()) {
            if (btn == btnRecord) {
                stopRecording();
            } else if (btn == btnRecordStream) {
                recordingThread.stopRecording();
            }
            appendLog("结束录制，耗时：" + (System.currentTimeMillis() - start) + "\n\n");
            btn.setText("录制");
            btn.setSelected(false);
        } else {
            if (btn == btnRecord) {
                startRecording();
            } else if (btn == btnRecordStream) {
                fileName = getExternalCacheDir().getAbsolutePath() + File.separatorChar + "RecordTest" + System.currentTimeMillis() + ".pcm";
                audioDataReceived.setFile(fileName);
                recordingThread.startRecording();
            }
            start = System.currentTimeMillis();
            tvLog.setText("");
            appendLog("开始录制");
            btn.setText("结束");
            btn.setSelected(true);
        }
    }

    private String transform(String inputFile) {
        String transformFile = inputFile.split("\\.m4a")[0] + "Tran.pcm";
        String msg = String.format("call transformFile(): %s ===> %s", inputFile, transformFile);
        Log.d(TAG, msg);
        appendLog(msg);
        String transformAudioCmd = "ffmpeg -i %s -acodec pcm_s16le -f s16le -ac 1 -ar 16000 %s";
        transformAudioCmd = String.format(transformAudioCmd, inputFile, transformFile);
        executeFFmpegCmd(transformAudioCmd);
        return transformFile;
    }

    private String pcm2m4a(String inputFile) {
        String transformFile = inputFile.split("\\.pcm")[0] + "Tran.m4a";
        String transformAudioCmd = "ffmpeg -ac 1 -ar 44100 -f s16le -i %s -ac 1 -ar 44100 -ab 128000 %s";
        transformAudioCmd = String.format(transformAudioCmd, inputFile, transformFile);
        executeFFmpegCmd(transformAudioCmd);
        return transformFile;
    }

    private String pcm2pcmLow(String inputFile) {
        String transformFile = inputFile.split("\\.pcm")[0] + "Tran16.pcm";
        String transformAudioCmd = "ffmpeg -ac 1 -ar 44100 -f s16le -i %s -ac 1 -ar 16000 -f s16le %s";
        transformAudioCmd = String.format(transformAudioCmd, inputFile, transformFile);
        executeFFmpegCmd(transformAudioCmd);
        return transformFile;
    }


    private void recognize(Button btn, String transformFile) {
        start = System.currentTimeMillis();
        if (btn.isSelected()) {
            recognizer.stop();
            btn.setSelected(false);
            btn.setText("识别");
        } else {
            recognizer.start(this, transformFile);
            btn.setSelected(true);
            btn.setText("停止");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        hasRecordPermission = requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (!hasRecordPermission) finish();
    }

    private void appendLog(String log) {
        tvLog.setText(tvLog.getText() + "\n" + log);
        scrollView.smoothScrollTo(0, tvLog.getBottom());
    }

    private void startRecording() {
        fileName = getExternalCacheDir().getAbsolutePath() + File.separatorChar + "RecordTest" + System.currentTimeMillis() + ".m4a";
        Log.d(TAG, String.format("call startRecording(): %s", fileName));
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioChannels(1);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "startRecording(): execution occurs error:" + e);
        }
        recorder.start();
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (Throwable t) {
                Log.e(TAG, "stopRecording(): execution occurs error:" + t);
            }
            recorder = null;
        }
    }

    private final static int MSG_BEGIN = 11;
    private final static int MSG_FINISH = 12;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_BEGIN:
                    start = System.currentTimeMillis();
                    appendLog("开始转码");
                    break;
                case MSG_FINISH:
                    appendLog("完成转码，耗时：" + (System.currentTimeMillis() - start) + "\n\n");
                    break;
                default:
                    break;
            }
        }
    };


    private void executeFFmpegCmd(final String transformAudioCmd) {
        Log.d(TAG, String.format("call executeFFmpegCmd(): transformAudioCmd = [%s]", transformAudioCmd));
        String[] commandLine = transformAudioCmd.split(" ");//以空格分割为字符串数组
        executeFFmpegCmd(commandLine);
    }

    /**
     * 执行ffmpeg命令行
     *
     * @param commandLine commandLine
     */
    private void executeFFmpegCmd(final String[] commandLine) {
        if (commandLine == null) {
            return;
        }
        String[] commands;
        if (BuildConfig.DEBUG) {
            commands = new String[commandLine.length + 1];
            commands[0] = commandLine[0];
            commands[1] = "-d";
            System.arraycopy(commandLine, 1, commands, 2, commandLine.length - 1);
        } else {
            commands = commandLine.clone();
        }
        Log.d(TAG, String.format("call executeFFmpegCmd(): commandLine = [%s]", Arrays.toString(commands)));
        FFmpegCmd.execute(commands, new FFmpegCmd.OnHandleListener() {
            @Override
            public void onBegin() {
                Log.i(TAG, "handle audio onBegin...");
                mHandler.obtainMessage(MSG_BEGIN).sendToTarget();
            }

            @Override
            public void onEnd(int result) {
                Log.i(TAG, String.format("handle audio onEnd...result=%s", result));
                mHandler.obtainMessage(MSG_FINISH).sendToTarget();
            }
        });
    }
}
