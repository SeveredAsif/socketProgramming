package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;

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
                    Server.userNametoSocket.put(providedUserName, socket);
                    out.writeObject(s);

                }
            } else {
                userMap.put(providedUserName, 1);
                s = "User registered and Logged in successfully!";
                Server.userNametoSocket.put(providedUserName, socket);
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
                        Server.userNametoSocket.remove(providedUserName, socket);
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
                } else if (continuousListen.contains("upload,")) {
                    String[] p = continuousListen.split(",");
                    String publicOrPrivate = p[1];
                    upload(in, out, providedUserName, this.dataInputStream, publicOrPrivate);

                } else if (continuousListen.equalsIgnoreCase("request file")) {
                    s = (String) in.readObject();
                    String[] p = s.split(",");
                    if (!p[1].equalsIgnoreCase("ALL")) {
                        // Socket recipientSocket = Server.userNametoSocket.get(p[1]);

                        // put reqId to userName map
                        Server.reqIdtoUsername.put(Server.reqID, providedUserName);

                        ArrayList<String> msgBox = Server.messageBox.getOrDefault(p[1], new ArrayList<>());
                        String msg = String.valueOf(Server.reqID) + "," + p[0];
                        msgBox.add(msg);

                        Server.messageBox.put(p[1], msgBox);
                        // System.out.println(Server.messageBox.get(p[1]));
                        Server.reqID++;

                    }
                    s = "request done!";
                    out.writeObject(s);
                    System.out.println("Request of user " + providedUserName + " added and sent to recipient");
                } else if (continuousListen.equalsIgnoreCase("read msgbox")) {
                    s = "Messages:\n   Request id,File description\n";
                    ArrayList<String> inbox = Server.messageBox.getOrDefault(providedUserName, new ArrayList<>());
                    for (String x : inbox) {
                        s += "* " + x + "\n";
                    }
                    out.writeObject(s);
                } else if (continuousListen.equalsIgnoreCase("upload in response to request")) {
                    System.out.println(continuousListen);
                    s = "Messages:\n";
                    ArrayList<String> inbox = Server.messageBox.getOrDefault(providedUserName, new ArrayList<>());
                    for (String x : inbox) {
                        s += "* " + x + "\n";
                    }
                    out.writeObject(s);

                    // have to upload
                    String clientSend = (String) in.readObject();
                    String[] p = clientSend.split(",");
                    String publicOrPrivate = p[1];
                    upload(in, out, providedUserName, this.dataInputStream, publicOrPrivate);

                    // get the req Id
                    s = (String) in.readObject();
                    int reqId = Integer.parseInt(s);
                    // handle error here ->left job
                    // reqId to socket map, send msg
                    String requesterName = Server.reqIdtoUsername.get(reqId);
                    ArrayList<String> msgBox = Server.messageBox.getOrDefault(requesterName, new ArrayList<>());
                    String msg = String.valueOf(Server.reqID)
                            + " -> File with this request id (that you requested) has been uploaded by user "
                            + providedUserName;
                    msgBox.add(msg);

                    Server.messageBox.put(requesterName, msgBox);

                } else if (continuousListen.equalsIgnoreCase("see downloads and uploads")) {

                    String publicDirName = "./" + providedUserName + "/public";
                    String privateDirName = "./" + providedUserName + "/private";
                    File dir = new File(publicDirName);
                    File[] dir_contents = dir.listFiles();

                    String publicFiles = "public:\n";
                    if (dir_contents != null) {
                        for (int i = 0; i < dir_contents.length; i++) {
                            publicFiles += dir_contents[i].getName() + "\n";
                        }
                    }

                    dir = new File(privateDirName);
                    dir_contents = dir.listFiles();

                    String privateFiles = "private:\n";
                    if (dir_contents != null) {
                        for (int i = 0; i < dir_contents.length; i++) {
                            privateFiles += dir_contents[i].getName() + "\n";
                        }
                    }

                    String serverResponse = "uploads:\n" + publicFiles + privateFiles;
                    out.writeObject(serverResponse);
                } else if (continuousListen.equalsIgnoreCase("download file")) {

                    StringBuilder response = new StringBuilder("uploads:\n\n");

                    for (Map.Entry<Integer, ArrayList<String>> entry : Server.fileIdtoFileNameAndUploader.entrySet()) {

                        int fileId = entry.getKey();
                        ArrayList<String> meta = entry.getValue();

                        String uploader = meta.get(0);
                        String fileName = meta.get(1);
                        String visibility = meta.get(2);

                        // only show public files
                        if (!visibility.equalsIgnoreCase("public"))
                            continue;

                        response.append("[File ID: ").append(fileId).append("]\n");
                        response.append("Uploader : ").append(uploader).append("\n");
                        response.append("Filename : ").append(fileName).append("\n\n");
                    }
                    response.append("choose a File ID\n");
                    out.writeObject(response.toString());

                    String choice = (String) in.readObject();
                    int choiceClient = Integer.parseInt(choice);
                    ArrayList<String> file = Server.fileIdtoFileNameAndUploader.get(choiceClient);
                    String s1 = file.get(0); // username
                    String s2 = file.get(1); // filename
                    String s3 = file.get(2);// public or private


                    //send filename to client
                    out.writeObject(s2);

                    //send the max buffer size 
                    out.writeObject(String.valueOf(Server.MAX_BUFFER_SIZE));

                    String path = "./" + s1 + "/" + s3 + "/" + s2;
                    System.out.println(path + " what im sending");

                    sendFile(path, dataOutputStream, in);
                    String x = "File sent to client (download)";
                    out.writeObject(x);
                }

            }
            this.dataInputStream.close();
            this.dataOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void receiveFile(String fileName, int chunksize, String providedUserName, ObjectOutputStream out,
            String publicOrPrivate, DataInputStream dataInputStream)
            throws Exception {
        int bytes = 0;
        String fileNewName = "./" + providedUserName + "/" + publicOrPrivate + "/" + fileName;
        File file = new File(fileNewName);

        // Create parent directory if missing
        file.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        System.out.println("here reached");
        long size = dataInputStream.readLong(); // read file size
        System.out.println("file size: " + size);

        // reducing the size of the buffer
        Server.CURR_BUFFER_SIZE += size;
        long initSize = size;
        System.out.println("Server Current Buffer Size: " + Server.CURR_BUFFER_SIZE);
        byte[] buffer = new byte[chunksize];
        int checkSize = 0;
        while (size > 0
                && (bytes = dataInputStream.read(
                        buffer, 0,
                        (int) Math.min(buffer.length, size))) != -1) {
            // Here we write the file using write method
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes; // read upto file size
            System.out.println("bytes: " + bytes);

            // send acknowledgement to clinet that server got this chunk
            String s = "Server received the chunk of size " + bytes + " bytes";
            out.writeObject(s);
            checkSize += bytes;

        }
        // Here we received file
        System.out.println("File is Received");

        // buffer size is restoring to the previous size
        Server.CURR_BUFFER_SIZE -= initSize;
        if (checkSize == initSize) {
            String s = "successful reception of whole file of size " + initSize + " bytes";
            out.writeObject(s);
        } else {
            String s = "Didn't get all the bytes, deleting garbage chunks";
            out.writeObject(s);
            // delete the chunks ->how?
        }
        System.out.println("Server Current Buffer Size: " + Server.CURR_BUFFER_SIZE);
        fileOutputStream.close();
    }

    public static void upload(ObjectInputStream in, ObjectOutputStream out, String providedUserName,
            DataInputStream dataInputStream, String publicOrPrivate) throws Exception {

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

        while (Server.CURR_BUFFER_SIZE + Integer.parseInt(p[1]) > Server.MAX_BUFFER_SIZE) {
            serverResponse = "wrong choice! Cannot upload this file due to its size right now. Provide another file_name";
            out.writeObject(serverResponse);
            System.out.println("Buffer Size exceeded! Cannot upload file to server");
            clientResponse = (String) in.readObject();
            p = clientResponse.split(",");
        }

        Random r = new Random();
        int chunksize = r.nextInt(Server.MAX_CHUNK_SIZE - Server.MIN_CHUNK_SIZE + 1)
                + Server.MIN_CHUNK_SIZE;
        System.out.println("Chunk size: " + chunksize);

        String fileId = String.valueOf(Server.fileId);
        ArrayList<String> al = new ArrayList<String>();
        al.add(providedUserName);
        al.add(p[0]); // p[0] is filename
        al.add(publicOrPrivate);
        Server.fileIdtoFileNameAndUploader.put(Server.fileId, al);

        Server.fileId++;
        serverResponse = String.valueOf(chunksize) + "," + fileId;
        out.writeObject(serverResponse);
        receiveFile(p[0], chunksize, providedUserName, out, publicOrPrivate, dataInputStream);
        // adding the size of the buffer again (the )
    }

    public static void sendFile(String path, DataOutputStream dataOutputStream, ObjectInputStream in)
            throws Exception {
        int bytes = 0;
        // Open the File where he located in your pc
        File file = new File(path);
        System.out.println(file);
        FileInputStream fileInputStream = new FileInputStream(file);
        // Here we send the File to Server
        dataOutputStream.writeLong(file.length());
        // Here we break file into chunks
        byte[] buffer = new byte[Server.MAX_BUFFER_SIZE];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            // Send the file to Server Socket
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();
        }

        // sending complete, get accomplishment/failure messege
        String serverReponse = (String) in.readObject();
        System.out.println(serverReponse);

        // close the file here
        fileInputStream.close();
    }

}
