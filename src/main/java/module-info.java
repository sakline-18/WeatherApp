module practice.weatherapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;


    opens practice.weatherapp to javafx.fxml;
    exports practice.weatherapp;
}