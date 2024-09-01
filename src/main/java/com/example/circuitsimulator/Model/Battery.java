package com.example.circuitsimulator.Model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Battery extends CircuitComponent implements CircuitProperties {
    private double emf;

    public Battery(double emf, ImageView view, Pane s, Image init, Image alt) {
        super(view, s, init, alt);
        this.emf = emf;
    }

    public void setEmf(double emf) {
        this.emf = emf;
    }

    @Override
    public double getResistance() {
        return 0;
    }

    @Override
    public double getEMF() {
        return emf;
    }
}
