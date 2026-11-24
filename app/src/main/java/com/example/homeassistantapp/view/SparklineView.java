package com.example.homeassistantapp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.color.MaterialColors;

import java.util.List;

/** 홈 타일의 작은 추이선. 축·글자·터치 없이 선만 그린다(차트 라이브러리보다 가볍다). */
public class SparklineView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private TrendValues trend;

    public SparklineView(Context context) {
        super(context);
        init();
    }

    public SparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        int color = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2f * density);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(color);
        dot.setColor(color);
    }

    public void setTrend(TrendValues trend) {
        this.trend = trend;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (trend == null) return;
        // 선 두께만큼 안쪽에 그려 위·아래 끝이 잘리지 않게 한다.
        float inset = line.getStrokeWidth();
        float width = getWidth() - getPaddingLeft() - getPaddingRight() - 2 * inset;
        float height = getHeight() - getPaddingTop() - getPaddingBottom() - 2 * inset;
        List<float[]> pieces = trend.polylines(width, height);
        canvas.save();
        canvas.translate(getPaddingLeft() + inset, getPaddingTop() + inset);
        for (float[] points : pieces) {
            if (points.length == 2) {
                canvas.drawCircle(points[0], points[1], line.getStrokeWidth(), dot);
                continue;
            }
            path.reset();
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
            canvas.drawPath(path, line);
        }
        canvas.restore();
    }
}
