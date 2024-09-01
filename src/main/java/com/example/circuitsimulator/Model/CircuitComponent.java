package com.example.circuitsimulator.Model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CircuitComponent {
    private static final ArrayList<Circle> NODES = new ArrayList<>();
    private static final ArrayList<CircuitComponent> COMPONENTS = new ArrayList<>();
    private static final Map<Circle, CircuitComponent> NODE_CONNECTIONS = new HashMap<>();

    private final Circle node1;
    private final Circle node2;
    private final ImageView imageView;
    private Image init;
    private Image alt;
    boolean rotate = false;

    private final ArrayList<Integer> current12 = new ArrayList<>();
    private final ArrayList<Integer> current21 = new ArrayList<>();
    private final DoubleProperty finalCurrent12 = new SimpleDoubleProperty(0);

    public CircuitComponent(ImageView view, Pane s, Image init, Image alt) {
        this.init = init;
        this.alt = alt;
        this.node1 = new Circle(0, 0, 5);
        node1.setLayoutX(view.getLayoutX());
        node1.setLayoutY(view.getLayoutY() + 71);
        this.node2 = new Circle(0, 0, 5);
        node2.setLayoutX(view.getLayoutX() + 200);
        node2.setLayoutY(view.getLayoutY() + 71);
        this.imageView = view;

        NODES.add(node1);
        NODES.add(node2);
        COMPONENTS.add(this);
        NODE_CONNECTIONS.put(node1, this);
        NODE_CONNECTIONS.put(node2, this);

        s.getChildren().addAll(this.imageView, this.node1, this.node2);
        finalCurrent12.set(0);
    }

    public Circle getNode1() {
        return node1;
    }

    public Circle getNode2() {
        return node2;
    }

    public double getX() {
        return imageView.getLayoutX();
    }

    public double getY() {
        return imageView.getLayoutY();
    }

    public static ArrayList<Circle> getNodes() {
        return NODES;
    }

    public static ArrayList<CircuitComponent> getConnectables() { return COMPONENTS; }

    public static Map<Circle, CircuitComponent> getNodeConnections() { return NODE_CONNECTIONS; }

    public ArrayList<Integer> getCurrent12() { return current12; }

    public ArrayList<Integer> getCurrent21() { return current21; }

    public void clearCurrent12() {
        current12.clear();
    }

    public void clearCurrent21() {
        current21.clear();
    }

    public void addCurrent12(int current) {
        current12.add(current);
    }

    public void addCurrent21(int current) {
        current21.add(current);
    }

    public void setPos(double x, double y) {
        double move_amt_x = x - imageView.getLayoutX();
        double move_amt_y = y - imageView.getLayoutY();

        imageView.setLayoutX(x);
        imageView.setLayoutY(y);

        node1.setLayoutX(node1.getLayoutX() + move_amt_x);
        node1.setLayoutY(node1.getLayoutY() + move_amt_y);

        node2.setLayoutX(node2.getLayoutX() + move_amt_x);
        node2.setLayoutY(node2.getLayoutY() + move_amt_y);
    }

    public void setFinalCurrent(double finalCurrent) {
        this.finalCurrent12.set(finalCurrent);
    }

    public double getFinalCurrent() { return this.finalCurrent12.get(); }

    public DoubleProperty currentProperty() { return finalCurrent12; }

    public void rotate() {
        Vector centre = new Vector((node1.getLayoutX() + node2.getLayoutX())/2, (node1.getLayoutY() + node2.getLayoutY())/2);

        imageView.setRotate(imageView.getRotate() + 90);
        double prev_x1 = node1.getLayoutX();
        node1.setLayoutX(centre.getY() + centre.getX() - node1.getLayoutY());
        node1.setLayoutY(centre.getY() - centre.getX() + prev_x1);

        double prev_x2 = node2.getLayoutX();
        node2.setLayoutX(centre.getY() + centre.getX() - node2.getLayoutY());
        node2.setLayoutY(centre.getY() - centre.getX() + prev_x2);

        rotate = !rotate;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void Highlight() {
        this.imageView.setImage(alt);
    }

    public void RemoveHighlight() {
        this.imageView.setImage(init);
    }

    public void setInit(Image init) {
        this.init = init;
    }

    public void setAlt(Image alt) {
        this.alt = alt;
    }

    public boolean isRotate() {
        return rotate;
    }

    public static void ClearAll() {
        NODES.clear();
        COMPONENTS.clear();
        NODE_CONNECTIONS.clear();
    }
}
