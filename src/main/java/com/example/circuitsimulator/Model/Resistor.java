package com.example.circuitsimulator.Model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Resistor extends CircuitComponent implements CircuitProperties {
    private double resistance;

    public Resistor(double resistance, ImageView view, Pane s, Image init, Image alt) {
        super(view, s, init, alt);
        this.resistance = resistance;
    }

    public void setResistance(double resistance) {
        this.resistance = resistance;
    }

    @Override
    public double getResistance() {
        return resistance;
    }

    @Override
    public double getEMF() {
        return 0;
    }
}
