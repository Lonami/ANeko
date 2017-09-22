package io.github.lonamiwebs.aneko.service;

import android.graphics.Point;
import android.graphics.PointF;

import java.util.Random;

import io.github.lonamiwebs.aneko.MotionDrawable;
import io.github.lonamiwebs.aneko.MotionParams;
import io.github.lonamiwebs.aneko.behaviours.Behaviour;

import static io.github.lonamiwebs.aneko.service.AnimationService.ANIMATION_INTERVAL;


class MotionState {
    PointF cur = new PointF();
    PointF target = new PointF();
    PointF vel = new PointF(); // pixels per sec
    Point display = new Point(1, 1); // screen dimensions

    MotionParams params;
    int alpha = 0xff;

    Behaviour behaviour = Behaviour.fromIndex(0);
    long lastBehaviourChanged = 0;

    String curState = null;

    boolean movingState = false;
    boolean stateChanged = false;
    boolean positionMoved = false;

    MotionDrawable.OnMotionEndListener onMotionEnd;

    final static Random random = new Random();

    MotionState(MotionDrawable.OnMotionEndListener onMotionEnd) {
        this.onMotionEnd = onMotionEnd;
    }

    void updateState() {
        stateChanged = false;
        positionMoved = false;

        PointF d = new PointF(target.x - cur.x, target.y - cur.y);
        float len = d.length();
        if (len <= params.getProximityDistance()) {
            if (movingState) {
                vel.set(0, 0);
                changeState(params.getInitialState());
            }
            return;
        }

        if (!movingState) {
            String nstate = params.getAwakeState();
            if (params.hasState(nstate)) {
                changeState(nstate);
            }
            return;
        }

        float interval = ANIMATION_INTERVAL / 1000f;

        float acceleration = params.getAcceleration();
        float maxVelocity = params.getMaxVelocity();
        float deaccelerationDistance = params.getDeaccelerationDistance();

        vel.x += acceleration * interval * d.x / len;
        vel.y += acceleration * interval * d.y / len;
        float vec = vel.length();
        float vmax = maxVelocity * Math.min((len + 1) / (deaccelerationDistance + 1), 1);
        if (vec > vmax) {
            float vr = vmax / vec;
            vel.x *= vr;
            vel.y *= vr;
        }

        cur.x += vel.x * interval;
        cur.y += vel.y * interval;
        positionMoved = true;

        changeToMovingState();
        return;
    }

    boolean checkWall() {
        if (!params.needCheckWall(curState)) {
            return false;
        }

        MotionDrawable drawable = getCurrentDrawable();
        float dw2 = drawable.getIntrinsicWidth() / 2f;
        float dh2 = drawable.getIntrinsicHeight() / 2f;

        MotionParams.WallDirection dir;
        float nx = cur.x;
        float ny = cur.y;
        if (cur.x >= 0 && cur.x < dw2) {
            nx = dw2;
            dir = MotionParams.WallDirection.LEFT;
        } else if (cur.x <= display.x && cur.x > display.x - dw2) {
            nx = display.x - dw2;
            dir = MotionParams.WallDirection.RIGHT;
        } else if (cur.y >= 0 && cur.y < dh2) {
            ny = dh2;
            dir = MotionParams.WallDirection.UP;
        } else if (cur.y <= display.y && cur.y > display.y - dh2) {
            ny = display.y - dh2;
            dir = MotionParams.WallDirection.DOWN;
        } else {
            return false;
        }

        String nstate = params.getWallState(dir);
        if (!params.hasState(nstate)) {
            return false;
        }

        cur.x = target.x = nx;
        cur.y = target.y = ny;
        changeState(nstate);

        return true;
    }

    boolean updateMovingState() {
        if (!params.needCheckMove(curState)) {
            return false;
        }

        float dx = target.x - cur.x;
        float dy = target.y - cur.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= params.getProximityDistance()) {
            return false;
        }

        changeToMovingState();
        return true;
    }

    void setParams(MotionParams _params) {
        String nstate = _params.getInitialState();
        if (!_params.hasState(nstate)) {
            throw new IllegalArgumentException(
                    "Initial State does not exist");
        }

        params = _params;

        changeState(nstate);
        movingState = false;
    }

    void changeState(String state) {
        if (state.equals(curState)) {
            return;
        }

        curState = state;
        stateChanged = true;
        movingState = false;
        getCurrentDrawable().setOnMotionEndListener(onMotionEnd);
    }

    boolean changeToNextState() {
        String nextState = params.getNextState(curState);
        if (nextState == null) {
            return false;
        }

        changeState(nextState);
        return true;
    }

    void changeToMovingState() {
        int dir = (int) (Math.atan2(vel.y, vel.x) * 4 / Math.PI + 8.5) % 8;
        MotionParams.MoveDirection dirs[] = {
                MotionParams.MoveDirection.RIGHT,
                MotionParams.MoveDirection.DOWN_RIGHT,
                MotionParams.MoveDirection.DOWN,
                MotionParams.MoveDirection.DOWN_LEFT,
                MotionParams.MoveDirection.LEFT,
                MotionParams.MoveDirection.UP_LEFT,
                MotionParams.MoveDirection.UP,
                MotionParams.MoveDirection.UP_RIGHT
        };

        String nstate = params.getMoveState(dirs[dir]);
        if (!params.hasState(nstate)) {
            return;
        }

        changeState(nstate);
        movingState = true;
    }

    void setBehaviour(Behaviour b) {
        behaviour = b;
        lastBehaviourChanged = 0;
    }

    void setTargetPosition(float x, float y) {
        target = behaviour.getTargetPosition(new PointF(x, y), cur, display);
    }

    void forceStop() {
        setTargetPosition(cur.x, cur.y);
        vel.set(0, 0);
    }

    boolean isStateChanged() {
        return stateChanged;
    }

    boolean isPositionMoved() {
        return positionMoved;
    }

    MotionDrawable getCurrentDrawable() {
        return params.getDrawable(curState);
    }

    Point getPosition() {
        MotionDrawable drawable = getCurrentDrawable();
        return new Point((int) (cur.x - drawable.getIntrinsicWidth() / 2f),
                (int) (cur.y - drawable.getIntrinsicHeight() / 2f));
    }
}
