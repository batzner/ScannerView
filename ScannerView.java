import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ScannerView extends View {

    // Styleable attributes:
    private float mFrameWidthPercentage;
    private float mFrameHeightPercentage;
    private float mFrameAspectRatio;
    private float mMovingLineWidth;
    private float mFrameLineWidth;
    private float mOrthogonalFrameLineLength;
    private int mPaddingColor;
    private int mMovingLineColor;
    private int mFrameColor;
    private float mUpAndDownSeconds;

    // Objects for drawing
    private Paint mFramePaint;
    private Paint mMovingLinePaint;
    private Paint mPaddingPaint;

    // Calculated properties depending on the size and state of the view
    private int mFrameWidth;
    private int mFrameHeight;
    private int mPaddingWidth;
    private int mPaddingHeight;
    private float mMovingLineProgress;
    private boolean mLineMovingDown = true;
    private boolean mAnimated = true;

    public ScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get the attributes with styles applied and resource references resolved
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ScannerView,
                0, 0);

        // Calculate the pixel-lengths of default values
        Resources r = context.getResources();
        float defaultMovingLineWidth = TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics());
        float defaultFrameLineWidth = TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, r.getDisplayMetrics());
        float defaultOrthogonalFrameLineLength = TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());

        try {
            // Parse the attributes
            mFrameWidthPercentage = a.getFloat(R.styleable.ScannerView_frameWidthPercentage,
                    0.75f);
            mFrameHeightPercentage = a.getFloat(R.styleable.ScannerView_frameHeightPercentage,
                    0.75f);
            mFrameAspectRatio = a.getFloat(R.styleable.ScannerView_frameAspectRatio,
                    1);

            mMovingLineWidth = a.getDimension(R.styleable.ScannerView_movingLineWidth,
                    defaultMovingLineWidth);
            mFrameLineWidth = a.getDimension(R.styleable.ScannerView_frameLineWidth,
                    defaultFrameLineWidth);
            mOrthogonalFrameLineLength = a.getDimension(
                    R.styleable.ScannerView_orthogonalFrameLineLength,
                    defaultOrthogonalFrameLineLength);

            mFrameColor = a.getColor(R.styleable.ScannerView_frameColor,
                    Color.argb(150, 200, 200, 200));
            mMovingLineColor = a.getColor(R.styleable.ScannerView_movingLineColor,
                    Color.argb(150, 0, 255, 0));
            mPaddingColor = a.getColor(R.styleable.ScannerView_paddingColor,
                    Color.argb(100, 150, 150, 150));
            mUpAndDownSeconds = a.getFloat(R.styleable.ScannerView_upAndDownSeconds, 3.5f);

            // Init the paints for drawing
            initPaints();
            startMovingLineAnimation();
        } finally {
            a.recycle();
        }
    }

    private void initPaints() {
        mFramePaint = new Paint(0);
        mFramePaint.setColor(mFrameColor);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setStrokeWidth(mFrameLineWidth);

        mMovingLinePaint = new Paint(0);
        mMovingLinePaint.setColor(mMovingLineColor);
        mMovingLinePaint.setStyle(Paint.Style.STROKE);
        mMovingLinePaint.setStrokeWidth(mMovingLineWidth);

        mPaddingPaint = new Paint(0);
        mPaddingPaint.setColor(mPaddingColor);
        mPaddingPaint.setStyle(Paint.Style.FILL);
    }

    private void startMovingLineAnimation() {
        // Set up the animation of the moving line
        ValueAnimator animation = ValueAnimator.ofFloat(0, 1);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration((long) (mUpAndDownSeconds / 2 * 1000));
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mMovingLineProgress = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });

        // Reverse the animation when it's finished
        animation.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                mLineMovingDown = !mLineMovingDown;
                startMovingLineAnimation();
            }
        });

        animation.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate all size-dependent attributes
        mFrameWidth = (int) (w * mFrameWidthPercentage);

        if (mFrameAspectRatio > 0) {
            // Ignore the frame height percentage and just use the aspect ratio
            mFrameHeight = (int) (mFrameWidth / mFrameAspectRatio);
        } else {
            // Use the frame height percentage attribute
            mFrameHeight = (int) (h * mFrameHeightPercentage);
        }

        mPaddingWidth = (w - mFrameWidth) / 2;
        mPaddingHeight = (h - mFrameHeight) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Top Padding
        canvas.drawRect(0, 0, this.getWidth(), mPaddingHeight, mPaddingPaint);
        // Bottom Padding
        canvas.drawRect(0, mPaddingHeight + mFrameHeight, this.getWidth(),
                this.getHeight(), mPaddingPaint);
        // Left Padding
        canvas.drawRect(0, mPaddingHeight, mPaddingWidth, mPaddingHeight + mFrameHeight,
                mPaddingPaint);
        // Right Padding
        canvas.drawRect(mPaddingWidth + mFrameWidth, mPaddingHeight, this.getWidth(),
                mPaddingHeight + mFrameHeight, mPaddingPaint);

        // Draw the frame
        canvas.drawRect(mPaddingWidth, mPaddingHeight, mPaddingWidth + mFrameWidth,
                mPaddingHeight + mFrameHeight, mFramePaint);

        // Draw the top orthogonal frame line
        float centerX = this.getWidth() / 2;
        float centerY = this.getHeight() / 2;
        float halfLineLength = 0.5f * mOrthogonalFrameLineLength;
        canvas.drawLine(centerX, mPaddingHeight - halfLineLength, centerX,
                mPaddingHeight + halfLineLength, mFramePaint);
        // Draw the bottom orthogonal frame line
        canvas.drawLine(centerX, mPaddingHeight + mFrameHeight - halfLineLength, centerX,
                mPaddingHeight + mFrameHeight + halfLineLength, mFramePaint);

        // Draw the left orthogonal frame line
        canvas.drawLine(mPaddingWidth - halfLineLength, centerY,
                mPaddingWidth + halfLineLength, centerY, mFramePaint);
        // Draw the right orthogonal frame line
        canvas.drawLine(mPaddingWidth + mFrameWidth - halfLineLength, centerY,
                mPaddingWidth + mFrameWidth + halfLineLength, centerY, mFramePaint);

        // Draw the moving line
        if (mAnimated) {
            float lineAreaHeight = mFrameHeight - mFrameLineWidth;
            float relativeY = mMovingLineProgress * lineAreaHeight;
            if (!mLineMovingDown) {
                relativeY = lineAreaHeight - relativeY;
            }
            float lineY = mPaddingHeight + mFrameLineWidth + relativeY;
            canvas.drawLine(mPaddingWidth + mFrameLineWidth / 2, lineY,
                    mPaddingWidth + mFrameWidth - mFrameLineWidth / 2, lineY, mMovingLinePaint);
        }
    }

    public void setAnimated(boolean animated) {
        this.mAnimated = animated;
    }
}

