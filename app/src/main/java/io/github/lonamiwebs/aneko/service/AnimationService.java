package io.github.lonamiwebs.aneko.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Random;

import io.github.lonamiwebs.aneko.MotionDrawable;
import io.github.lonamiwebs.aneko.MotionParams;
import io.github.lonamiwebs.aneko.NekoSkin;
import io.github.lonamiwebs.aneko.R;
import io.github.lonamiwebs.aneko.behaviours.Behaviour;

public class AnimationService extends Service {
    public static final String ACTION_START = "io.github.lonamiwebs.aneko.action.START";
    public static final String ACTION_STOP = "io.github.lonamiwebs.aneko.action.STOP";
    public static final String ACTION_TOGGLE =
            "io.github.lonamiwebs.aneko.action.TOGGLE";

    public static final String ACTION_GET_SKIN =
            "io.github.lonamiwebs.aneko.action.GET_SKIN";
    public static final String META_KEY_SKIN = "io.github.lonamiwebs.aneko.skin";

    public static final String PREF_KEY_ENABLE = "motion.enable";
    public static final String PREF_KEY_VISIBLE = "motion.visible";
    public static final String PREF_KEY_TRANSPARENCY = "motion.transparency";
    public static final String PREF_KEY_BEHAVIOUR = "motion.behaviour";
    public static final String PREF_KEY_SKIN_COMPONENT = "motion.skin";

    private static final int NOTIF_ID = 1;

    private static final int MSG_ANIMATE = 1;

    static final long ANIMATION_INTERVAL = 125; // msec
    private static final long BEHAVIOUR_CHANGE_DURATION = 4000; // msec

    private static final String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE =
            "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
    private static final String EXTRA_CHANGED_PACKAGE_LIST =
            "android.intent.extra.changed_package_list";

    private static final boolean ICS_OR_LATER =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private boolean isStarted;
    private SharedPreferences prefs;
    private PreferenceChangeListener prefListener;

