package com.dsarpg.ui;

/** A simple rectangle used for hit-testing clickable canvas elements. */
public final class UiRect {

    public final double x;
    public final double y;
    public final double w;
    public final double h;

    public UiRect(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public double centerX() {
        return x + w / 2.0;
    }

    public double centerY() {
        return y + h / 2.0;
    }
}
