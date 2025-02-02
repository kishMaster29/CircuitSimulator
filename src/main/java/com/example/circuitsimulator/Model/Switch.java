package com.example.circuitsimulator.Model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Switch extends CircuitComponent implements CircuitProperties {
    private boolean open;

    public Switch(ImageView view, Pane s, Image init, Image alt) {
        super(view, s, init, alt);
        open = true;
    }

    @Override
    public String get_name() {
        return "Switch";
    }

    public void toggleSwitch() {
        open = !open;
    }

    public boolean isSwitchOpen() {
        return open;
    }

    @Override
    public double getResistance() {
        if (!open) return 0;
        else return 1000000.0;
    }

    @Override
    public double getEMF() {
        return 0;
    }
}
