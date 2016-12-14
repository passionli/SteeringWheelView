package com.liguang.steeringwheel;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

public class LGViewUtils {
    public static int dp2px(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int px2dp(Context context, int px) {
        return (int) (px / context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static void setFullscreen(Activity activity, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |=  WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            winParams.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        win.setAttributes(winParams);
    }

    public static boolean isFullScreen(Activity activity) {
        return (WindowManager.LayoutParams.FLAG_FULLSCREEN & activity.getWindow().getAttributes().flags) != 0;
    }

    public static void rotateScreen(Activity activity) {
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public static void enableDisableView(View view, boolean enabled) {
        view.setEnabled(enabled);

        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                enableDisableView(((ViewGroup) view).getChildAt(i), enabled);
            }
        }
    }

    public static View find(View view, int id) {
        if (view.getId() == id)
            return view;
        if (view instanceof ViewGroup) {
            int count = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < count; i++) {
                View child = ((ViewGroup) view).getChildAt(i);
                View found = find(child, id);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    public static boolean isChildOf(View root, View child) {
        if (root == child)
            return true;

        if (root instanceof ViewGroup) {
            int count = ((ViewGroup) root).getChildCount();
            for (int i = 0; i < count; i++) {
                View v = ((ViewGroup) root).getChildAt(i);
                if (isChildOf(v, child))
                    return true;
            }
        }

        return false;
    }

    public static int getStatusBarHeight(Context context) {
        int value, statusBarHeight;
        try {
            Class<?> classR = Class.forName("com.android.internal.R$dimen");
            Object obj = classR.newInstance();
            Field field = classR.getField("status_bar_height");
            value = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(value);
            return statusBarHeight;
        } catch (Exception e1) {
            e1.printStackTrace();
            return 0;
        }
    }

    public static Bitmap createBitmapFromView(View v) {
        if (v == null) {
            return null;
        }
        Bitmap screenshot;
        screenshot = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(screenshot);
        c.translate(-v.getScrollX(), -v.getScrollY());
        v.draw(c);
        return screenshot;
    }

    public static boolean saveBitmap2Disk(Bitmap bitmap, String path, String fileName) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File folder = new File(
                    Environment.getExternalStorageDirectory()
                            + path);
            boolean folderExists = true;
            if (!folder.exists()) {
                folderExists = folder.mkdirs();
            }

            if (folderExists) {
                File outputFile = new File(folder, fileName);
                if (outputFile.exists()) {
                    if (!outputFile.delete())
                        return false;
                }

                try {
                    FileOutputStream out = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                    out.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        return false;
    }

    public static void setTransparent(Context context, View view) {
        view.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
    }

    public static void viewAnim(Context context, View view, int animId) {
        view.startAnimation(AnimationUtils.loadAnimation(context, animId));
    }
}