    private Handler handler;
    private MotionState motionState;
    private Random random;
    private View touchView;
    private ImageView imageView;
    private WindowManager.LayoutParams touchParams;
    private WindowManager.LayoutParams imageParams;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        isStarted = false;
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return onHandleMessage(msg);
            }
        });
        random = new Random();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        onStartCommand(intent, 0, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isStarted && (intent == null || ACTION_START.equals(intent.getAction()))) {
            startAnimation();
            setForegroundNotification(true);
            isStarted = true;
        } else if (ACTION_TOGGLE.equals(intent.getAction())) {
            toggleAnimation();
        } else if (isStarted && ACTION_STOP.equals(intent.getAction())) {
            stopAnimation();
            stopSelfResult(startId);
            setForegroundNotification(false);
            isStarted = false;
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {
        if (!isStarted || motionState == null) {
            return;
        }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int dw = wm.getDefaultDisplay().getWidth();
        int dh = wm.getDefaultDisplay().getHeight();
        motionState.display.set(dw, dh);
    }

    private void startAnimation() {
        prefListener = new PreferenceChangeListener();
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        if (!checkPrefEnable()) {
            return;
        }
        if (!loadMotionState()) {
            return;
        }

        // prepare to receive broadcast
        IntentFilter filter;
        receiver = new Receiver();

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        registerReceiver(receiver, filter);

        filter = new IntentFilter();
        filter.addAction(ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        registerReceiver(receiver, filter);

        // touch event sink and overlay view
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        touchView = new View(this);
        touchView.setOnTouchListener(new TouchListener());
        touchParams = new WindowManager.LayoutParams(
                0, 0,
                (ICS_OR_LATER ?
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR :
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        touchParams.gravity = Gravity.LEFT | Gravity.TOP;
        wm.addView(touchView, touchParams);

        imageView = new ImageView(this);
        imageParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        imageParams.gravity = Gravity.LEFT | Gravity.TOP;
        wm.addView(imageView, imageParams);

        requestAnimate();
    }

    private void stopAnimation() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (touchView != null) {
            wm.removeView(touchView);
        }
        if (imageView != null) {
            wm.removeView(imageView);
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }

        motionState = null;
        touchView = null;
        imageView = null;
        receiver = null;

        handler.removeMessages(MSG_ANIMATE);
    }

    private void toggleAnimation() {
        boolean visible = prefs.getBoolean(PREF_KEY_VISIBLE, true);

        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREF_KEY_VISIBLE, !visible);
        edit.commit();

        startService(new Intent(this, AnimationService.class).setAction(ACTION_START));
    }

    private void setForegroundNotification(boolean start) {
        PendingIntent intent = PendingIntent.getService(this, 0,
                new Intent(this, AnimationService.class).setAction(ACTION_TOGGLE), 0);

        Notification notif = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(start ? R.string.notification_enable : R.string.notification_disable))
                .setSmallIcon(start ? R.drawable.mati1 : R.drawable.sleep1)
                .setContentIntent(intent)
                .setWhen(0)
                .build();

        notif.flags = Notification.FLAG_ONGOING_EVENT;

        if (start) {
            startForeground(NOTIF_ID, notif);
        } else {
            stopForeground(true);

            boolean enable = prefs.getBoolean(PREF_KEY_ENABLE, true);
            if (enable) {
                NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                nm.notify(NOTIF_ID, notif);
            }
        }
    }

    private boolean loadMotionState() {
        String skinPkg = prefs.getString(PREF_KEY_SKIN_COMPONENT, null);
        ComponentName skinComp = skinPkg == null ? null : ComponentName.unflattenFromString(skinPkg);

        if (skinComp != null && loadMotionState(skinComp)) {
            return true;
        }

        skinComp = new ComponentName(this, NekoSkin.class);
        return loadMotionState(skinComp);
    }

    private boolean loadMotionState(ComponentName skinComp) {
        motionState = new MotionState(new MotionDrawable.OnMotionEndListener() {
            @Override
            public void onMotionEnd(MotionDrawable drawable) {
                if (isStarted && motionState != null &&
                        drawable == motionState.getCurrentDrawable()) {
                    updateToNext();
                }
            }
        });

        try {
            PackageManager pm = getPackageManager();
            ActivityInfo ai = pm.getActivityInfo(skinComp, PackageManager.GET_META_DATA);
            Resources res = pm.getResourcesForActivity(skinComp);

            int rid = ai.metaData.getInt(META_KEY_SKIN, 0);

            MotionParams params = new MotionParams(this, res, rid);
            motionState.setParams(params);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.msg_skin_load_failed, Toast.LENGTH_LONG).show();
            startService(new Intent(this, AnimationService.class).setAction(ACTION_TOGGLE));
            return false;
        }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int dw = wm.getDefaultDisplay().getWidth();
        int dh = wm.getDefaultDisplay().getHeight();
        int cx, cy;
        {
            int pos = random.nextInt(400);
            int ratio = pos % 100;
            if (pos / 200 == 0) {
                cx = (dw + 200) * ratio / 100 - 100;
                cy = ((pos / 100) % 2 == 0 ? -100 : dh + 100);
            } else {
                cx = ((pos / 100) % 2 == 0 ? -100 : dw + 100);
                cy = (dh + 200) * ratio / 100 - 100;
            }
        }

        String alphaStr = prefs.getString(PREF_KEY_TRANSPARENCY, "0.0");
        motionState.alpha = (int) ((1 - Float.valueOf(alphaStr)) * 0xff);

        motionState.setBehaviour(Behaviour.fromName(
                prefs.getString(PREF_KEY_BEHAVIOUR, motionState.behaviour.toString())));

        motionState.display.set(dw, dh);
        motionState.cur.set(cx, cy);
        motionState.target.set(dw / 2, dh / 2);

        return true;
    }

    private void requestAnimate() {
        if (!handler.hasMessages(MSG_ANIMATE)) {
            handler.sendEmptyMessage(MSG_ANIMATE);
        }
    }

    private void updateDrawable() {
        if (motionState == null || imageView == null) {
            return;
        }

        MotionDrawable drawable = motionState.getCurrentDrawable();
        if (drawable == null) {
            return;
        }

        drawable.setAlpha(motionState.alpha);
        imageView.setImageDrawable(drawable);
        drawable.stop();
        drawable.start();
    }

    private void updatePosition() {
        Point pt = motionState.getPosition();
        imageParams.x = pt.x;
        imageParams.y = pt.y;

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.updateViewLayout(imageView, imageParams);
    }

    private void updateToNext() {
        if (motionState.checkWall() ||
                motionState.updateMovingState() ||
                motionState.changeToNextState()) {
            updateDrawable();
            updatePosition();
            requestAnimate();
        }
    }

    private boolean onHandleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ANIMATE:
                handler.removeMessages(MSG_ANIMATE);

                motionState.updateState();
                if (motionState.isStateChanged() ||
                        motionState.isPositionMoved()) {
                    if (motionState.isStateChanged()) {
                        updateDrawable();
                    }

                    updatePosition();

                    handler.sendEmptyMessageDelayed(
                            MSG_ANIMATE, ANIMATION_INTERVAL);
                }
                break;

            default:
                return false;
        }

        return true;
    }

    private boolean checkPrefEnable() {
        boolean enable = prefs.getBoolean(PREF_KEY_ENABLE, true);
        boolean visible = prefs.getBoolean(PREF_KEY_VISIBLE, true);
        if (!enable || !visible) {
            startService(new Intent(this, AnimationService.class).setAction(ACTION_STOP));
            return false;
        } else {
            return true;
        }
    }

    private class PreferenceChangeListener
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs,
                                              String key) {
            if (PREF_KEY_ENABLE.equals(key) || PREF_KEY_VISIBLE.equals(key)) {
                checkPrefEnable();
            } else if (loadMotionState()) {
                requestAnimate();
            }
        }
    }

    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] pkgnames = null;
            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) && intent.getData() != null) {
                pkgnames = new String[]{intent.getData().getEncodedSchemeSpecificPart()};
            } else if (ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(intent.getAction())) {
                pkgnames = intent.getStringArrayExtra(EXTRA_CHANGED_PACKAGE_LIST);
            }
            if (pkgnames == null) {
                return;
            }

            String skin = prefs.getString(PREF_KEY_SKIN_COMPONENT, null);
            ComponentName skinComp = skin == null ? null : ComponentName.unflattenFromString(skin);
            if (skinComp == null) {
                return;
            }

            String skinPkg = skinComp.getPackageName();
            for (String pkgname : pkgnames) {
                if (skinPkg.equals(pkgname)) {
                    if (loadMotionState()) {
                        requestAnimate();
                    }
                    break;
                }
            }
        }
    }

    private class TouchListener implements View.OnTouchListener {
        public boolean onTouch(View v, MotionEvent ev) {
            if (motionState == null) {
                return false;
            }

            if (ev.getAction() == MotionEvent.ACTION_OUTSIDE) {
                motionState.setTargetPosition(ev.getX(), ev.getY());
                requestAnimate();
            } else if (ev.getAction() == MotionEvent.ACTION_CANCEL) {
                motionState.forceStop();
                requestAnimate();
            }

            return false;
        }
    }
}
