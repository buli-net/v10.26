package net.buli.w.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class MiningCircleView extends View {
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress = 0f;
    private boolean isLate = false;

    public MiningCircleView(Context context) {
        super(context);
        init();
    }

    public MiningCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(6f);
        bgPaint.setColor(0x33FFFFFF);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(6f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float p, boolean late) {
        this.progress = Math.max(0f, Math.min(1f, p));
        this.isLate = late;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        float pad = 8f;
        RectF oval = new RectF(pad, pad, size - pad, size - pad);
        canvas.drawArc(oval, 0, 360, false, bgPaint);
        int color;
        if (isLate) color = 0xFF888888;
        else if (progress < 0.33f) color = 0xFF4CAF50;
        else if (progress < 0.66f) color = 0xFFFFC107;
        else color = 0xFFF44336;
        progressPaint.setColor(color);
        canvas.drawArc(oval, -90, 360 * progress, false, progressPaint);
        // No text drawn inside circle
    }
}
