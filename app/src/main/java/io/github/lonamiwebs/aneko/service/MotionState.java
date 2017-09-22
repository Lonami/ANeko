package io.github.lonamiwebs.aneko.service;

import android.graphics.Point;
import android.graphics.PointF;

import java.util.Random;

import io.github.lonamiwebs.aneko.MotionDrawable;
import io.github.lonamiwebs.aneko.MotionParams;
import io.github.lonamiwebs.aneko.behaviours.Behaviour;

import static io.github.lonamiwebs.aneko.service.AnimationService.ANIMATION_INTERVAL;


class MotionState {
    float curX = 0;
    float curY = 0;
    float targetX = 0;
    float targetY = 0;
    float vx = 0; // pixels per sec
    float vy = 0; // pixels per sec

    int displayWidth = 1;
    int displayHeight = 1;

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

        float dx = targetX - curX;
        float dy = targetY - curY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= params.getProximityDistance()) {
            if (movingState) {
                vx = 0;
                vy = 0;
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

        vx += acceleration * interval * dx / len;
        vy += acceleration * interval * dy / len;
        float vec = (float) Math.sqrt(vx * vx + vy * vy);
        float vmax = maxVelocity * Math.min((len + 1) / (deaccelerationDistance + 1), 1);
        if (vec > vmax) {
            float vr = vmax / vec;
            vx *= vr;
            vy *= vr;
        }

        curX += vx * interval;
        curY += vy * interval;
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
        float nx = curX;
        float ny = curY;
        if (curX >= 0 && curX < dw2) {
            nx = dw2;
            dir = MotionParams.WallDirection.LEFT;
        } else if (curX <= displayWidth && curX > displayWidth - dw2) {
            nx = displayWidth - dw2;
            dir = MotionParams.WallDirection.RIGHT;
        } else if (curY >= 0 && curY < dh2) {
            ny = dh2;
            dir = MotionParams.WallDirection.UP;
        } else if (curY <= displayHeight && curY > displayHeight - dh2) {
            ny = displayHeight - dh2;
            dir = MotionParams.WallDirection.DOWN;
        } else {
            return false;
        }

        String nstate = params.getWallState(dir);
        if (!params.hasState(nstate)) {
            return false;
        }

        curX = targetX = nx;
        curY = targetY = ny;
        changeState(nstate);

        return true;
    }

    boolean updateMovingState() {
        if (!params.needCheckMove(curState)) {
            return false;
        }

        float dx = targetX - curX;
        float dy = targetY - curY;
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
        int dir = (int) (Math.atan2(vy, vx) * 4 / Math.PI + 8.5) % 8;
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

    void setDisplaySize(int w, int h) {
        displayWidth = w;
        displayHeight = h;
    }

    void setBehaviour(Behaviour b) {
        behaviour = b;
        lastBehaviourChanged = 0;
    }

    void setCurrentPosition(float x, float y) {
        curX = x;
        curY = y;
    }

    void setTargetPosition(float x, float y) {
        PointF target = behaviour.getTargetPosition(
                new PointF(x, y), new PointF(curX, curY), new PointF(displayWidth, displayHeight));

        targetX = target.x;
        targetY = target.y;
    }

    void setTargetPositionDirect(float x, float y) {
        targetX = x;
        targetY = y;
    }

    void forceStop() {
        setTargetPosition(curX, curY);
        vx = 0;
        vy = 0;
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
        return new Point((int) (curX - drawable.getIntrinsicWidth() / 2f),
                (int) (curY - drawable.getIntrinsicHeight() / 2f));
    }
}
