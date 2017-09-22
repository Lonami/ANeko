package io.github.lonamiwebs.aneko.behaviours;


import android.graphics.Point;
import android.graphics.PointF;

import java.util.Random;

public class BehaviourFurther extends Behaviour {
    private static final Random random = new Random();

    @Override
    public PointF getTargetPosition(PointF p, PointF cur, Point display) {
        float dx = display.x / 2f - p.x;
        float dy = display.y / 2f - p.y;
        if (dx == 0 && dy == 0) {
            float ang = random.nextFloat() * (float) Math.PI * 2;
            dx = (float) Math.cos(ang);
            dy = (float) Math.sin(ang);
        }
        if (dx < 0) {
            dx = -dx;
            dy = -dy;
        }

        PointF e1, e2;
        if (dy > dx * display.y / display.x || dy < -dx * display.y / display.x) {
            float dxdy = dx / dy;
            e1 = new PointF((display.x - display.y * dxdy) / 2f, 0);
            e2 = new PointF((display.x + display.y * dxdy) / 2f, display.y);
        } else {
            float dydx = dy / dx;
            e1 = new PointF(0, (display.y - display.x * dydx) / 2f);
            e2 = new PointF(display.x, (display.y + display.x * dydx) / 2f);
        }

        double d1 = Math.hypot(e1.x - p.x, e1.y - p.y);
        double d2 = Math.hypot(e2.x - p.x, e2.y - p.y);
        PointF e = (d1 > d2 ? e1 : e2);

        float r = 0.9f + random.nextFloat() * 0.1f;
        return new PointF(e.x * r + p.x * (1 - r), e.y * r + p.y * (1 - r));
    }
}
