# SteeringWheelView
自定义触屏游戏方向盘控件，可用于控制游戏中英雄、控制安防中的云台、控制机器人等
# 效果图
![image](https://github.com/passionli/SteeringWheelView/blob/master/screencapture.png)
# 特性
- 支持自定义数据回调间隔
- 支持自定义球回弹动画
- 支持角度、力度、大方向

# Sample
xml布局：
``` xml
<com.liguang.steeringwheel.SteeringWheelView
    android:id="@+id/steeringWheelView"
    android:layout_width="200dp"
    android:layout_height="200dp"
    android:layout_gravity="bottom|start"
    app:arrowRight="@mipmap/ic_keyboard_arrow_right_black_48dp"
    app:ballColor="#33b5e5"
    app:ballPressedSrc="@mipmap/ic_blur_circular_black_48dp_pressed"
    app:ballSrc="@mipmap/ic_blur_circular_black_48dp" />
```
初始化：
``` java
//配置参数
mSteeringWheel.notifyInterval(16).listener(this).interpolator(new OvershootInterpolator());
```
接收数据回调:
``` java
@Override
public void onStatusChanged(SteeringWheelView view, int angle, int power, int direction) {
    //更新UI
    String text = constructText(angle, power, direction);
    mTv.setText(text);
    ...
    //把数据交给底层
}
```
