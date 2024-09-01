package com.example.circuitsimulator.Model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Wire extends CircuitComponent implements CircuitProperties {
    double length;

    public Wire(ImageView view, Pane s, Image init, Image alt) {
        super(view, s, init, alt);
        length = 200;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    @Override
    public double getResistance() {
        return 0;
    }

    @Override
    public double getEMF() {
        return 0;
    }
}