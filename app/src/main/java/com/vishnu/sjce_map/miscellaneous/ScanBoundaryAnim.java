package com.vishnu.sjce_map.miscellaneous;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.vishnu.sjce_map.R;

public class ScanBoundaryAnim extends View {
    private final float scaleFactor = 1.0f;
    private final float strokeWidth = 25;

    public ScanBoundaryAnim(Context context) {
        super(context);
        init();
    }

    public ScanBoundaryAnim(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanBoundaryAnim(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Paint paint = new Paint();
        paint.setARGB(255, 0, 0, 0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Calculate the width and height after scaling
        int scaledWidth = (int) (getWidth() * scaleFactor);
        int scaledHeight = (int) (getHeight() * scaleFactor);

        // Draw L-shaped lines at each corner
        drawCornerLines(canvas, centerX - (float) scaledWidth / 2, centerY - (float) scaledHeight / 2);
        drawCornerLines(canvas, centerX + (float) scaledWidth / 2, centerY - (float) scaledHeight / 2);
        drawCornerLines(canvas, centerX - (float) scaledWidth / 2, centerY + (float) scaledHeight / 2);
        drawCornerLines(canvas, centerX + (float) scaledWidth / 2, centerY + (float) scaledHeight / 2);
    }

    private void drawCornerLines(Canvas canvas, float x, float y) {
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.scan_corner_box));
        paint.setStrokeWidth(6);

        // Drawing lines for each corner
        // Drawing a vertical line on the Y-axis
        canvas.drawLine(x, 0, x, (float) canvas.getHeight(), paint);
        // Drawing a horizontal line on the X-axis
        canvas.drawLine(0, y, (float) canvas.getWidth(), y, paint);
    }
}
