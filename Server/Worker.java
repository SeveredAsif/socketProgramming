package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

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

    public void receiveFile(String fileName, int chunksize, String providedUserName)
            throws Exception {
        int bytes = 0;
        String fileNewName = "./" + providedUserName + "/" + fileName;
        FileOutputStream fileOutputStream = new FileOutputStream(fileNewName);
        System.out.println("here reached");
        long size = this.dataInputStream.readLong(); // read file size
        System.out.println("file size: " + size);

        // reducing the size of the buffer
        Server.CURR_BUFFER_SIZE += size;
        long initSize = size;
        System.out.println("Server Current Buffer Size: " + Server.CURR_BUFFER_SIZE);
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

        // buffer size is restoring to the previous size
        Server.CURR_BUFFER_SIZE -= initSize;
        System.out.println("Server Current Buffer Size: " + Server.CURR_BUFFER_SIZE);
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
                System.out.println("A new directory with name " + providedUserName
                        + " has been created! (or it already existed!)");

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
                    String serverResponse = "provide the file_name";
                    out.writeObject(serverResponse);
                    String clientResponse = (String) in.readObject();

                    // client will send path and file size
                    String[] p = clientResponse.split(",");
                    while (p.length != 2) {
                        serverResponse = "format wrong: Use format file_name,file_size";
                        out.writeObject(serverResponse);
                        clientResponse = (String) in.readObject();
                        p = clientResponse.split(",");
                    }
                    for (String part : p) {
                        System.out.println(part + " : in server");
                    }

                    // check if file can fit in buffer
                    // if it can, decrease the buffer size of the class. if not, send some kind of
                    // response to client
                    // File file = new File(p[0]);
                    // long filesize = file.length();
                    // System.out.println("filesize: " + filesize);
                    // System.out.println(Server.CURR_BUFFER_SIZE);
                    // System.out.println(Server.MAX_BUFFER_SIZE);
                    while (Server.CURR_BUFFER_SIZE + Integer.parseInt(p[1]) > Server.MAX_BUFFER_SIZE) {
                        serverResponse = "wrong choice! Cannot upload this file due to its size right now. Provide another file_name";
                        out.writeObject(serverResponse);
                        System.out.println("Buffer Size exceeded! Cannot upload file to server");
                        clientResponse = (String) in.readObject();
                        p = clientResponse.split(",");
                    }
                    // while (Server.CURR_BUFFER_SIZE + filesize <= Server.MAX_BUFFER_SIZE) {
                    // serverResponse = "wrong choice! Cannot upload this file due to its size right
                    // now";
                    // out.writeObject(serverResponse);
                    // System.out.println("Buffer Size exceeded! Cannot upload file to server");
                    // clientResponse = (String) in.readObject();
                    // p = clientResponse.split(",");
                    // }

                    Random r = new Random();
                    int chunksize = r.nextInt(Server.MAX_CHUNK_SIZE - Server.MIN_CHUNK_SIZE + 1)
                            + Server.MIN_CHUNK_SIZE;
                    System.out.println("Chunk size: " + chunksize);

                    serverResponse = String.valueOf(chunksize);
                    out.writeObject(serverResponse);
                    receiveFile(p[0], chunksize, providedUserName);
                    // adding the size of the buffer again (the )

                }
            }
            this.dataInputStream.close();
            this.dataOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
