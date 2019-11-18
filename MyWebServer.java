/*--------------------------------------------------------

1. Lea Middleton / 4.17.2019:

2. Java version used: Java 1.8.0_201

3. Precise command-line compilation examples / instructions:

> javac MyWebServer.java

4. Precise examples / instructions to run this program:

In separate shell windows:

> java MyWebServer

5. List of other files accompanied with MyWebServer.java.

 a. http-streams.txt
 b. serverlog.txt
 c. checklist-mywebserver.html

----------------------------------------------------------*/


import java.io.*;
import java.net.*;

public class MyWebServer {
    /* Control switch */
    private static boolean controlSwitch = true;

    public static void main(String args[]){
        int request_limit = 6;
        int port = 2540;

        System.out.println("\n\nLea's WebServer Java 1.8 started....\n");
        Socket webSocket;
        ServerSocket serverSocket;
        System.out.println("In working directory: " + System.getProperty("user.dir")+ "\n");
        try{
            serverSocket = new ServerSocket(port, request_limit);
            System.out.println("Lea's WebServer listening at port " + String.valueOf(port)+ ".....\n");

            while(controlSwitch){
                webSocket = serverSocket.accept();
                new WebServerListenerWorker(webSocket).start();
                System.out.println("Connection established \n");
            }
        } catch (IOException e) {
            System.out.println("Port specified is currently in use!\n");
        }
    }
    public static boolean getStatus(){
        return controlSwitch;
    }

    public static void setStatus(boolean status){
        controlSwitch = status;
    }
}

// handle requests
class WebServerListenerWorker extends Thread{
    private Socket webSocket;
    private String webData = "";
    private File fileInfo;
    private String parentDir;
    private File[] filesInDirectory;
    private String setFileType = "";
    private String userInput = "";
    private String pageInputs[];
    public WebServerListenerWorker(Socket socket) {
        this.webSocket = socket;
    }

