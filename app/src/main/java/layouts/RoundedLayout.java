package layouts;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tajos.iccnotes.R;

public class RoundedLayout extends FrameLayout {

    private float topLeftCornerRadius;
    private float topRightCornerRadius;
    private float bottomLeftCornerRadius;
    private float bottomRightCornerRadius;
    private float shadowRadius;
    private float borderRadius = 0;

    private final int shadowColor = Color.parseColor("#656565"); // somewhat gray
    private int borderColor;
    private int bgColor;

    private final Paint shadowPaint = new Paint();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RoundedLayout(@NonNull Context context) {
        super(context);
        init(context, null, 0);
    }

    public RoundedLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RoundedLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RoundedLayout, defStyleAttr, 0);

        //get the default value form the attrs
        shadowRadius = typedArray.getDimension(R.styleable.RoundedLayout_shadowRadius, 0);
        borderColor = typedArray.getColor(R.styleable.RoundedLayout_borderColor, 0);
        bgColor = typedArray.getColor(R.styleable.RoundedLayout_bgColor, 0);
        final float cornerRadius = typedArray.getDimension(R.styleable.RoundedLayout_cornerRadius, 0);

        if (cornerRadius == 0) {
            topLeftCornerRadius = typedArray.getDimension(R.styleable.RoundedLayout_topLeftCornerRadius, 0);
            topRightCornerRadius = typedArray.getDimension(R.styleable.RoundedLayout_topRightCornerRadius, 0);
            bottomLeftCornerRadius = typedArray.getDimension(R.styleable.RoundedLayout_bottomLeftCornerRadius, 0);
            bottomRightCornerRadius = typedArray.getDimension(R.styleable.RoundedLayout_bottomRightCornerRadius, 0);
        } else {
            topLeftCornerRadius = cornerRadius;
            topRightCornerRadius = cornerRadius;
            bottomLeftCornerRadius = cornerRadius;
            bottomRightCornerRadius = cornerRadius;
        }
        typedArray.recycle();

        if (borderColor!=0)
            borderRadius = 5f;

        bgPaint.setColor(bgColor);
        shadowPaint.setShadowLayer(shadowRadius, 0, shadowRadius/3, shadowColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);
        borderPaint.setStrokeWidth(borderRadius);
        borderPaint.setColor(borderColor);

        if (shadowRadius > 0) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            setPadding(getPaddingLeft() + (int)shadowRadius, getPaddingTop() + (int)shadowRadius,
                    getPaddingRight() + (int)shadowRadius, getPaddingBottom() + (int)shadowRadius);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        int count = canvas.save();

        final Path path = new Path();

        float[] cornerDimensions = {
                topLeftCornerRadius, topLeftCornerRadius,
                topRightCornerRadius, topRightCornerRadius,
                bottomRightCornerRadius, bottomRightCornerRadius,
                bottomLeftCornerRadius, bottomLeftCornerRadius};

        path.addRoundRect(new RectF(shadowRadius+borderRadius, shadowRadius+borderRadius, canvas.getWidth()-(shadowRadius+borderRadius), canvas.getHeight()-(shadowRadius+borderRadius))
                , cornerDimensions, Path.Direction.CW);

        if (shadowRadius != 0)
            canvas.drawPath(path, shadowPaint); // draws shadow

        if (bgColor != 0)
            canvas.drawPath(path, bgPaint); // draws background

        if (borderColor != 0)
            canvas.drawPath(path, borderPaint); // draws border

        canvas.clipPath(path);

        super.dispatchDraw(canvas);
        canvas.restoreToCount(count);
    }

    public void setShadowRadius(float shadowRadius) {
        this.shadowRadius = shadowRadius;
        invalidate();
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        this.borderPaint.setColor(borderColor);
        invalidate();
    }

    public void setBgroundColor(int bgColor) {
        this.bgColor = bgColor;
        this.bgPaint.setColor(bgColor);
        invalidate();
    }

    public void setTopLeftCornerRadius(float topLeftCornerRadius) {
        this.topLeftCornerRadius = topLeftCornerRadius;
        invalidate();
    }

    public void setTopRightCornerRadius(float topRightCornerRadius) {
        this.topRightCornerRadius = topRightCornerRadius;
        invalidate();
    }

    public void setBottomLeftCornerRadius(float bottomLeftCornerRadius) {
        this.bottomLeftCornerRadius = bottomLeftCornerRadius;
        invalidate();
    }

    public void setBottomRightCornerRadius(float bottomRightCornerRadius) {
        this.bottomRightCornerRadius = bottomRightCornerRadius;
        invalidate();
    }
}

