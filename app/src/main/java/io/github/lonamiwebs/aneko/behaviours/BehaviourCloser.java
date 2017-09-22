package io.github.lonamiwebs.aneko.behaviours;


import android.graphics.Point;
import android.graphics.PointF;

public class BehaviourCloser extends Behaviour {
    @Override
    public PointF getTargetPosition(PointF p, PointF cur, Point display) {
        return p;
    }
}
