package com.example.circuitsimulator.Model;

import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class Arrow extends Group {

    private final Line line;

    public Arrow() {
        this(new Line(), new Line(), new Line());
    }

    private static final double arrowLength = 10;
    private static final double arrowWidth = 10;

    private Arrow(Line line, Line arrow1, Line arrow2) {
        super(line, arrow1, arrow2);
        this.line = line;
        InvalidationListener updater = _ -> {
            double ex = getEndX();
            double ey = getEndY();
            double sx = getStartX();
            double sy = getStartY();

            arrow1.setEndX(ex);
            arrow1.setEndY(ey);
            arrow2.setEndX(ex);
            arrow2.setEndY(ey);

            line.setStroke(new Color(1, 0, 0, 1));
            arrow1.setStroke(new Color(1, 0, 0, 1));
            arrow2.setStroke(new Color(1, 0, 0, 1));

            line.setStrokeWidth(5);
            arrow1.setStrokeWidth(5);
            arrow2.setStrokeWidth(5);

            if (ex == sx && ey == sy) {
                // arrow parts of length 0
                arrow1.setStartX(ex);
                arrow1.setStartY(ey);
                arrow2.setStartX(ex);
                arrow2.setStartY(ey);
            } else {
                double hypot = Math.hypot(sx - ex, sy - ey);
                double factor = arrowLength / hypot;
                double factorO = arrowWidth / hypot;

                // part in direction of main line
                double dx = (sx - ex) * factor;
                double dy = (sy - ey) * factor;

                // part orthogonal to main line
                double ox = (sx - ex) * factorO;
                double oy = (sy - ey) * factorO;

                arrow1.setStartX(ex + dx - oy);
                arrow1.setStartY(ey + dy + ox);
                arrow2.setStartX(ex + dx + oy);
                arrow2.setStartY(ey + dy - ox);
            }
        };

        // add updater to properties
        startXProperty().addListener(updater);
        startYProperty().addListener(updater);
        endXProperty().addListener(updater);
        endYProperty().addListener(updater);
        updater.invalidated(null);
    }

    // start/end properties

    public final void setStartX(double value) {
        line.setStartX(value);
    }

    public final double getStartX() {
        return line.getStartX();
    }

    public final DoubleProperty startXProperty() {
        return line.startXProperty();
    }

    public final void setStartY(double value) {
        line.setStartY(value);
    }

    public final double getStartY() {
        return line.getStartY();
    }

    public final DoubleProperty startYProperty() {
        return line.startYProperty();
    }

    public final void setEndX(double value) {
        line.setEndX(value);
    }

    public final double getEndX() {
        return line.getEndX();
    }

    public final DoubleProperty endXProperty() {
        return line.endXProperty();
    }

    public final void setEndY(double value) {
        line.setEndY(value);
    }

    public final double getEndY() {
        return line.getEndY();
    }

    public final DoubleProperty endYProperty() {
        return line.endYProperty();
    }

}
