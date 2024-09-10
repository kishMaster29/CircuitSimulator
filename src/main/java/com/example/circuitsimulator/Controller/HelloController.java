package com.example.circuitsimulator.Controller;

import com.example.circuitsimulator.Model.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloController {
    @FXML
    public Pane SimSpace;
    @FXML
    public AnchorPane CanvasPane;
    @FXML
    public ListView<ImageView> ComponentList;
    @FXML
    public SplitPane BasePane;
    @FXML
    public AnchorPane InfoPane;
    @FXML
    public Label ErrorText;
    @FXML
    public TextField InputField;
    @FXML
    public Button ok;
    @FXML
    public Button cancel;
    @FXML
    public CheckBox closeToggle;
    @FXML
    public Label NameLabel;
    @FXML
    public Label Info1;
    @FXML
    public Label Info2;
    @FXML
    public Label Info3;
    @FXML
    public TabPane PaneTab;

    private final String[] file_name = {"resistor.png", "battery.png", "load.png", "open_switch.png", "wire.png", "closed_switch.png"};
    private final String[] highlight_file_name = {"select_resistor.png", "select_battery.png", "select_load.png", "select_open_switch.png", "select_wire.png", "select_closed_switch.png"};
    public static final ArrayList<Image> canvas_images = new ArrayList<>();
    public static final ArrayList<Image> highlight_canvas_images = new ArrayList<>();
    private CircuitComponent selected_component = null;
    private Line lineHighlight;
    private final Map<CircuitComponent, Line> wire_components = new HashMap<>();
    private final Map<CircuitComponent, Circle> load_components = new HashMap<>();

    private int drag_delta_x;
    private int drag_delta_y;
    private boolean dragged = false;

    private double typed_val;

    @FXML
    public void initialize() {

        for (String s : file_name) {
            Image component = new Image(s);
            Image img = new Image(s, 200, 0, true, true);
            canvas_images.add(img);
            ImageView imageView = new ImageView(component);
            imageView.fitWidthProperty().bind(ComponentList.widthProperty().subtract(20));
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
            ComponentList.getItems().add(imageView);
        }

        for (String s : highlight_file_name) {
            Image img = new Image(s, 200, 0, true, true);
            highlight_canvas_images.add(img);
        }

        ComponentList.setOnDragDetected(_ -> {
            if (selected_component != null) {
                if (selected_component instanceof Wire) {
                    SimSpace.getChildren().remove(lineHighlight);
                } else {
                    selected_component.RemoveHighlight();
                }
            }
            selected_component = null;
            HideInspector();
            Dragboard dragboard = ComponentList.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putImage(canvas_images.get(ComponentList.getSelectionModel().getSelectedIndex()));
            content.putString(String.valueOf(ComponentList.getSelectionModel().getSelectedIndex()));
            dragboard.setContent(content);
        });

        SimSpace.setOnDragOver(dragEvent -> {
            if (dragEvent.getDragboard().hasImage() && dragEvent.getGestureSource() != SimSpace) {
                dragEvent.acceptTransferModes(TransferMode.COPY);
            }

            dragEvent.consume();
        });

        SimSpace.setOnMousePressed(mouseEvent -> {
            boolean flag = true;
            for (CircuitComponent c : CircuitComponent.getConnectables()) {
                if (c.getImageView().isPressed()) {
                    flag = false;
                }
            }
            for (Line c : wire_components.values()) {
                if (c.isPressed()) {
                    flag = false;
                }
            }

            if (flag) {
                if (selected_component != null) {
                    if (selected_component instanceof Wire) {
                        SimSpace.getChildren().remove(lineHighlight);
                    } else {
                        selected_component.RemoveHighlight();
                    }
                }
                selected_component = null;
                HideInspector();
            }
        });

        SimSpace.setOnDragDropped(dragEvent -> {
            Image img = dragEvent.getDragboard().getImage();
            ImageView view = new ImageView(img);
            double pos_x = dragEvent.getSceneX() - (img.getWidth()/2);
            double pos_y = dragEvent.getSceneY() - (img.getHeight()/2);
            view.setCache(true);
            view.setLayoutX(roundNearest25(pos_x));
            view.setLayoutY(roundNearest25(pos_y));

            int index = Integer.parseInt(dragEvent.getDragboard().getString());
            CircuitComponent c;

            if (index == 0) {
                c = new Resistor(1, view, SimSpace, img, highlight_canvas_images.get(index));
                setupDrag(view, view, c);
            } else if (index == 1) {
                c = new Battery(1, view, SimSpace, img, highlight_canvas_images.get(index));
                setupDrag(view, view, c);
            } else if (index == 2) {
                c = new Load(1, view, SimSpace, img, highlight_canvas_images.get(index));
                setupDrag(view, view, c);
                Circle bulb_light = new Circle();
                bulb_light.setRadius(45);
                ObjectBinding<Color> colorBinding = Bindings.createObjectBinding(() -> Color.hsb(50, Math.max(0, Math.min(1, (Math.abs(c.getFinalCurrent()))/10)), 1), c.currentProperty());
                bulb_light.fillProperty().bind(colorBinding);
                bulb_light.layoutXProperty().bind(view.layoutXProperty().add(102));
                bulb_light.layoutYProperty().bind(view.layoutYProperty().add(72));
                load_components.put(c, bulb_light);
                SimSpace.getChildren().add(bulb_light);
                bulb_light.toBack();
            } else if (index == 3) {
                c = new Switch(view, SimSpace, img, highlight_canvas_images.get(index));
                setupDrag(view, view, c);
            } else if (index == 4) {
                view.setVisible(false);
                c = new Wire(view, SimSpace, img, highlight_canvas_images.get(index));

                // Instead of ImageView, Wire is treated as a Line to facilitate increase and decrease of length
                Line line = new Line();
                line.startXProperty().bind(c.getNode1().layoutXProperty());
                line.startYProperty().bind(c.getNode1().layoutYProperty());
                line.endXProperty().bind(c.getNode2().layoutXProperty());
                line.endYProperty().bind(c.getNode2().layoutYProperty());
                line.setStrokeWidth(4.5);
                setupDrag(line, view, c);
                SimSpace.getChildren().add(line);
                line.toBack();

                wire_components.put(c, line);

            } else if (index == 5) {
                c = new Switch(view, SimSpace, img, highlight_canvas_images.get(index));
                Switch aSwitch = (Switch) c;
                aSwitch.toggleSwitch();
                setupDrag(view, view, c);
            } else {
                c = null;
            }
//            Arrow arrow = new Arrow();
//            arrow.setStartX(c.getNode1().getLayoutX());
//            arrow.setStartY(c.getNode1().getLayoutY());
//            arrow.setEndX(c.getNode2().getLayoutX());
//            arrow.setEndY(c.getNode2().getLayoutY());
//            SimSpace.getChildren().add(arrow);
//            arrow.toFront();

            SimHandler.updateConnections(c);
            dragEvent.consume();
        });

        BasePane.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.R) && selected_component != null) {
                selected_component.rotate();
                SimHandler.updateConnections(selected_component);
            } else if (keyEvent.getCode().equals(KeyCode.P) && selected_component instanceof Wire wire) {
                Circle c1 = selected_component.getNode1();
                Circle c2 = selected_component.getNode2();

                ArrayList<Double> arr = pointTowards(selected_component);
                double delX = arr.get(0);
                double delY = arr.get(1);

                c2.setLayoutX(c2.getLayoutX() + delX * 25);
                c2.setLayoutY(c2.getLayoutY() + delY * 25);

                c1.setLayoutX(c1.getLayoutX() - delX * 25);
                c1.setLayoutY(c1.getLayoutY() - delY * 25);

                SimHandler.updateConnections(selected_component);
                wire.setLength(wire.getLength() + 50);
            } else if (keyEvent.getCode().equals(KeyCode.O) && selected_component instanceof Wire wire) {
                if (wire.getLength() > 50) {
                    Circle c1 = selected_component.getNode1();
                    Circle c2 = selected_component.getNode2();

                    ArrayList<Double> arr = pointTowards(selected_component);
                    double delX = arr.get(0);
                    double delY = arr.get(1);

                    c2.setLayoutX(c2.getLayoutX() - delX * 25);
                    c2.setLayoutY(c2.getLayoutY() - delY * 25);

                    c1.setLayoutX(c1.getLayoutX() + delX * 25);
                    c1.setLayoutY(c1.getLayoutY() + delY * 25);
                    SimHandler.updateConnections(selected_component);
                    wire.setLength(wire.getLength() - 50);
                }
            } else if (keyEvent.getCode().equals(KeyCode.D) && selected_component != null) {
                CircuitComponent.getConnectables().remove(selected_component);
                CircuitComponent.getNodes().remove(selected_component.getNode1());
                CircuitComponent.getNodes().remove(selected_component.getNode2());
                CircuitComponent.getNodeConnections().remove(selected_component.getNode1());
                CircuitComponent.getNodeConnections().remove(selected_component.getNode2());
                SimSpace.getChildren().remove(selected_component.getImageView());
                if (selected_component instanceof Wire) {
                    SimSpace.getChildren().remove(wire_components.get(selected_component));
                    SimSpace.getChildren().remove(lineHighlight);
                } else if (selected_component instanceof Load) {
                    SimSpace.getChildren().remove(load_components.get(selected_component));
                }
                SimSpace.getChildren().remove(selected_component.getNode1());
                SimSpace.getChildren().remove(selected_component.getNode2());
                SimHandler.removeFromConnections(selected_component);
                SimHandler.generateCycles();
                SimHandler.addCurrents();
                SimHandler.Calculate();
                selected_component = null;
                HideInspector();
            }
        });

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (selected_component instanceof Resistor resistor) {
                if (text.equals("Resistance: " + resistor.getResistance() + " Ohms")) {
                    ok.setDisable(true);
                    cancel.setDisable(true);
                    return change;
                } else if (text.matches("Resistance: [0-9.]* Ohms")) {
                    ok.setVisible(true);
                    cancel.setVisible(true);
                    cancel.setDisable(false);

                    Pattern pattern = Pattern.compile("(?<!\\S)\\d+(\\.\\d+)?(?!\\S)");
                    Matcher matcher = pattern.matcher(text);

                    if (matcher.find()) {
                        double val = Double.parseDouble(matcher.group());
                        if (val != resistor.getResistance() && val != 0) {
                            ok.setDisable(false);
                            typed_val = val;
                        }
                    } else {
                        ok.setDisable(true);
                    }
                    return change;
                }
            }
            else if (selected_component instanceof Battery battery) {
                if (text.equals("EMF: " + battery.getEMF()+ " Volts")) {
                    ok.setDisable(true);
                    cancel.setDisable(true);
                    return change;
                } else if (text.matches("EMF: [0-9.]* Volts")) {
                    ok.setVisible(true);
                    cancel.setVisible(true);
                    cancel.setDisable(false);

                    Pattern pattern = Pattern.compile("(?<!\\S)\\d+(\\.\\d+)?(?!\\S)");
                    Matcher matcher = pattern.matcher(text);

                    if (matcher.find()) {
                        double val = Double.parseDouble(matcher.group());
                        if (val != battery.getEMF()) {
                            ok.setDisable(false);
                            typed_val = val;
                        }
                    } else {
                        ok.setDisable(true);
                    }
                    return change;
                }
            }
            else if (selected_component instanceof Load load) {
                if (text.equals("Load: " + load.getResistance() + " Ohms")) {
                    ok.setDisable(true);
                    cancel.setDisable(true);
                    return change;
                } else if (text.matches("Load: [0-9.]* Ohms")) {
                    ok.setVisible(true);
                    cancel.setVisible(true);
                    cancel.setDisable(false);

                    Pattern pattern = Pattern.compile("(?<!\\S)\\d+(\\.\\d+)?(?!\\S)");
                    Matcher matcher = pattern.matcher(text);

                    if (matcher.find()) {
                        double val = Double.parseDouble(matcher.group());
                        if (val != load.getResistance() && val != 0) {
                            ok.setDisable(false);
                            typed_val = val;
                        }
                    } else {
                        ok.setDisable(true);
                    }
                    return change;
                }
            }
            else if (selected_component instanceof Wire) {
                return change;
            }
            else if (selected_component instanceof Switch) {
                return change;
            }

            return null;
        };
        InputField.setTextFormatter(new TextFormatter<>(filter));
    }

    private void setupDrag(Node node, Node node2, CircuitComponent c) {
        node.setOnMousePressed(mouseEvent -> {
            if (selected_component != null) {
                if (selected_component instanceof Wire) {
                    SimSpace.getChildren().remove(lineHighlight);
                } else {
                    selected_component.RemoveHighlight();
                }
            }
            selected_component = c;
            HighlightComponent(node, c);

            ShowLabels();
            SetText();

            dragged = true;
            drag_delta_x = (int) (node2.getLayoutX() - mouseEvent.getSceneX());
            drag_delta_y = (int) (node2.getLayoutY() - mouseEvent.getSceneY());
        });

        node.setOnMouseDragged(mouseEvent -> c.setPos(mouseEvent.getSceneX() + drag_delta_x, mouseEvent.getSceneY() + drag_delta_y));

        node.setOnMouseReleased(_ -> {
            if (dragged) {
                c.setPos(roundNearest25(c.getX()), roundNearest25(c.getY()));
                SimHandler.updateConnections(c);
                dragged = false;
            }
        });

        node.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 1 && !dragged) {
                if (selected_component != null) {
                    if (selected_component instanceof Wire) {
                        SimSpace.getChildren().remove(lineHighlight);
                    } else {
                        selected_component.RemoveHighlight();
                    }
                }
                HighlightComponent(node, c);
                c.Highlight();
                selected_component = c;
                ShowLabels();
                SetText();
            }
        });
    }

    private void HighlightComponent(Node node, CircuitComponent c) {
        if (node instanceof Line) {
            Line line = new Line();
            line.setStrokeWidth(10);
            line.setStroke(new Color(0, 1, 1, 1));
            line.startXProperty().bind(c.getNode1().layoutXProperty());
            line.startYProperty().bind(c.getNode1().layoutYProperty());
            line.endXProperty().bind(c.getNode2().layoutXProperty());
            line.endYProperty().bind(c.getNode2().layoutYProperty());
            SimSpace.getChildren().add(line);
            line.toBack();
            lineHighlight = line;
        } else {
            c.Highlight();
        }
    }

    public void okPress() {
        if (selected_component instanceof Resistor resistor) {
            resistor.setResistance(typed_val);
            InputField.setText("Resistance: " + resistor.getResistance() + " Ohms");
        } else if (selected_component instanceof Battery battery) {
            battery.setEmf(typed_val);
            InputField.setText("EMF: " + battery.getEMF() + " Volts");
        } else if (selected_component instanceof Load load) {
            load.setResistance(typed_val);
            InputField.setText("Load: " + load.getResistance() + " Ohms");
        }
        SimHandler.generateCycles();
        SimHandler.addCurrents();
        SimHandler.Calculate();
    }

    public void cancelPress() {
        SetText();
    }

    public void SaveClick() {
        String path = null;

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CIR files (*.cir)", "*.cir");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(Stage.getWindows().getFirst());

        if (file != null) {
            path = file.getAbsolutePath();
        }

        if (path != null) {
            try {
                FileWriter fw = new FileWriter(path);

                for (CircuitComponent c : CircuitComponent.getConnectables()) {
                    double extras1 = -1, extras2 = -1;
                    int type = -1, rotate = 0;

                    if (c.isRotate()) {
                        rotate = 1;
                    }

                    if (c instanceof Resistor resistor) {
                        type = 0;
                        extras1 = resistor.getResistance();
                    } else if (c instanceof Battery battery) {
                        type = 1;
                        extras1 = battery.getEMF();
                    } else if (c instanceof Load load) {
                        type = 2;
                        extras1 = load.getResistance();
                    } else if (c instanceof Switch switch_element) {
                        type = 3;
                        if (switch_element.isSwitchOpen()) extras2 = 1;
                        else extras2 = 0;
                    } else if (c instanceof Wire wire) {
                        type = 4;
                        extras1 = wire.getLength();
                    }

                    fw.write(c.getX() + "," + c.getY() + "," + type + "," + extras1 + "," + extras2 + "," + rotate + "\n");
                }

                fw.close();
            } catch (IOException e) {
                System.out.println("Error creating file");
            }
        }

    }

    public void LoadClick() {
        ResetAll();
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CIR files (*.cir)", "*.cir");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(Stage.getWindows().getFirst());

        if (file != null) {
            try {
                String line;
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file.getAbsolutePath()));

                while ((line = bufferedReader.readLine()) != null) {
                    double x_pos, y_pos, extras1, extras2;
                    int type, rotate;
                    line = line.replace("\n", "");
                    String[] arr = line.split(",");
                    x_pos = Double.parseDouble(arr[0]);
                    y_pos = Double.parseDouble(arr[1]);
                    type = Integer.parseInt(arr[2]);
                    extras1 = Double.parseDouble(arr[3]);
                    extras2 = Double.parseDouble(arr[4]);
                    rotate = Integer.parseInt(arr[5]);

                    Image img;
                    if (type != 3) {
                        img = canvas_images.get(type);
                    } else {
                        if (extras2 == 0) {
                            img = canvas_images.get(5);
                        } else {
                            img = canvas_images.get(3);
                        }
                    }
                    ImageView view = new ImageView(img);

                    CircuitComponent c;

                    if (type == 0) {
                        c = new Resistor(extras1, view, SimSpace, img, highlight_canvas_images.get(type));
                        setupDrag(view, view, c);
                    } else if (type == 1) {
                        c = new Battery(extras1, view, SimSpace, img, highlight_canvas_images.get(type));
                        setupDrag(view, view, c);
                    } else if (type == 2) {
                        c = new Load(extras1, view, SimSpace, img, highlight_canvas_images.get(type));
                        setupDrag(view, view, c);
                        Circle bulb_light = new Circle();
                        bulb_light.setRadius(45);
                        ObjectBinding<Color> colorBinding = Bindings.createObjectBinding(() -> Color.hsb(50, Math.max(0, Math.min(1, (Math.abs(c.getFinalCurrent()))/10)), 1), c.currentProperty());
                        bulb_light.fillProperty().bind(colorBinding);
                        bulb_light.layoutXProperty().bind(view.layoutXProperty().add(102));
                        bulb_light.layoutYProperty().bind(view.layoutYProperty().add(72));
                        load_components.put(c, bulb_light);
                        SimSpace.getChildren().add(bulb_light);
                        bulb_light.toBack();
                    } else if (type == 3) {
                        if (extras2 == 0) {
                            c = new Switch(view, SimSpace, img, highlight_canvas_images.get(5));
                            ((Switch)c).toggleSwitch();
                        } else {
                            c = new Switch(view, SimSpace, img, highlight_canvas_images.get(3));
                        }
                        setupDrag(view, view, c);
                    } else if (type == 4) {
                        view.setVisible(false);
                        c = new Wire(view, SimSpace, img, highlight_canvas_images.get(type));

                        // Instead of ImageView, Wire is treated as a Line to facilitate increase and decrease of length
                        Line wire_line = new Line();
                        wire_line.startXProperty().bind(c.getNode1().layoutXProperty());
                        wire_line.startYProperty().bind(c.getNode1().layoutYProperty());
                        wire_line.endXProperty().bind(c.getNode2().layoutXProperty());
                        wire_line.endYProperty().bind(c.getNode2().layoutYProperty());
                        wire_line.setStrokeWidth(4.5);
                        setupDrag(wire_line, view, c);
                        SimSpace.getChildren().add(wire_line);
                        wire_line.toBack();

                        wire_components.put(c, wire_line);

                        if (extras1 < 200) {
                            Circle c1 = c.getNode1();
                            Circle c2 = c.getNode2();

                            ArrayList<Double> delta = pointTowards(c);
                            double delX = delta.get(0);
                            double delY = delta.get(1);

                            double val = (200 - extras1)/2;

                            c2.setLayoutX(c2.getLayoutX() - delX * val);
                            c2.setLayoutY(c2.getLayoutY() - delY * val);

                            c1.setLayoutX(c1.getLayoutX() + delX * val);
                            c1.setLayoutY(c1.getLayoutY() + delY * val);
                        } else if (extras1 > 200) {
                            Circle c1 = c.getNode1();
                            Circle c2 = c.getNode2();

                            ArrayList<Double> delta = pointTowards(c);
                            double delX = delta.get(0);
                            double delY = delta.get(1);

                            double val = (extras1 - 200)/2;

                            c2.setLayoutX(c2.getLayoutX() + delX * val);
                            c2.setLayoutY(c2.getLayoutY() + delY * val);

                            c1.setLayoutX(c1.getLayoutX() - delX * val);
                            c1.setLayoutY(c1.getLayoutY() - delY * val);
                        }
                        ((Wire)c).setLength(extras1);

                    } else {
                        c = null;
                    }
                    if (rotate == 1) {
                        c.rotate();
                    }
                    c.setPos(x_pos, y_pos);
                    SimHandler.updateConnections(c);
                }
            } catch (IOException e) {
                System.out.println("Error reading file");
            }
        }
    }

    public void ResetClick() {
        ResetAll();
    }

    private void ResetAll() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Reset");
        a.setContentText("This action will clear the current work area. Continue?");

        Optional<ButtonType> result = a.showAndWait();
        if (result.get().equals(ButtonType.OK)) {
            a.close();
            if (selected_component != null) {
                if (selected_component instanceof Wire) {
                    SimSpace.getChildren().remove(lineHighlight);
                } else {
                    selected_component.RemoveHighlight();
                }
            }
            selected_component = null;
            HideInspector();
            for (CircuitComponent c : CircuitComponent.getConnectables()) {
                SimSpace.getChildren().remove(c.getImageView());
                if (c instanceof Wire) {
                    SimSpace.getChildren().remove(wire_components.get(c));
                    SimSpace.getChildren().remove(lineHighlight);
                } else if (c instanceof Load) {
                    SimSpace.getChildren().remove(load_components.get(c));
                }
                SimSpace.getChildren().remove(c.getNode1());
                SimSpace.getChildren().remove(c.getNode2());
            }
            wire_components.clear();
            SimHandler.ClearAll();
            CircuitComponent.ClearAll();
        } else {
            a.close();
        }
    }

    private double roundNearest25(double n) {
        return Math.round(n / 25) * 25;
    }

    private void ShowLabels() {
        InputField.setVisible(true);
        InputField.setDisable(false);
        ok.setVisible(true);
        ok.setDisable(true);
        cancel.setVisible(true);
        cancel.setDisable(true);
        Info1.setVisible(true);
        Info2.setVisible(true);
        Info3.setVisible(true);
        closeToggle.setVisible(true);

        StringBinding formatted1 = Bindings.createStringBinding(() ->
                        String.format("Current: %.2f A", Math.abs(selected_component.getFinalCurrent())),
                selected_component.currentProperty()
        );
        Info1.textProperty().bind(formatted1);
        closeToggle.setVisible(false);

        if (selected_component instanceof Resistor resistor) {
            StringBinding formatted2 = Bindings.createStringBinding(() ->
                            String.format("PD: %.2f Volts", resistor.getResistance() * Math.abs(selected_component.getFinalCurrent())),
                    selected_component.currentProperty()
            );

            Info2.textProperty().bind(formatted2);

            StringBinding formatted3 = Bindings.createStringBinding(() ->
                            String.format("Power: %.2f Watts", resistor.getResistance() * selected_component.getFinalCurrent() * selected_component.getFinalCurrent()),
                    selected_component.currentProperty()
            );
            Info3.textProperty().bind(formatted3);
            NameLabel.setText("Resistor");
        } else if (selected_component instanceof Battery battery) {
            Info3.setVisible(false);
            StringBinding formatted2 = Bindings.createStringBinding(() ->
                            String.format("Power: %.2f Watts", battery.getEMF() * selected_component.getFinalCurrent()),
                    selected_component.currentProperty()
            );
            Info2.textProperty().bind(formatted2);
            NameLabel.setText("Battery");
        } else if (selected_component instanceof Wire) {
            InputField.setText("");
            InputField.setDisable(true);
            Info2.setVisible(false);
            Info3.setVisible(false);
            NameLabel.setText("Wire");
        } else if (selected_component instanceof Switch switch_element) {
            InputField.setText("");
            closeToggle.setVisible(true);
            closeToggle.setDisable(false);
            closeToggle.setSelected(!switch_element.isSwitchOpen());
            InputField.setDisable(true);
            Info2.setVisible(false);
            Info3.setVisible(false);

            closeToggle.setOnAction(_ -> {
                switch_element.toggleSwitch();
                if (!switch_element.isSwitchOpen()) {
                    switch_element.setInit(canvas_images.get(5));
                    switch_element.setAlt(highlight_canvas_images.get(5));
                    switch_element.Highlight();
                } else {
                    switch_element.setInit(canvas_images.get(3));
                    switch_element.setAlt(highlight_canvas_images.get(3));
                    switch_element.Highlight();
                }
                SimHandler.generateCycles();
                SimHandler.addCurrents();
                SimHandler.Calculate();
            });

            NameLabel.setText("Switch");
        } else if (selected_component instanceof Load load) {
            StringBinding formatted2 = Bindings.createStringBinding(() ->
                            String.format("PD: %.2f Volts", load.getResistance() * Math.abs(selected_component.getFinalCurrent())),
                    selected_component.currentProperty()
            );

            Info2.textProperty().bind(formatted2);

            StringBinding formatted3 = Bindings.createStringBinding(() ->
                            String.format("Power: %.2f Watts", load.getResistance() * selected_component.getFinalCurrent() * selected_component.getFinalCurrent()),
                    selected_component.currentProperty()
            );
            Info3.textProperty().bind(formatted3);

            NameLabel.setText("Load");
        }
    }

    private void SetText() {
        if (selected_component instanceof Resistor resistor) {
            InputField.setText("Resistance: " + resistor.getResistance() + " Ohms");
        } else if (selected_component instanceof Battery battery) {
            InputField.setText("EMF: " + battery.getEMF() + " Volts");
        } else if (selected_component instanceof Load load) {
            InputField.setText("Load: " + load.getResistance() + " Ohms");
        }
    }

    private void HideInspector() {
        InputField.setVisible(false);
        ok.setVisible(false);
        cancel.setVisible(false);
        Info1.setVisible(false);
        Info2.setVisible(false);
        Info3.setVisible(false);
        closeToggle.setVisible(false);
        NameLabel.setText("Nothing to show");
    }

    private ArrayList<Double> pointTowards(CircuitComponent c) {
        Circle c1 = c.getNode1();
        Circle c2 = c.getNode2();

        double delX = c2.getLayoutX() - c1.getLayoutX();
        double delY = c2.getLayoutY() - c1.getLayoutY();

        double mag = Math.sqrt(delX*delX + delY*delY);
        delX /= mag;
        delY /= mag;

        ArrayList<Double> temp = new ArrayList<>();
        temp.add(delX);
        temp.add(delY);
        return temp;
    }
}