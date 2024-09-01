module com.example.circuitsimulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jgrapht.core;
    requires commons.math3;


    opens com.example.circuitsimulator to javafx.fxml;
    exports com.example.circuitsimulator;
    exports com.example.circuitsimulator.Controller;
    opens com.example.circuitsimulator.Controller to javafx.fxml;
    exports com.example.circuitsimulator.Model;
    opens com.example.circuitsimulator.Model to javafx.fxml;
}