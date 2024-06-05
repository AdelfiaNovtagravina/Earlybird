/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simplewebserver;

import java.io.File;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 *
 * @author Adelfia
 */
public class ServerUI extends Application {
    private WebServer webServer;
    private TextField filePathField, logsPathField, portField;
    private TextArea logArea;
    private Button startButton, stopButton;
    private final Preferences preferences = Preferences.userNodeForPackage(ServerUI.class);

    // Meluncurkan aplikasi JavaFX
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    // Menginisialisasi komponen UI dan menampilkan UI
    public void start(Stage primaryStage) {
        primaryStage.setTitle("EasyWS - Simple Web Server");
        
        Label titleLabel = new Label("EasyWS Control Panel");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(20, 0, 20, 0));
        
        BorderPane layout = new BorderPane();
        layout.setTop(titleBox);
        
        // Port input
        Label portLabel = new Label("\tPort\t\t");
        portLabel.setStyle("-fx-font-weight: bold;");
        portField = new TextField(preferences.get("port", "8080"));
        HBox portBox = new HBox(5, portLabel, portField);
        portBox.setAlignment(Pos.CENTER_LEFT);
        
        // File path input
        Label pathLabel = new Label("\tFile\t\t");
        pathLabel.setStyle("-fx-font-weight: bold;");
        filePathField = new TextField(preferences.get("filePath", "D:\\Web\\Files"));
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseFilePath(primaryStage));
        HBox filePathBox = new HBox(5, pathLabel, filePathField, browseButton);
        filePathBox.setAlignment(Pos.CENTER_LEFT);
        
        // Logs path input
        Label logsPathLabel = new Label("\tLogs\t\t");
        logsPathLabel.setStyle("-fx-font-weight: bold;");
        logsPathField = new TextField(preferences.get("logsPath", "D:\\Web\\Logs"));
        Button logsBrowseButton = new Button("Browse");
        logsBrowseButton.setOnAction(e -> browseLogsPath(primaryStage));
        HBox logsPathBox = new HBox(5, logsPathLabel, logsPathField, logsBrowseButton);
        logsPathBox.setAlignment(Pos.CENTER_LEFT);

        // Start button & Stop button
        startButton = new Button("Start");
        stopButton = new Button("Stop");
        
        startButton.setOnAction(e -> startWebServer());
        stopButton.setOnAction(e -> stopWebServer());

        HBox buttonBox = new HBox(10, startButton, stopButton);
        buttonBox.setPadding(new Insets(0, 0, 0, 0));
        buttonBox.setAlignment(Pos.CENTER);

        // Pengaturan tata letak
        VBox contentLayout = new VBox(10, portBox, filePathBox, logsPathBox, buttonBox);
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        VBox logBox = new VBox(10, logArea);
        logBox.setPadding(new Insets(10, 20, 20, 20));

        VBox mainLayout = new VBox(10, contentLayout, logBox);
        layout.setCenter(mainLayout);
        
        Scene scene = new Scene(layout, 500, 400);
        Color backgroundColor = Color.LIGHTGRAY;
        BackgroundFill backgroundFill = new BackgroundFill(backgroundColor, null, null);
        Background background = new Background(backgroundFill);
        layout.setBackground(background);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void browseFilePath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select File Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            filePathField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    private void browseLogsPath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Logs Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            logsPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void startWebServer() {
        if (webServer == null || !webServer.isAlive()) {
            String filePath = filePathField.getText();
            String logsPath = logsPathField.getText();
            String portText = portField.getText(); 
            
            int port;

            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                logArea.appendText("Invalid port number\n");
                return;
            }
        
            // Menyimpan input ke preferensi
            preferences.put("port", String.valueOf(port)); 
            preferences.put("filePath", filePath);
            preferences.put("logsPath", logsPath);

            if (webServer == null || !webServer.isAlive()) {
                webServer = new WebServer(filePath, logsPath, port, this); 
                webServer.start(); 
                logArea.appendText("Attempting to start EasyWS app on port " + port + "\n");
                startButton.setText("Start");
                startButton.setDisable(false);
                stopButton.setDisable(false);
                startButton.setDisable(true); 
                System.out.println("Attempting to start EasyWS app on port " + port + "\n");
            } else {
                logArea.appendText("Server already running\n");
                System.out.println("Server already running");
            }
        }
    }

    private void stopWebServer() {
        if (webServer != null && webServer.isAlive()) {
            int port;
            try {
                port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException e) {
                logArea.appendText("Invalid port number\n");
                return;
            }
            
            preferences.put("port", String.valueOf(port));
            
            if (webServer != null && webServer.isAlive()) { 
                webServer.stopServer();
                logArea.appendText("Attempting to stop EasyWS app on port " + port + "\n");
                startButton.setText("Start"); 
                startButton.setDisable(true); 
                stopButton.setDisable(true);
                logArea.appendText("Loading...\n");
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // Menunggu 3 detik (dapat disesuaikan sesuai kebutuhan)
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        startButton.setText("Start");
                        startButton.setDisable(false);
                        stopButton.setDisable(false);
                        logArea.appendText("EasyWS app stopped on port " + port + "\n");
                    });
                }).start();
            } else {
                logArea.appendText("Server is not running\n");
            }
        }
    }

    // Menambahkan pesan log
    public void appendLog(String logMessage) {
        Platform.runLater(() -> logArea.appendText(logMessage + "\n"));
    }
}