package Server;

import java.io.File;
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
        loadExistingFiles(fileIdtoFileNameAndUploader);

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

    public static void loadExistingFiles(HashMap<Integer, ArrayList<String>> fileIdtoFileNameAndUploader) {
        File dir = new File("./");
        String username = "";
        int fileid = 0;
        File[] dir_contents = dir.listFiles();
        System.out.println(dir_contents);
        for (File file : dir_contents) {
            System.out.println(file.getName());
            if (file.getName().startsWith(".")) {
                continue; // i assume no other problem will be there
            }
            if (file.isDirectory()) {
                username = file.getName(); // this is the user directory
                File userDir = new File(file.getAbsolutePath());
                File[] user_dir_contents = userDir.listFiles();
                if (user_dir_contents == null) {
                    continue;
                }
                for (File userfiles : user_dir_contents) {
                    if (userfiles.getName().equals("private") && userfiles.isDirectory()) { // if it is private direc
                        File privateDir = new File(userfiles.getAbsolutePath());
                        if (privateDir.listFiles() != null) {
                            for (File privateFile : privateDir.listFiles()) {
                                ArrayList<String> arr = new ArrayList<>();
                                arr.add(username);
                                arr.add(privateFile.getName());
                                arr.add("private");
                                fileIdtoFileNameAndUploader.put(fileid, arr);
                                fileid++;
                            }
                        }

                    } else if (userfiles.getName().equals("public") && userfiles.isDirectory()) { // if it is public
                                                                                                  // direc
                        File publicDir = new File(userfiles.getAbsolutePath());
                        if (publicDir.listFiles() != null) {
                            for (File publicFile : publicDir.listFiles()) {

                                ArrayList<String> arr = new ArrayList<>();
                                arr.add(username);
                                arr.add(publicFile.getName());
                                arr.add("public");
                                fileIdtoFileNameAndUploader.put(fileid, arr);
                                fileid++;
                            }
                        }

                    }
                }
            }
        }
        Server.fileId = fileid;
    }
    // String s1 = file.get(0); // username
    // String s2 = file.get(1); // filename
    // String s3 = file.get(2);// public or private
}