    public void run() {
        PrintStream webSocketOut;
        BufferedReader webSocketIn;

        String pathInfo = "";

        try {
            webSocketIn = new BufferedReader(new InputStreamReader(webSocket.getInputStream()));
            webSocketOut = new PrintStream(webSocket.getOutputStream());

            /* Get Request Header and print to server console */
            if((webData = webSocketIn.readLine()) == null){
                MyWebServer.setStatus(false);
            }

            System.out.println("Request : " + webData);
            /* Saved webData header info */
            pathInfo = webData.split(" ")[1];

            ///////* Determine what to do when given a file extension or directory */////

            ///////////* Files ending in .txt and .java */////////////
            if(pathInfo.contains(".txt") || pathInfo.contains(".java") || pathInfo.contains(".class")){
                System.out.println("Case: .txt or .java or .class");
                System.out.println("File retrieved is of .txt or .java type");
               fileInfo = new File("." + pathInfo);
               setFileType = "text/plain";
               /* Send Response Header */
                webSocketOut.println("HTTP/1.1 200 OK\r\n" +
                        "<!DOCTYPE html><html>" +
                        "Content-Length: " + (int)fileInfo.length()+ "\r\n" +
                        "Content-Type: " + setFileType +
                        "\r\n");
                InputStream file = new FileInputStream(fileInfo);
                pushToBrowser(file, webSocketOut);

                /* Print to server console files as they are given hotlinks for the browser */
                System.out.println("Response : " + webData + " 200 OK");
                System.out.print("Current file being displayed in browser: ");
                System.out.println(pathInfo + "\n");

            }//////////////* Files ending in .html */////////////////
            else if(pathInfo.contains(".html")){
                System.out.println("Case: .html");
                System.out.println("File retrieved is of .html type");
                fileInfo = new File("." + pathInfo);
                setFileType = "text/html";
                /* Send Response Header */
                webSocketOut.println("HTTP/1.1 200 OK\r\n" +
                        "<!DOCTYPE html><html>" +
                        "Content-Length: " + (int)fileInfo.length()+ "\r\n" +
                        "Content-Type: " + setFileType +
                        "\r\n");
                InputStream file = new FileInputStream(fileInfo);
                pushToBrowser(file, webSocketOut);

                /* Print to server console files as they are given hotlinks for the browser */
                System.out.println("Response : " + webData + " 200 OK");
                System.out.print("Current file being displayed in browser: ");
                System.out.println(pathInfo + "\n");

            }//////////////* Files ending in fake-cgi *///////////////
            else if(pathInfo.contains(".fake-cgi")){
                System.out.println("Case: .fake-cgi");
                /* Get user inputs and split in order to access data */
                userInput = pathInfo.split("\\?")[1];
                pageInputs = userInput.split("&");

                /* Add 1st number after the split.("=") in pageInputs[1] with 2nd number after the split.("=") in pageInputs[2] */
                int sum = Integer.parseInt((pageInputs[1]).split("=")[1]) + Integer.parseInt((pageInputs[2]).split("=")[1]) ;

                // send html to client
                webSocketOut.append("HTTP/1.1 200 OK\r\n" +
                        "\r\n" +
                        "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" +
                        "<TITLE>SUM of 2 NUMBERS</TITLE>\r\n" +
                        "</HEAD><BODY>\r\n" +
                        "<H1><center><font color='blue'>" + pageInputs[0].split("=")[1] + ",  " +Integer.parseInt((pageInputs[1]).split("=")[1]) + " + "
                        + Integer.parseInt((pageInputs[2]).split("=")[1]) +
                        " = " + sum +
                        "</font color></center></H1>\r\n</BODY></HTML>\r\n");
                System.out.println("Response : " + webData + " 200 OK");
            }////////////////////* Directories *////////////////////
            else{
                if((fileInfo = new File("." + pathInfo)) == null) {
                    webSocketOut.println("404, The selected directory or file does not exist!!");
                    System.out.println("Response : " + webData + " 400 Not Found ");
                } else {
                    System.out.println("In localhost directory");
                    filesInDirectory = fileInfo.listFiles();
                    webSocketOut.append("HTTP/1.1 200 OK\r\n" +
                            "\r\n" +
                            "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" +
                            "<TITLE>Directory</TITLE>\r\n" +
                            "</HEAD><BODY>\r\n" +
                            "<H1>Index of " + pathInfo + "</H1>\r\n");
                    /* Get parent directory */
                    parentDir = fileInfo.getParent();
                    if(parentDir != null) {
                        if(parentDir.split("\\.").length == 0) {
                            webSocketOut.append("<a href=/> Parent Directory </a><br>");
                        }else {
                            webSocketOut.append("<a href=" + parentDir.split("\\.")[1] + "> Parent Directory </a><br>");
                        }
                    }
                    /* Print to server console files as they are given hotlinks for the browser */
                    System.out.println("Retrieving: ");
                    /* Create HTML display of files and directories */
                    for(File file: filesInDirectory){
                        if(file.isDirectory()) {
                            webSocketOut.append("<a href=" + file.getPath() + ">" + file.getName() + "/</a><br>");
                            System.out.println("Response : " + webData + " 200 OK");
                            System.out.println("          "+file.getPath() + file.getName());
                        }
                        else if (file.isFile()) {
                            webSocketOut.append("<a href=" + file.getPath().substring(1) + ">" + file.getName() + "</a><br>");
                            System.out.println("Response : " + webData + " 200 OK");
                            System.out.println("          "+file.getPath().substring(1) + file.getName());
                        }
                    }
                    System.out.println("\n");
                    webSocketOut.println("</BODY></HTML>\r\n");
                }
            }
            webSocket.close();
            System.out.println("Connection closed \n");
        } catch (IOException e) {
            System.out.println("Connection reset. Listening again...");
        }
    }

    /* Send file contents to web browser */
    private void pushToBrowser(InputStream file, OutputStream webServerOut) {
        byte[] buffer = new byte[1000];
        try {
            while (file.available() > 0) {
                webServerOut.write(buffer, 0, file.read(buffer));
            }
        } catch (IOException e) {
            System.out.println("Error when reading send file");
        }
    }
}
