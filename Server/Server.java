package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {

    int MAX_BUFFER_SIZE = 100;
    int MIN_CHUNK_SIZE = 2;
    int MAX_CHUNK_SIZE = 10;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket welcomeSocket = new ServerSocket(6666);
        HashMap<String, Integer> userMap = new HashMap<>();

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
