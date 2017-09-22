package io.github.lonamiwebs.aneko.behaviours;

import android.graphics.Point;
import android.graphics.PointF;


public abstract class Behaviour {
    /**
     * Calculates the new target position for the neko.
     * @param p desired target position
     * @param cur current position
     * @param display display dimensions
     * @return the new position
     */
    public abstract PointF getTargetPosition(PointF p, PointF cur, Point display);

    public static Behaviour fromIndex(int index) {
        switch (index) {
            case 0:
                return new BehaviourCloser();
            case 1:
                return new BehaviourFurther();
            case 2:
            default:
                return new BehaviourWhimsical();
        }
    }

    public static Behaviour fromName(String name) {
        switch (name) {
            case "closer":
                return new BehaviourCloser();
            case "further":
                return new BehaviourFurther();
            case "whimsical":
            default:
                return new BehaviourWhimsical();
        }
    }
}
