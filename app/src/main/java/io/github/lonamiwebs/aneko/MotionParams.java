package io.github.lonamiwebs.aneko;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;

public class MotionParams {
    public enum MoveDirection {
        UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
    }

    public enum WallDirection {
        UP, DOWN, LEFT, RIGHT
    }

    private static final String TAG_MOTION_PARAMS = "motion-params";
    private static final String TAG_MOTION = "motion";
    private static final String TAG_ITEM = "item";
    private static final String TAG_REPEAT_ITEM = "repeat-item";

    private static final String ATTR_ACCELERATION = "acceleration";
    private static final String ATTR_MAX_VELOCITY = "maxVelocity";
    private static final String ATTR_DEACCELERATION = "deaccelerationDistance";
    private static final String ATTR_PROXIMITY = "proximityDistance";

    private static final String ATTR_INITIAL_STATE = "initialState";
    private static final String ATTR_AWAKE_STATE = "awakeState";
    private static final String ATTR_MOVE_STATE_PREFIX = "moveStatePrefix";
    private static final String ATTR_WALL_STATE_PREFIX = "wallStatePrefix";

    private static final String ATTR_STATE = "state";
    private static final String ATTR_DURATION = "duration";
    private static final String ATTR_NEXT_STATE = "nextState";
    private static final String ATTR_CHECK_WALL = "checkWall";
    private static final String ATTR_CHECK_MOVE = "checkMove";

    private static final String ATTR_ITEM_DRAWABLE = "drawable";
    private static final String ATTR_ITEM_DURATION = "duration";
    private static final String ATTR_ITEM_REPEAT_COUNT = "repeatCount";

    private static final int DEF_ACCELERATION = 160; // dp per sec^2
    private static final int DEF_MAX_VELOCITY = 100; // dp per sec
    private static final int DEF_DEACCELERATE_DISTANCE = 100; // dp
    private static final int DEF_PROXIMITY_DISTANCE = 10; // dp

    private static final String DEF_INITIAL_STATE = "stop";
    private static final String DEF_AWAKE_STATE = "awake";
    private static final String DEF_MOVE_STATE_PREFIX = "move";
    private static final String DEF_WALL_STATE_PREFIX = "wall";

    private float acceleration;
    private float maxVelocity;
    private float deaccelerationDistance;
    private float proximityDistance;

    private String initialState;
    private String awakeState;
    private String moveStatePrefix;
    private String wallStatePrefix;

    private HashMap<String, Motion> motions = new HashMap<String, Motion>();

    private static class Motion {
        private String name;
        private String nextState;

        private boolean checkMove;
        private boolean checkWall;

        private MotionDrawable items;
    }

    public MotionParams(Context context, Resources res, int resid) {
        XmlPullParser xml = res.getXml(resid);
        AttributeSet attrs = Xml.asAttributeSet(xml);
        try {
            parseXml(res, xml, attrs);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Load failed: " + res.getResourceName(resid), e);
        }
    }

    public float getAcceleration() {
        return acceleration;
    }

    public float getMaxVelocity() {
        return maxVelocity;
    }

    public float getDeaccelerationDistance() {
        return deaccelerationDistance;
    }

    public float getProximityDistance() {
        return proximityDistance;
    }

    public boolean hasState(String state) {
        return motions.containsKey(state);
    }

    public String getInitialState() {
        return initialState;
    }

    public String getAwakeState() {
        return awakeState;
    }

    public String getMoveState(MoveDirection dir) {
        return moveStatePrefix + enumToString(dir);
    }

    public String getWallState(WallDirection dir) {
        return wallStatePrefix + enumToString(dir);
    }

    private static String enumToString(final WallDirection dir) {
        switch (dir) {
            case UP:
                return "Up";
            case DOWN:
                return "Down";
            case LEFT:
                return "Left";
            case RIGHT:
                return "Right";
            default:
                return "";
        }
    }

    private static String enumToString(final MoveDirection dir) {
        switch (dir) {
            case UP:
                return "Up";
            case DOWN:
                return "Down";
            case LEFT:
                return "Left";
            case RIGHT:
                return "Right";
            case UP_LEFT:
                return "UpLeft";
            case UP_RIGHT:
                return "UpRight";
            case DOWN_LEFT:
                return "DownLeft";
            case DOWN_RIGHT:
                return "DownRight";
            default:
                return "";
        }
    }

