package io.github.lonamiwebs.aneko;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import java.util.ArrayList;

public class MotionDrawable extends Drawable implements Animatable {
    public interface OnMotionEndListener {
        public void onMotionEnd(MotionDrawable drawable);
    }

    private MotionConstantState state;

    private int curFrame = -1;
    private int curRepeat = 0;
    private int curDuration = -1;
    private OnMotionEndListener onEnd;

    private int alpha = 0xff;
    private boolean dither = true;
    private ColorFilter colorFilter;

    private Runnable frameUpdater = new Runnable() {
        @Override
        public void run() {
            updateFrame();
        }
    };
    private Drawable.Callback childCallback = new ChildCallback();
    private OnMotionEndListener childEnd = new ChildOnMotionEnd();

    public MotionDrawable() {
        state = new MotionConstantState();
    }

    public MotionDrawable(AnimationDrawable anim) {
        this();

        state.repeatCount = (anim.isOneShot() ? 1 : -1);

        int nf = anim.getNumberOfFrames();
        for (int i = 0; i < nf; i++) {
            addFrame(anim.getFrame(i), anim.getDuration(i));
        }
    }

    public void setTotalDuration(int duration) {
        state.totalDuration = duration;
    }

    public void setRepeatCount(int count) {
        state.repeatCount = count;
    }

    public void addFrame(Drawable drawable, int duration) {
        if (drawable instanceof AnimationDrawable) {
            MotionDrawable md = new MotionDrawable((AnimationDrawable) drawable);
            md.setTotalDuration(duration);
            drawable = md;
        }

        if (drawable instanceof MotionDrawable) {
            MotionDrawable md = (MotionDrawable) drawable;
            md.setOnMotionEndListener(childEnd);
        }
        drawable.setCallback(childCallback);
        state.addFrame(drawable, duration);
    }

    public Drawable getCurrentFrame() {
        return state.getFrame(curFrame);
    }

    public void setOnMotionEndListener(OnMotionEndListener listener) {
        onEnd = listener;
    }

    private void invokeOnMotionEndListener() {
        if (onEnd != null) {
            onEnd.onMotionEnd(this);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return getCurrentFrame().getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return getCurrentFrame().getIntrinsicHeight();
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        return state;
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            current.draw(canvas);
        }
    }

    @Override
    public int getOpacity() {
        Drawable current = getCurrentFrame();
        return ((current == null || !current.isVisible()) ?
                PixelFormat.TRANSPARENT : state.getOpacity());
    }

