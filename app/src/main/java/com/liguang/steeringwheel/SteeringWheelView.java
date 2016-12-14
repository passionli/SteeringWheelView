package com.liguang.steeringwheel;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * 方向盘控件。
 */
public class SteeringWheelView extends View {
    private static final String TAG = "SteeringWheelView";
    /**
     * 当前方向无效，方向盘没有触摸时处于该状态
     */
    public static final int INVALID = -1;
    /**
     * 向右
     */
    public static final int RIGHT = 0;
    /**
     * 向上
     */
    public static final int UP = 1;
    /**
     * 向左
     */
    public static final int LEFT = 2;
    /**
     * 向下
     */
    public static final int DOWN = 4;
    /**
     * 外部监听器
     */
    private SteeringWheelListener mListener;
    private static final int mDefaultWidthDp = 200;
    private static final int mDefaultHeightDp = 200;
    /**
     * 当采用wrap_content测量模式时，默认宽度
     */
    private static int mDefaultWidth;
    /**
     * 当采用wrap_content测量模式时，默认高度
     */
    private static int mDefaultHeight;
    /**
     * 画笔对象
     */
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * 画笔的颜色
     */
    private int mColor;
    /**
     * 当前中心X
     */
    private float mCenterX;
    /**
     * 当前中心Y
     */
    private float mCenterY;
    /**
     * 球
     */
    private Drawable mBallDrawable;
    /**
     * 被按下后，球的图片
     */
    private Drawable mBallPressedDrawable;
    /**
     * 当前球中心X坐标
     */
    private float mBallCenterX;
    /**
     * 当前球中心Y坐标
     */
    private float mBallCenterY;
    /**
     * 球的半径
     */
    private float mBallRadius;
    /**
     * 当前半径
     */
    private float mRadius;
    /**
     * 当前角度
     */
    private double mAngle;
    /**
     * 当前偏离中心的百分比，取值为 0 - 100
     */
    private int mPower;
    /**
     * 通知的时间最小间隔
     */
    private long mNotifyInterval = 0;
    /**
     * 通知者
     */
    private Runnable mNotifyRunnable;
    /**
     * 上次通知监听者的时间
     */
    private long mLastNotifyTime;
    /**
     * 当前方向
     */
    private int mDirection = INVALID;
    /**
     * 向右箭头
     */
    private Drawable mArrowRightDrawable;
    /**
     * 回弹动画
     */
    private ObjectAnimator mAnimator;

    /**
     * 时间插值器
     */
    private TimeInterpolator mInterpolator;
    private boolean mWasTouched;

    /**
     * 获取球X坐标
     *
     * @return 球X坐标
     */
    public float getBallX() {
        return mBallCenterX;
    }

    /**
     * 设置球X坐标。目前该API的执行时机为Choreographer中每帧中的动画阶段,由底层动画框架反射调用
     *
     * @param ballX 球X坐标
     */
    public void setBallX(float ballX) {
        if (ballX != mBallCenterX) {
            mBallCenterX = ballX;
            updatePower();
            updateDirection();
            invalidate();
            notifyStatusChanged();
        }
    }

    /**
     * 获取球Y坐标
     *
     * @return 球Y坐标
     */
    public float getBallY() {
        return mBallCenterY;
    }

    /**
     * 设置球Y坐标。目前该API的执行时机为Choreographer中每帧中的动画阶段,由底层动画框架反射调用
     *
     * @param ballY 球Y坐标
     */
    public void setBallY(float ballY) {
        if (mBallCenterY != ballY) {
            mBallCenterY = ballY;
            updatePower();
            updateDirection();
            invalidate();
            notifyStatusChanged();
        }
    }

    public SteeringWheelView(Context context) {
        super(context);
        init(null, 0);
    }