    public String getNextState(String state) {
        Motion motion = motions.get(state);
        return (motion != null ? motion.nextState : null);
    }

    public boolean needCheckMove(String state) {
        Motion motion = motions.get(state);
        return (motion != null ? motion.checkMove : false);
    }

    public boolean needCheckWall(String state) {
        Motion motion = motions.get(state);
        return (motion != null ? motion.checkWall : false);
    }

    public MotionDrawable getDrawable(String state) {
        Motion motion = motions.get(state);
        return (motion != null ? motion.items : null);
    }

    private void parseXml(Resources res, XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_MOTION_PARAMS.equals(name)) {
                parseMotionParams(res, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }
    }

    private void parseMotionParams(Resources res,
                                   XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        float density = res.getDisplayMetrics().density;
        acceleration = density * attrs.getAttributeIntValue(
                null, ATTR_ACCELERATION, DEF_ACCELERATION);

        deaccelerationDistance = density * attrs.getAttributeIntValue(
                null, ATTR_DEACCELERATION, DEF_DEACCELERATE_DISTANCE);

        maxVelocity = density * attrs.getAttributeIntValue(
                null, ATTR_MAX_VELOCITY, DEF_MAX_VELOCITY);

        proximityDistance = density * attrs.getAttributeIntValue(
                null, ATTR_PROXIMITY, DEF_PROXIMITY_DISTANCE);

        initialState = attrs.getAttributeValue(null, ATTR_INITIAL_STATE);
        initialState = (initialState != null ? initialState : DEF_INITIAL_STATE);

        awakeState = attrs.getAttributeValue(null, ATTR_AWAKE_STATE);
        awakeState = (awakeState != null ? awakeState : DEF_AWAKE_STATE);

        moveStatePrefix = attrs.getAttributeValue(null, ATTR_MOVE_STATE_PREFIX);
        moveStatePrefix = (moveStatePrefix != null ? moveStatePrefix : DEF_MOVE_STATE_PREFIX);

        wallStatePrefix = attrs.getAttributeValue(null, ATTR_WALL_STATE_PREFIX);
        wallStatePrefix = (wallStatePrefix != null ? wallStatePrefix : DEF_WALL_STATE_PREFIX);

        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_MOTION.equals(name)) {
                parseMotion(res, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }
    }

    private void parseMotion(Resources res, XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        Motion motion = new Motion();

        motion.name = attrs.getAttributeValue(null, ATTR_STATE);
        if (motion.name == null) {
            throw new IllegalArgumentException(
                    "state is not specified: " + attrs.getPositionDescription());
        }

        int duration = attrs.getAttributeIntValue(null, ATTR_DURATION, -1);
        motion.nextState = attrs.getAttributeValue(null, ATTR_NEXT_STATE);
        motion.checkMove = attrs.getAttributeBooleanValue(null, ATTR_CHECK_MOVE, false);
        motion.checkWall = attrs.getAttributeBooleanValue(null, ATTR_CHECK_WALL, false);

        motion.items = new MotionDrawable();

        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_ITEM.equals(name)) {
                parseItem(res, motion.items, xml, attrs);
            } else if (TAG_REPEAT_ITEM.equals(name)) {
                parseRepeatItem(res, motion.items, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }

        motion.items.setTotalDuration(duration);
        motion.items.setRepeatCount(1);

        motions.put(motion.name, motion);
    }

    private void parseItem(Resources res, MotionDrawable items,
                           XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        int drawable = attrs.getAttributeResourceValue(null, ATTR_ITEM_DRAWABLE, 0);
        int duration = attrs.getAttributeIntValue(null, ATTR_ITEM_DURATION, -1);

        items.addFrame(res.getDrawable(drawable), duration);
    }

    private void parseRepeatItem(Resources res, MotionDrawable items,
                                 XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        int duration = attrs.getAttributeIntValue(null, ATTR_ITEM_DURATION, -1);
        int repeat = attrs.getAttributeIntValue(null, ATTR_ITEM_REPEAT_COUNT, -1);
        MotionDrawable dr = new MotionDrawable();

        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_ITEM.equals(name)) {
                parseItem(res, dr, xml, attrs);
            } else if (TAG_REPEAT_ITEM.equals(name)) {
                parseRepeatItem(res, dr, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }

        dr.setTotalDuration(duration);
        dr.setRepeatCount(repeat);
        items.addFrame(dr, -1);
    }
}
