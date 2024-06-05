/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simplewebserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Adelfia
 */
public class WebServer extends Thread {
    private volatile boolean shouldStop = false;
    private ServerSocket serverSocket;
    private static String webRoot;
    private int port;
    private String logsPath;
    private ServerUI ui;
    
    public WebServer(String webRoot, String logsPath, int port, ServerUI ui){
        WebServer.webRoot = webRoot;
        this.logsPath = logsPath;
        this.port = port;
        this.ui = ui;
    }
    
    public static String getWebRoot(){
        return webRoot;
    }
    
    // Menghentikan server dengan menutup 'ServerSocket'
    public void stopServer() {
        shouldStop = true;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Status change detected: stopped");
                ui.appendLog("Status change detected: stopped");
            } else {
                System.out.println("Server is already stopped");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    @Override
    // Menerima koneksi dan menangani permintaan dengan membuat 'ClientHandler' baru
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Status change detected: running");
            ui.appendLog("Status change detected: running");

            while (!shouldStop()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler requestHandler = new ClientHandler(clientSocket, logsPath, this);
                requestHandler.start();
            }
        } catch (IOException e) {
            if (!shouldStop) {
                e.printStackTrace();
                ui.appendLog(e.getMessage());
            }
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    ui.appendLog("Server socket closed.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                ui.appendLog(e.getMessage());
            }
        }
    }

    public boolean shouldStop() {
        return shouldStop;
    }
    
    // Menambahkan pesan log ke UI
    public void logAccess(String logMessage) {
        ui.appendLog(logMessage);
    }
}