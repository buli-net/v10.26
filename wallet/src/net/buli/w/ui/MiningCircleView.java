package net.buli.w.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import androidx.core.graphics.ColorUtils;

public class MiningCircleView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress = 0f;
    private boolean isLate = false;
    private RectF rect = new RectF();

    public MiningCircleView(Context ctx) {
        super(ctx);
    }

    public void setProgress(float p, boolean late) {
        this.progress = Math.max(0, Math.min(1.5f, p));
        this.isLate = late;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        rect.set(0, 0, size, size);

        // nền mờ
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x22000000);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // màu theo tiến độ
        int color;
        if (isLate) {
            color = 0xFF9E9E9E;
        } else if (progress <= 0.5f) {
            color = ColorUtils.blendARGB(0xFF4CAF50, 0xFFFFC107, progress * 2f);
        } else {
            float t = (progress - 0.5f) * 2f;
            color = ColorUtils.blendARGB(0xFFFFC107, 0xFFF44336, t);
        }
        paint.setColor(color);

        // VẼ ĐẶC RUỘT - true = pie
        float sweep = 360f * Math.min(progress, 1f);
        if (sweep > 0) {
            canvas.drawArc(rect, -90, sweep, true, paint);
        }
    }
}