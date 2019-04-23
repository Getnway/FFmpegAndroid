package com.frank.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Getnway
 * @Email Getnway@gmail.com
 * @since 2019-03-06
 */
public class WaveView extends View {
    private float waveThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
    private float waveInterval = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
    private float waveMaxHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
    private float waveMinHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
    private int colorNotSound = Color.parseColor("#E6E6E6");
    private int colorSound = Color.parseColor("#50C878");
    private Paint wavePaint;
    private List<Short> data = new ArrayList<>();
    private float rate;

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
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        //rate = 48;//-64 * density + 72 * (width / 360);
        float z = 119262F / 91321;
        float y = -7016F / 91321;
        float x = -11198992F / 91321;
        this.rate = x * metrics.density + y * metrics.heightPixels + z * metrics.ydpi;
//        Log.d(getClass().getSimpleName(), String.format("call init():%s %s", this.rate, getResources().getDisplayMetrics().toString()));
        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setData(@NonNull List<Short> data) {
        if (data == null) return;
        this.data = data;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int count = getWidth() / (int) (waveInterval + waveThickness);
//        Log.d(getClass().getSimpleName(), String.format("call onDraw(): size=%s count=%s width=%s height=%s", data == null ? -1 : data.size(), count, getWidth(), getHeight()));

        float waveLeft = 0, waveHeight;
        int offset = data.size() > count ? data.size() - count : 0;

        // 没声音部分
        wavePaint.setColor(colorNotSound);
//        Ln.d("call onDraw(): not sound start=%s/%s", i, count);
        for (int i = 0; i < count - data.size(); ++i) {
            canvas.drawRect(waveLeft, (getHeight() / 2.0f) - (waveMinHeight / 2), waveLeft + waveThickness, (getHeight() / 2.0f) + (waveMinHeight / 2), wavePaint);
            waveLeft += (waveInterval + waveThickness);
        }

        // 有声音部分
        wavePaint.setColor(colorSound);
//        Ln.d("call onDraw(): sound.size=%s/%s", data.size() - offset, count);
        for (int j = 0; j < data.size() - offset; ++j) {
            waveHeight = Math.abs(data.get(j + offset) / rate);
            waveHeight = waveHeight > (waveMaxHeight / 2) ? (waveMaxHeight / 2) : waveHeight;
            waveHeight = waveHeight < (waveMinHeight / 2) ? (waveMinHeight / 2) : waveHeight;
            canvas.drawRect(waveLeft, (getHeight() / 2.0f) - waveHeight, waveLeft + waveThickness, (getHeight() / 2.0f) + waveHeight, wavePaint);
            waveLeft += (waveInterval + waveThickness);
        }
    }
}
