package com.frank.ffmpeg.activity;

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
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.baidu.aip.asrwakeup3.core.recog.RecogResult;
import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;
import com.frank.ffmpeg.BuildConfig;
import com.frank.ffmpeg.FFmpegCmd;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.Recognizer;
import com.frank.ffmpeg.util.FFmpegUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private boolean hasRecordPermission = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 567;

    private Button btnRecord;
    private Button btnTransform;
    private Button btnRecognize;
    private TextView tvLog;
    private ScrollView scrollView;
    private long start;
    private static final String FILE_FORMAT = "m4a";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        btnRecord = findViewById(R.id.btn_record);
        btnTransform = findViewById(R.id.btn_transform);
        btnRecognize = findViewById(R.id.btn_recognize);
        scrollView = findViewById(R.id.scroll_view);
        tvLog = findViewById(R.id.tv_log);

        btnRecord.setOnClickListener(this);
        btnTransform.setOnClickListener(this);
        btnRecognize.setOnClickListener(this);

        recognizer = new Recognizer();
        recognizer.init(this, this);
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
                record();
                break;
            case R.id.btn_transform:
                transform();
                break;
            case R.id.btn_recognize:
                recognize();
                break;
        }
    }

    private void record() {
        if (btnRecord.isSelected()) {
            stopRecording();
            appendLog("结束录制，耗时：" + (System.currentTimeMillis() - start) + "\n\n");
            btnRecord.setText("录制");
            btnRecord.setSelected(false);
        } else {
            startRecording();
            start = System.currentTimeMillis();
            tvLog.setText("");
            appendLog("开始录制");
            btnRecord.setText("结束");
            btnRecord.setSelected(true);
        }
    }

    private void transform() {
        transformFile = fileName.split("\\." + FILE_FORMAT)[0] + "Tran.pcm";
        String msg = String.format("call transformFile(): %s ===> %s", fileName, transformFile);
        Log.d(TAG, msg);
        appendLog(msg);
        String[] commandLine = FFmpegUtil.transformAudio(fileName, transformFile);
        executeFFmpegCmd(commandLine);
    }

    private void recognize() {
        start = System.currentTimeMillis();
        recognizer.start(this, transformFile);
//        if (btnRecognize.isSelected()) {
//            recognizer.stop();
//            btnRecognize.setSelected(false);
//            btnRecognize.setText("识别");
//        } else {
//            recognizer.start(this, transformFile);
//            btnRecognize.setSelected(true);
//            btnRecognize.setText("停止");
//        }
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
        fileName = getExternalCacheDir().getAbsolutePath() + File.separatorChar + "RecordTest" + System.currentTimeMillis() + "." + FILE_FORMAT;
        Log.d(TAG, String.format("call startRecording(): %s", fileName));
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(192 * 1000);
        recorder.setAudioChannels(2);

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
