package com.liguang.steeringwheel;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SteeringWheelView.SteeringWheelListener {
    private static final String TAG = "MainActivity";
    @BindView(R.id.steeringWheelView)
    SteeringWheelView mSteeringWheel;
    @BindView(R.id.tv)
    TextView mTv;
    private SparseArray<String> mSparseArray = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_steering_wheel_view);
        initData();
        initView();
    }

    private void initData() {
        mSparseArray.put(SteeringWheelView.LEFT, getString(R.string.left));
        mSparseArray.put(SteeringWheelView.UP, getString(R.string.up));
        mSparseArray.put(SteeringWheelView.RIGHT, getString(R.string.right));
        mSparseArray.put(SteeringWheelView.DOWN, getString(R.string.down));
        mSparseArray.put(SteeringWheelView.INVALID, getString(R.string.idle));
    }

    private void initView() {
        ButterKnife.bind(this);
        mSteeringWheel.notifyInterval(16).listener(this).interpolator(new OvershootInterpolator());
    }

    @Override
    public void onStatusChanged(SteeringWheelView view, int angle, int power, int direction) {
        String text = constructText(angle, power, direction);
        mTv.setText(text);
    }

    private String constructText(int angle, int power, int direction) {
        return String.format(Locale.CHINESE, "angle = %3d\npower = %3d\ndirection = %s",
                angle, power, direction2Text(direction));
    }

    private String direction2Text(int direction) {
        return mSparseArray.get(direction);
    }
}