    public SteeringWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SteeringWheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SteeringWheelView);
        //读取XML配置
        mColor = a.getColor(R.styleable.SteeringWheelView_ballColor, Color.RED);
        mArrowRightDrawable = a.getDrawable(R.styleable.SteeringWheelView_arrowRight);
        mBallDrawable = a.getDrawable(R.styleable.SteeringWheelView_ballSrc);
        mBallPressedDrawable = a.getDrawable(R.styleable.SteeringWheelView_ballPressedSrc);
        a.recycle();
        mBallRadius = mBallDrawable.getIntrinsicWidth() >> 1;
        mDefaultWidth = LGViewUtils.dp2px(getContext(), mDefaultWidthDp);
        mDefaultHeight = LGViewUtils.dp2px(getContext(), mDefaultHeightDp);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(TAG, "onMeasure: ");
        //handle wrap_content
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        //解析上层ViewGroup传下来的数据，高两位是模式，低30位是大小
        //主要需要特殊处理wrap_content情形
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mDefaultWidth, mDefaultHeight);
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mDefaultWidth, heightSpecSize);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, mDefaultHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: w=" + w + "#h=" + h + "#oldw=" + oldw + "#oldh=" + oldh);
        //在layout过程中会回调该方法
        //handle padding
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        int width = getWidth() - paddingLeft - paddingRight;
        int height = getHeight() - paddingTop - paddingBottom;

        mRadius = (Math.min(width, height) >> 1) - mArrowRightDrawable.getIntrinsicWidth() / 2;
        mBallCenterX = mCenterX = paddingLeft + (width >> 1);
        mBallCenterY = mCenterY = paddingTop + (height >> 1);

        //calc arrow bounds
        mArrowRightDrawable.setBounds((int) (mCenterX + mRadius - mArrowRightDrawable.getIntrinsicWidth() / 2),
                (int) (mCenterY - mArrowRightDrawable.getIntrinsicHeight() / 2),
                (int) (mCenterX + mRadius + mArrowRightDrawable.getIntrinsicWidth() / 2),
                (int) (mCenterY + mArrowRightDrawable.getIntrinsicHeight() / 2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画横线
        canvas.drawLine(mCenterX - mRadius, mCenterY, mCenterX + mRadius, mCenterY, mPaint);
        //画竖线
        canvas.drawLine(mCenterX, mCenterY - mRadius, mCenterX, mCenterY + mRadius, mPaint);
        //画大圆
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
        //画球
        drawBall(canvas);
        //画箭头
        drawArrow(canvas);
    }

    private void drawBall(Canvas canvas) {
        Drawable drawable;
        //point to the right drawable instance
        if (mWasTouched) {
            drawable = mBallPressedDrawable;
        } else {
            drawable = mBallDrawable;
        }
        drawable.setBounds((int) (mBallCenterX - drawable.getIntrinsicWidth() / 2),
                (int) (mBallCenterY - drawable.getIntrinsicHeight() / 2),
                (int) (mBallCenterX + drawable.getIntrinsicWidth() / 2),
                (int) (mBallCenterY + drawable.getIntrinsicHeight() / 2));
        drawable.draw(canvas);
    }

    /**
     * 画箭头
     *
     * @param canvas 画布对象
     */
    private void drawArrow(Canvas canvas) {
        if (!mWasTouched)
            return;

        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        //旋转角度
        canvas.rotate((float) -mAngle, mCenterX, mCenterY);
        mArrowRightDrawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: ");
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mWasTouched = true;
                if (mAnimator != null && mAnimator.isRunning()) {
                    //在本次触摸事件序列中，如果上一个复位动画还没执行完毕，则需要取消动画，及时响应用户输入
                    mAnimator.cancel();
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                updateBallData(x, y);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mWasTouched = false;
                resetBall();
                break;
            }
            default:
                break;
        }

        notifyStatusChanged();
        return true;
    }

    /**
     * 指定球回弹动画时间插值器
     *
     * @param value 插值器
     */
    public SteeringWheelView interpolator(TimeInterpolator value) {
        if (value != null) {
            mInterpolator = value;
        } else {
            mInterpolator = new OvershootInterpolator();
        }
        return this;
    }

    private TimeInterpolator getInterpolator() {
        if (mInterpolator == null) {
            mInterpolator = new OvershootInterpolator();
        }
        return mInterpolator;
    }

    /**
     * 弹性滑动
     */
    private void resetBall() {
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("BallX", mBallCenterX, mCenterX);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("BallY", mBallCenterY, mCenterY);
        mAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvhX, pvhY).setDuration(150);
        mAnimator.setInterpolator(getInterpolator());
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAngle = 0;
                mPower = 0;
                mDirection = INVALID;
                notifyStatusChanged();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAnimator.start();
    }

    private void updateBallData(int x, int y) {
        mBallCenterX = x;
        mBallCenterY = y;
        boolean outOfRange = outOfRange(x, y);
        //采用(a, b]开闭区间
        if (x >= mCenterX && y < mCenterY) {
            //第一象限
            mAngle = Math.toDegrees(Math.atan((mCenterY - y) / (x - mCenterX)));
            if (outOfRange) {
                mBallCenterX = (float) (mCenterX + Math.cos(Math.toRadians(mAngle)) * (mRadius - mBallRadius));
                mBallCenterY = (float) (mCenterY - Math.sin(Math.toRadians(mAngle)) * (mRadius - mBallRadius));
            }
        } else if (x < mCenterX && y <= mCenterY) {
            //第二象限
            mAngle = 180 - Math.toDegrees(Math.atan((mCenterY - y) / (mCenterX - x)));
            if (outOfRange) {
                mBallCenterX = (float) (mCenterX - Math.cos(Math.toRadians(180 - mAngle)) * (mRadius - mBallRadius));
                mBallCenterY = (float) (mCenterY - Math.sin(Math.toRadians(180 - mAngle)) * (mRadius - mBallRadius));
            }
        } else if (x <= mCenterX && y > mCenterY) {
            //第三象限
            mAngle = 270 - Math.toDegrees(Math.atan((mCenterX - x) / (y - mCenterY)));
            if (outOfRange) {
                mBallCenterX = (float) (mCenterX - Math.cos(Math.toRadians(mAngle - 180)) * (mRadius - mBallRadius));
                mBallCenterY = (float) (mCenterY + Math.sin(Math.toRadians(mAngle - 180)) * (mRadius - mBallRadius));
            }
        } else if (x > mCenterX && y >= mCenterY) {
            //第四象限
            mAngle = 360 - Math.toDegrees(Math.atan((y - mCenterY) / (x - mCenterX)));
            if (outOfRange) {
                mBallCenterX = (float) (mCenterX + Math.cos(Math.toRadians(360 - mAngle)) * (mRadius - mBallRadius));
                mBallCenterY = (float) (mCenterY + Math.sin(Math.toRadians(360 - mAngle)) * (mRadius - mBallRadius));
            }
        }
        updatePower();
        updateDirection();
        invalidate();
    }

    private void updatePower() {
        mPower = (int) (100 * Math.sqrt(Math.pow(mBallCenterX - mCenterX, 2) + Math.pow(mBallCenterY - mCenterY, 2)) / (mRadius - mBallRadius));
        Log.d(TAG, "updatePower: mPower = " + mPower);
    }

    private boolean outOfRange(int newX, int newY) {
        return (Math.pow(newX - mCenterX, 2) + Math.pow(newY - mCenterY, 2)) > Math.pow(mRadius - mBallRadius, 2);
    }

    /**
     * 采用(a,b]开闭区间
     *
     * @return 方向值
     */
    private int updateDirection() {
        if (Math.abs(mCenterX - mBallCenterX) < 0.00000001
                && Math.abs(mCenterY - mBallCenterY) < 0.00000001)
            mDirection = INVALID;
        else if (mAngle <= 45 || mAngle > 315)
            mDirection = RIGHT;
        else if (mAngle > 45 && mAngle <= 135)
            mDirection = UP;
        else if (mAngle > 135 && mAngle <= 225)
            mDirection = LEFT;
        else
            mDirection = DOWN;
        return mDirection;
    }

    /**
     * 通知监听者方向盘状态改变
     */
    private void notifyStatusChanged() {
        if (mListener == null)
            return;

        long delay = 0;
        if (mNotifyRunnable == null) {
            mNotifyRunnable = createNotifyRunnable();
        } else {
            long now = System.currentTimeMillis();
            if (now - mLastNotifyTime < mNotifyInterval) {
                //移除旧消息
                removeCallbacks(mNotifyRunnable);
                delay = mNotifyInterval - (now - mLastNotifyTime);
            }
        }

        postDelayed(mNotifyRunnable, delay);
    }

    private Runnable createNotifyRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: ");
                mLastNotifyTime = System.currentTimeMillis();
                //取当前数据，而非过去数据的snapshot
                mListener.onStatusChanged(SteeringWheelView.this, (int) mAngle, mPower, mDirection);
            }
        };
    }

    /**
     * 设置回调时间间隔
     *
     * @param interval 回调时间间隔
     */
    public SteeringWheelView notifyInterval(long interval) {
        if (interval < 0) {
            throw new RuntimeException("notifyInterval interval < 0 is not accept");
        }

        mNotifyInterval = interval;
        return this;
    }

    /**
     * 设置监听器
     *
     * @param listener 监听器对象
     */
    public SteeringWheelView listener(SteeringWheelListener listener) {
        mListener = listener;
        return this;
    }

    interface SteeringWheelListener {
        /**
         * 方向盘状态改变的回调
         *
         * @param view      方向盘实例对象
         * @param angle     当前角度。范围0-360，其中右0，上90，左180，下270
         * @param power     方向上的力度。范围0-100
         * @param direction 大致方向。取值为 {@link #RIGHT} {@link #UP} {@link #LEFT} {@link #DOWN}
         */
        void onStatusChanged(SteeringWheelView view, int angle, int power, int direction);
    }
}
