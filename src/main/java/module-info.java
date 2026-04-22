module com.inkwell {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;

    opens com.inkwell.controller to javafx.fxml;
    exports com.inkwell.app;
    exports com.inkwell.model;
    exports com.inkwell.controller;
    exports com.inkwell.database;
}
