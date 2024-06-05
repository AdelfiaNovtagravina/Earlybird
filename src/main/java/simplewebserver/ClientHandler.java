/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simplewebserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Base64;

/**
 * 
 * @author Adelfia
 */
public class ClientHandler extends Thread {
    private Socket socket;
    private String logsPath;
    private WebServer webServer;

    public ClientHandler(Socket socket, String logsPath, WebServer server) {
        this.socket = socket;
        this.logsPath = logsPath;
        this.webServer = server;
    }

    @Override
    // Membaca permintaan user dan mengirimkan respon
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] tokens = requestLine.split(" ");

            if (tokens.length < 2) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String method = tokens[0];
            String requestURL = tokens[1];

            if (method.equals("GET")) {
                serveFile(requestURL, out);
            } else {
                sendErrorResponse(out, 501, "Not Implemented");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Melayani permintaan GET untuk file atau direktori
    private void serveFile(String requestURL, DataOutputStream out) throws IOException {
        try {
            String filePath = WebServer.getWebRoot() + requestURL.replace("/", "\\");
            File file = new File(filePath);

            String clientIP = socket.getInetAddress().getHostAddress();

            if (file.exists()) {
                if (file.isDirectory()) {
                    if (requestURL.endsWith("/")) {
                        listDirectory(file, out, getParentDirectory(requestURL));
                        logAccess(requestURL, clientIP, "200 OK");
                    } else {
                        String redirectURL = requestURL + "/";
                        String response = "HTTP/1.1 301 Moved Permanently\r\nLocation: " + redirectURL + "\r\n\r\n";
                        out.writeBytes(response);
                    }
                } else {
                    String contentType = getContentType(file);
                    byte[] fileData = Files.readAllBytes(file.toPath());
                    String response = "HTTP/1.1 200 OK\r\nContent-Length: " + fileData.length +
                            "\r\nContent-Type: " + contentType + "\r\n\r\n";
                    out.writeBytes(response);
                    out.write(fileData);
                    logAccess(requestURL, clientIP, "200 OK");
                }
            } else {
                sendErrorResponse(out, 404, "Not Found");
                logAccess(requestURL, clientIP, "404 Not Found");
            }
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            String response = "HTTP/1.1 500 Internal Server Error\r\n\r\n";
            out.writeBytes(response);
            logAccess(requestURL, socket.getInetAddress().getHostAddress(), "500 Internal Server Error: " + errorMessage);
        }
    }

    // Mengirimkan respon error dengan kode status dan pesan
    private void sendErrorResponse(DataOutputStream out, int statusCode, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n\r\n";
        out.writeBytes(response);
    }

    // Mengembalikan content type berdasarkan ekstensi file
    private String getContentType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else {
            return "application/octet-stream";
        }
    }

    // Menampilkan daftar isi direktori
    private void listDirectory(File directory, DataOutputStream out, String parentDirectory) throws IOException {
        File[] files = directory.listFiles();
        StringBuilder responseBuilder = new StringBuilder("<html><head>");

        responseBuilder.append("<style>");
        responseBuilder.append("body { font-family: Arial, sans-serif; background-color: #f0f0f0; }");
        responseBuilder.append("h1 { color: #6C78AF; margin-bottom: 10px; }");
        responseBuilder.append("p { margin-top: 5px; }");
        responseBuilder.append("table { width: 50%; border-collapse: collapse; }");
        responseBuilder.append("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }");
        responseBuilder.append("th { background-color: #f2f2f2; }");
        responseBuilder.append("</style>");

        responseBuilder.append("</head><body>");
        responseBuilder.append("<h1><i style=\"color:#6C78AF\">EasyWS - MyAdmin: </i></h1>");
        responseBuilder.append("<p>Welcome to EasyWS! The Easy Web Server is successfully connected.</p>");

        if (parentDirectory != null) {
            responseBuilder.append("<button onclick=\"goBack()\">Back</button><br><br>");
        }

        responseBuilder.append("<table>");
        responseBuilder.append("<tr><th> </th><th>Name of Files</th><th>Size</th></tr>");

        for (File file : files) {
            String fileName = file.getName();
            String size = file.isDirectory() ? formatSize(calculateDirectorySize(file)) : formatSize(file.length());

            String iconBase64 = getIconForFile(file);
            responseBuilder.append("<tr>");
            responseBuilder.append("<td style='text-align: center;'><img src=\"data:image/png;base64,").append(iconBase64).append("\" width=\"32\" height=\"32\"></td>");
            responseBuilder.append("<td><a href=\"").append(fileName).append("\">").append(fileName).append("</a></td>");
            responseBuilder.append("<td>").append(size).append("</td>");
            responseBuilder.append("</tr>");
        }

        responseBuilder.append("</table>");
        responseBuilder.append("<script>");
        responseBuilder.append("function goBack() { window.history.back(); }");
        responseBuilder.append("</script>");
        responseBuilder.append("</body></html>");

        String response = "HTTP/1.1 200 OK\r\nContent-Length: " + responseBuilder.length() + "\r\nContent-Type: text/html\r\n\r\n" + responseBuilder.toString();
        out.writeBytes(response);
    }


    private String getIconForFile(File file) {
        String iconPath;
        if (file.isDirectory()) {
            iconPath = "/icons/folder_icon.png";
        } else {
            iconPath = "/icons/file_icon.png";
        }

        try (InputStream inputStream = getClass().getResourceAsStream(iconPath)) {
            if (inputStream != null) {
                byte[] iconBytes = inputStream.readAllBytes();
                String base64Icon = Base64.getEncoder().encodeToString(iconBytes);
                System.out.println("Base64 Icon: " + base64Icon);
                return base64Icon;
            } else {
                System.err.println("Icon file not found: " + iconPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getParentDirectory(String requestURL) {
        int lastSlashIndex = requestURL.lastIndexOf("/");
        if (lastSlashIndex > 0) {
            return requestURL.substring(0, lastSlashIndex);
        }
        return null;
    }

    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }

    private String formatSize(long size) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double sizeInUnits = size;
        while (sizeInUnits >= 1024 && unitIndex < units.length - 1) {
            sizeInUnits /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", sizeInUnits, units[unitIndex]);
    }

    // Mencatat akses ke file logs dan menambahkan ke UI
    private void logAccess(String requestURL, String clientIP, String statusCode) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String timestamp = formatter.format(new Date());

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String date = dateFormatter.format(new Date());

        String logMessage = String.format("%s [%s] \t %s - %s", timestamp, requestURL, clientIP, statusCode);
        String logFileName = date + ".log";
        try {
            Files.write(Paths.get(logsPath, logFileName), (logMessage + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            webServer.logAccess(logMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}