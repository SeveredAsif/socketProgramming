package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class Worker extends Thread {
    Socket socket;
    HashMap<String, Integer> userMap;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    // private static DataOutputStream dataOutputStream = null;
    // private static DataInputStream dataInputStream = null;

    public Worker(Socket socket, HashMap<String, Integer> userMap) {
        this.socket = socket;
        this.userMap = userMap;
        try {
            dataInputStream = new DataInputStream(
                    this.socket.getInputStream());
            dataOutputStream = new DataOutputStream(
                    this.socket.getOutputStream());
        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    public void receiveFile(String fileName, int chunksize)
            throws Exception {
        int bytes = 0;
        FileOutputStream fileOutputStream = new FileOutputStream("xxxxx.txt");
        System.out.println("here reached");
        long size = this.dataInputStream.readLong(); // read file size
        System.out.println("file size: " + size);
        byte[] buffer = new byte[chunksize];
        while (size > 0
                && (bytes = dataInputStream.read(
                        buffer, 0,
                        (int) Math.min(buffer.length, size))) != -1) {
            // Here we write the file using write method
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes; // read upto file size
            System.out.println("bytes: " + bytes);
        }
        // Here we received file
        System.out.println("File is Received");
        fileOutputStream.close();
    }

    public void run() {
        // buffers
        try {

            ObjectOutputStream out = new ObjectOutputStream(this.socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(this.socket.getInputStream());

            // for login, we need username
            String s = "Provide username";
            out.writeObject(s);

            // recieve the username
            String providedUserName = (String) in.readObject();
            if (userMap.containsKey(providedUserName)) {
                if (userMap.get(providedUserName) > 0) {
                    s = "User already exists";
                    out.writeObject(s);
                    // close the connection
                    try {
                        socket.close();
                    } catch (Exception e) {
                        System.out.println("Error: " + e);
                    }

                }

                else {
                    userMap.put(providedUserName, 1);
                    s = "Logged in successfully!";
                    System.out.println(providedUserName + " logged in !");
                    out.writeObject(s);
                }
            } else {
                userMap.put(providedUserName, 1);
                s = "User registered and Logged in successfully!";

                new File("./" + providedUserName).mkdirs();
                System.out.println(providedUserName + " registered and logged in !");
                System.out.println("A new directory with name " + providedUserName + " has been created!");

                out.writeObject(s);
            }

            while (true) {
                String continuousListen = (String) in.readObject();
                // System.out.println(continuousListen);
                if (continuousListen.equalsIgnoreCase("logout")) {
                    userMap.put(providedUserName, 0);
                    // close the connection
                    try {
                        socket.close();
                        System.out.println(providedUserName + " logged out!");
                        break;
                    } catch (Exception e) {
                        // System.out.println("Error: " + e);
                    }
                } else if (continuousListen.equalsIgnoreCase("list of clients")) {
                    // System.out.println(userMap.keySet());
                    String clients = "";
                    for (String entry : userMap.keySet()) {
                        clients += ("," + entry);
                    }
                    clients = clients.substring(1, clients.length());
                    out.writeObject(clients);
                } else if (continuousListen.equalsIgnoreCase("upload")) {
                    String serverResponse = "provide the file_name,file_size";
                    out.writeObject(serverResponse);
                    String clientResponse = (String) in.readObject();
                    String[] p = clientResponse.split(",");
                    while (p.length != 2) {
                        serverResponse = "format wrong: Use format file_name,file_size";
                        out.writeObject(serverResponse);
                        clientResponse = (String) in.readObject();
                        p = clientResponse.split(",");
                    }
                    for (String part : p) {
                        System.out.println(part + " :in server");
                    }
                    serverResponse = "4096"; // have to replace it using a random number
                    out.writeObject(serverResponse);
                    receiveFile(p[0], 4096);
                }
            }
            this.dataInputStream.close();
            this.dataOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