    @Override
    public void setAlpha(int _alpha) {
        if (alpha != _alpha) {
            alpha = _alpha;
            Drawable current = getCurrentFrame();
            if (current != null) {
                current.setAlpha(alpha);
            }
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (colorFilter != cf) {
            colorFilter = cf;
            Drawable current = getCurrentFrame();
            if (current != null) {
                current.setColorFilter(colorFilter);
            }
        }
    }

    @Override
    public void setDither(boolean _dither) {
        if (dither != _dither) {
            dither = _dither;
            Drawable current = getCurrentFrame();
            if (current != null) {
                current.setDither(dither);
            }
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            current.setBounds(bounds);
        }
    }

    @Override
    protected boolean onLevelChange(int level) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            return current.setLevel(level);
        }
        return false;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            return current.setState(state);
        }
        return false;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        Drawable current = getCurrentFrame();
        if (current != null) {
            current.setVisible(visible, restart);
        }

        if (visible) {
            if (changed || restart) {
                stop();
                start();
            }
        } else {
            stop();
        }

        return changed;
    }

    @Override
    public boolean isRunning() {
        return (curDuration >= 0);
    }

    @Override
    public void start() {
        if (!isRunning()) {
            curFrame = -1;
            curRepeat = 0;
            curDuration = 0;
            updateFrame();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            unscheduleSelf(frameUpdater);
            curDuration = -1;
        }
    }

    private void updateFrame() {
        int nf = state.getFrameCount();
        int next = curFrame + 1;
        int nextRepeat = curRepeat;
        if (next >= nf) {
            next = 0;
            nextRepeat = curRepeat + 1;

            if (state.repeatCount >= 0 && nextRepeat >= state.repeatCount) {
                curDuration = -1;
                invokeOnMotionEndListener();
                return;
            }
        }

        if (state.totalDuration >= 0 && curDuration >= state.totalDuration) {
            curDuration = -1;
            invokeOnMotionEndListener();
            return;
        }

        {
            Drawable current = getCurrentFrame();
            if (current != null) {
                current.setVisible(false, false);
            }
        }

        curFrame = next;
        curRepeat = nextRepeat;

        Drawable nextDrawable = state.getFrame(next);
        nextDrawable.setVisible(isVisible(), true);
        nextDrawable.setAlpha(alpha);
        nextDrawable.setDither(dither);
        nextDrawable.setColorFilter(colorFilter);
        nextDrawable.setState(getState());
        nextDrawable.setLevel(getLevel());
        nextDrawable.setBounds(getBounds());

        int duration = state.getFrameDuration(next);
        int nextDuration =
            (duration < 0 && state.totalDuration < 0 ? -1 :
             duration < 0 ? state.totalDuration - curDuration :
             state.totalDuration < 0 ? curDuration + duration :
             Math.min(curDuration + duration,
                      state.totalDuration));
        if(nextDuration >= 0) {
            duration = nextDuration - curDuration;
            scheduleSelf(frameUpdater, SystemClock.uptimeMillis() + duration);
        }

        curDuration = (nextDuration >= 0 ? nextDuration : curDuration);
        invalidateSelf();
    }

    private static class MotionConstantState extends ConstantState {
        private ArrayList<ItemInfo> drawables;
        private int changingConfigurations = 0;
        private int opacity;
        private int totalDuration = 0;
        private int repeatCount = 1;

        private MotionConstantState() {
            drawables = new ArrayList<ItemInfo>();
        }

        private void addFrame(Drawable drawable, int duration) {
            drawables.add(new ItemInfo(drawable, duration));
            if (duration >= 0 && totalDuration >= 0) {
                totalDuration += duration;
            } else {
                totalDuration = -1;
            }

            changingConfigurations |= drawable.getChangingConfigurations();
            opacity = (drawables.size() > 1 ?
                    Drawable.resolveOpacity(opacity, drawable.getOpacity()) :
                    drawable.getOpacity());
        }

        private Drawable getFrame(int idx) {
            idx = (idx < 0 ? 0 : idx);
            if (drawables.size() <= idx) {
                return null;
            }

            return drawables.get(idx).drawable;
        }

        private int getFrameDuration(int idx) {
            idx = (idx < 0 ? 0 : idx);
            if (drawables.size() <= idx) {
                return 0;
            }

            return drawables.get(idx).duration;
        }

        private int getFrameCount() {
            return drawables.size();
        }

        @Override
        public int getChangingConfigurations() {
            return changingConfigurations;
        }

        @Override
        public Drawable newDrawable() {
            throw new UnsupportedOperationException(
                    "newDrawable is not supported");
        }

        @Override
        public Drawable newDrawable(Resources res) {
            throw new UnsupportedOperationException(
                    "newDrawable is not supported");
        }

        private int getOpacity() {
            return (drawables.size() > 1 ? opacity : PixelFormat.TRANSPARENT);
        }
    }

    private static class ItemInfo {
        private Drawable drawable;
        private int duration;

        private ItemInfo(Drawable drawable, int duration) {
            this.drawable = drawable;
            this.duration = duration;
        }
    }

    private class ChildCallback implements Drawable.Callback {
        @Override
        public void invalidateDrawable(Drawable who) {
            if (who == getCurrentFrame()) {
                invalidateSelf();
            }
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            if (who == getCurrentFrame()) {
                scheduleSelf(what, when);
            }
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            if (who == getCurrentFrame()) {
                unscheduleSelf(what);
            }
        }
    }

    private class ChildOnMotionEnd implements OnMotionEndListener {
        @Override
        public void onMotionEnd(MotionDrawable drawable) {
            if (drawable == getCurrentFrame()) {
                updateFrame();
            }
        }
    }
}
