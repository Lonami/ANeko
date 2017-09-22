package io.github.lonamiwebs.aneko.behaviours;


import android.graphics.Point;
import android.graphics.PointF;

import java.util.Random;

public class BehaviourWhimsical extends Behaviour {
    private static final Random random = new Random();

    @Override
    public PointF getTargetPosition(PointF p, PointF cur, Point display) {
        float minWh2 = Math.min(display.x, display.y) / 2f;
        float r = random.nextFloat() * minWh2 + minWh2;
        float a = random.nextFloat() * 360;
        float nx = cur.x + r * (float) Math.cos(a);
        float ny = cur.y + r * (float) Math.sin(a);

        if (nx < 0) nx = -nx;
        else nx = nx >= display.x ? display.x * 2 - nx - 1 : nx;

        if (ny < 0) ny = -ny;
        else ny = ny >= display.y ? display.y * 2 - ny - 1 : ny;

        return new PointF(nx, ny);
    }
}
