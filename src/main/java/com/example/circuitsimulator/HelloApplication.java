package com.example.circuitsimulator;

import com.example.circuitsimulator.Controller.HelloController;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class HelloApplication extends Application {
    static final String SPLASH_IMAGE = "splash_screen_image.png";
    private Pane splashLayout;
    private ProgressBar loadProgress;
    private Label progressText;
    private static final int SPLASH_WIDTH = 800;
    private Scene mainScene;
    private Parent newPage;

    @Override
    public void init() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("View/hello-view.fxml"));
            newPage = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImageView imageView = new ImageView(new Image(SPLASH_IMAGE));
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(SPLASH_WIDTH);

        loadProgress = new ProgressBar();
        loadProgress.setPrefWidth(SPLASH_WIDTH - 20);
        progressText = new Label("Loading Application...");
        progressText.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(imageView, loadProgress, progressText);
        vBox.setAlignment(Pos.CENTER);

        splashLayout = new AnchorPane();
        splashLayout.getChildren().add(vBox);

        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);
        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setBottomAnchor(vBox, 0.0);
    }

    @Override
    public void start(Stage stage) throws IOException {
        final Task<ObservableList<String>> loadTask = new Task<ObservableList<String>>() {
            @Override
            protected ObservableList<String> call() throws Exception {

                ObservableList<String> completedProgress = FXCollections.observableArrayList();
                ObservableList<String> allProgress = FXCollections.observableArrayList(
                        "Resistors", "Batteries", "Switches", "Bulbs", "Algorithms"
                );
                updateProgress(0, allProgress.size());

                updateMessage("Loading...");
                for (int i = 0; i < allProgress.size(); i++) {
                    Thread.sleep(500);
                    updateProgress(i+1, allProgress.size());
                    String nextProgress = allProgress.get(i);
                    completedProgress.add(nextProgress);
                    updateMessage("Loading...Finished loading " + nextProgress);
                }
                Thread.sleep(500);
                updateMessage("Loading Complete.");
                return completedProgress;
            }
        };
        showSplash(stage, loadTask, ()->showMainStage(loadTask.valueProperty()));
        new Thread(loadTask).start();
    }

    private void showMainStage(ReadOnlyObjectProperty<ObservableList<String>> loaded) {
        newPage.setOpacity(0);
        mainScene.setRoot(newPage);
        FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.5), newPage);
        fadeSplash.setFromValue(0.0);
        fadeSplash.setToValue(1.0);
        fadeSplash.play();
    }

    private void showSplash(final Stage stage, Task<?> loadTask, InitCompletionHandler initCompletionHandler) {
        progressText.textProperty().bind(loadTask.messageProperty());
        loadProgress.progressProperty().bind(loadTask.progressProperty());
        loadTask.stateProperty().addListener((_, _, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                loadProgress.progressProperty().unbind();
                loadProgress.setProgress(1);
                stage.toFront();
                FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.5), splashLayout);
                fadeSplash.setFromValue(1.0);
                fadeSplash.setToValue(0.0);
                fadeSplash.setOnFinished(_ -> {initCompletionHandler.complete();});
                fadeSplash.play();
            }
        });

        mainScene = new Scene(splashLayout);
        stage.setScene(mainScene);
        stage.setTitle("Circuit Simulator");
        stage.setMaximized(true);
        stage.show();
    }

    public interface InitCompletionHandler { void complete(); }

    public static void main(String[] args) {
        launch();
    }
}