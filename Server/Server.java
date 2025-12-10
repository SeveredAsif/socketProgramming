package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.ArrayList;

public class Server {

    static int MAX_BUFFER_SIZE = 50000;
    static int MIN_CHUNK_SIZE = 1024;
    static int MAX_CHUNK_SIZE = 4096;
    static int CURR_BUFFER_SIZE = 0;
    static HashMap<String, Socket> userNametoSocket;
    static int reqID = 0;
    static HashMap<String, ArrayList<String>> messageBox;
    static HashMap<Integer, String> reqIdtoUsername;
    static int fileId = 0;
    static HashMap<Integer, ArrayList<String>> fileIdtoFileNameAndUploader;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket welcomeSocket = new ServerSocket(6666);
        HashMap<String, Integer> userMap = new HashMap<>();
        userNametoSocket = new HashMap<>();
        messageBox = new HashMap<>();
        reqIdtoUsername = new HashMap<>();
        fileIdtoFileNameAndUploader = new HashMap<>();

        while (true) {
            int x = 5;
            System.out.println("Waiting for connection...");
            Socket socket = welcomeSocket.accept();
            System.out.println("Connection established");
            // open thread
            Thread worker = new Worker(socket, userMap);
            worker.start();
            if (x != 5) {
                break;
            }
        }
        welcomeSocket.close();

    }
}
