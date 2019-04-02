package com.frank.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.List;

/**
 * @author Getnway
 * @Email Getnway@gmail.com
 * @since 2019-03-06
 */
public class WaveView extends View {
    private static final String TAG = "WaveView";
    private Paint linePaint;
    private List<Short> data;
    private float density;

    public WaveView(Context context) {
        this(context, null);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Log.d(TAG, String.format("call init():%s %s", getResources().getDisplayMetrics().density, getResources().getDisplayMetrics().densityDpi));
        density = getResources().getDisplayMetrics().density;
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.GREEN);
    }

    public void setData(List<Short> data) {
        this.data = data;
        postInvalidate();
    }

    private static final int INTERVAL = 4;
    private static final int RATE = 48;

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, String.format("call onDraw(): size=%s width=%s height=%s", data == null ? -1 : data.size(), getWidth(), getHeight()));
        canvas.drawLine(0, getHeight() * 0.5f, getWidth(), getHeight() * 0.5f, linePaint);
        if (data != null && data.size() > 0) {
            int count = getWidth() / INTERVAL;

            float x, yStart, yEnd;
            int offset = data.size() > count ? data.size() - count : 0;
            for (int i = 0; i < data.size() - offset; ++i) {
                x = i * INTERVAL;
                if (x > getWidth()) {
                    x = getWidth();
                }
                yStart = getHeight() / 2 + data.get(i + offset) / (RATE/density);
                yEnd = getHeight() / 2 - data.get(i + offset) / (RATE/density);
//                Log.d(TAG, String.format("call onDraw(): x=%s \tyStart=%s \tyEnd=%s", x, yStart, yEnd));
                canvas.drawLine(x, yStart, x, yEnd, linePaint);
            }
        }
    }
}
