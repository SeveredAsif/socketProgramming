package Server;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

public class Worker extends Thread {
    Socket socket;
    ConcurrentHashMap<String, Integer> userMap;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    HashMap<Integer, Integer> unReadMap;

    // private static DataOutputStream dataOutputStream = null;
    // private static DataInputStream dataInputStream = null;

    public Worker(Socket socket, ConcurrentHashMap<String, Integer> userMap) {
        this.socket = socket;
        this.userMap = userMap;
        this.unReadMap = new HashMap<>();
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
            ArrayList<String> downloads = new ArrayList<String>();

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
                        if (userMap.get(entry) == 1) {
                            clients += ("," + entry + "(Online)");
                        } else {
                            clients += ("," + entry + "(Offline)");
                        }

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
                        int len = msgBox.size();
                        unReadMap.put(len - 1, 0);

                        Server.messageBox.put(p[1], msgBox);
                        // System.out.println(Server.messageBox.get(p[1]));
                        increaseReqId();
                    } else {
                        Server.reqIdtoUsername.put(Server.reqID, providedUserName);
                        for (String users : userMap.keySet()) {
                            ArrayList<String> msgBox = Server.messageBox.getOrDefault(users, new ArrayList<>());
                            String msg = "Req id of file:=" + String.valueOf(Server.reqID)
                                    + ", requested file description::"
                                    + p[0];
                            msgBox.add(msg);

                            int len = msgBox.size();
                            unReadMap.put(len - 1, 0);
                            Server.messageBox.put(users, msgBox);
                        }
                        increaseReqId();
                    }
                    s = "request done!";
                    out.writeObject(s);
                    System.out.println("Request of user " + providedUserName + " added and sent to recipient(s)");
                } else if (continuousListen.equalsIgnoreCase("read msgbox")) {
                    s = "Messages:\n ";
                    ArrayList<String> inbox = Server.messageBox.getOrDefault(providedUserName, new ArrayList<>());
                    int cnt = 0;
                    for (String x : inbox) {
                        if (unReadMap.getOrDefault(cnt, 0) == 0) {
                            s += "Msg: " + x + " (Unread)\n";
                            unReadMap.put(cnt, 1);
                        } else {
                            s += "Msg: " + x + " (Read)\n";
                        }
                        cnt++;

                    }
                    out.writeObject(s);
                } else if (continuousListen.equalsIgnoreCase("upload in response to request")) {
                    System.out.println(continuousListen);
                    s = "Messages:\n";
                    ArrayList<String> inbox = Server.messageBox.getOrDefault(providedUserName, new ArrayList<>());
                    for (String x : inbox) {
                        s += "Msg: " + x + "\n";
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
                    String msg = String.valueOf(reqId)
                            + " -> File with this request id (that you requested) has been uploaded by user "
                            + providedUserName;
                    msgBox.add(msg);

                    int len = msgBox.size();
                    unReadMap.put(len - 1, 0);
                    Server.messageBox.put(requesterName, msgBox);

                } else if (continuousListen.equalsIgnoreCase("see downloads and uploads")) {
                    File myObj = new File("./" + providedUserName + "/logs.txt");
                    String serverResponse = "1.Show Uploads\n2.Show Downloads";
                    ArrayList<String> uploads = new ArrayList<>();
                    ArrayList<String> downloaded = new ArrayList<>();

                    try (Scanner myReader = new Scanner(myObj)) {
                        while (myReader.hasNextLine()) {
                            String data = myReader.nextLine();
                            String p[] = data.split(",");
                            if (p[0].equalsIgnoreCase("upload")) {
                                uploads.add(data);
                            } else {
                                downloaded.add(data);
                            }
                            // serverResponse += data;
                            System.out.println(data);
                        }
                    } catch (FileNotFoundException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }

                    // String publicDirName = "./" + providedUserName + "/public";
                    // String privateDirName = "./" + providedUserName + "/private";
                    // File dir = new File(publicDirName);
                    // File[] dir_contents = dir.listFiles();

                    // String publicFiles = "public:\n";
                    // if (dir_contents != null) {
                    // for (int i = 0; i < dir_contents.length; i++) {
                    // publicFiles += dir_contents[i].getName() + "\n";
                    // }
                    // }

                    // dir = new File(privateDirName);
                    // dir_contents = dir.listFiles();

                    // String privateFiles = "private:\n";
                    // if (dir_contents != null) {
                    // for (int i = 0; i < dir_contents.length; i++) {
                    // privateFiles += dir_contents[i].getName() + "\n";
                    // }
                    // }

                    // String serverResponse = "uploads:\n" + publicFiles + privateFiles;
                    // serverResponse += "downloads:\n";
                    // for (String d : downloads) {
                    // serverResponse += d;
                    // }
                    out.writeObject(serverResponse);

                    // client will now say whether downloads or uploads is what he wants to see

                    String msg = (String) in.readObject();
                    int choice = Integer.parseInt(msg);
                    serverResponse = "";

                    if (choice == 1) {
                        serverResponse = formatLogEntries(uploads, "UPLOADS");
                    } else {
                        serverResponse = formatLogEntries(downloaded, "DOWNLOADS");
                    }
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
                        if (!visibility.equalsIgnoreCase("public")) {
                            if (!uploader.equalsIgnoreCase(providedUserName)) {
                                continue;
                            }
                        }

                        response.append("[File ID: ").append(fileId).append("]\n");
                        if (uploader.equals(providedUserName)) {
                            response.append("Uploader : ").append(uploader).append("(SELF)\n");
                        } else {
                            response.append("Uploader : ").append(uploader).append("\n");
                        }

                        if (visibility.equalsIgnoreCase("private")) {
                            response.append("Filename : ").append(fileName).append("(PRIVATE)\n\n");
                        } else {
                            response.append("Filename : ").append(fileName).append("(PUBLIC)\n\n");
                        }

                    }
                    response.append("choose a File ID, type \"No\" if you want to go back\n");
                    out.writeObject(response.toString());

                    String choice = (String) in.readObject();
                    if (choice.equalsIgnoreCase("No")) {
                        continue;
                    }
                    int choiceClient = Integer.parseInt(choice);
                    ArrayList<String> file = Server.fileIdtoFileNameAndUploader.get(choiceClient);
                    String s1 = file.get(0); // username
                    String s2 = file.get(1); // filename
                    String s3 = file.get(2);// public or private

                    // send filename to client
                    out.writeObject(s2);

                    // send the max buffer size
                    out.writeObject(String.valueOf(Server.MAX_CHUNK_SIZE));

                    String path = "./" + s1 + "/" + s3 + "/" + s2;
                    System.out.println(path + " what im sending");

                    int succ = sendFile(path, dataOutputStream, in, s2, providedUserName);
                    String x;
                    if (succ == 1) {
                        x = "File sent to client (download)";
                    } else {
                        x = "Failed to download properly";
                    }

                    System.out.println(x);
                    out.writeObject(x);

                    // adding to self downloads
                    downloads.add(s2);
                }

            }
            this.dataInputStream.close();
            this.dataOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void increaseReqId() {
        Server.reqID++;
    }

    public static synchronized void incrementFileId() {
        Server.fileId++;
    }

    public static synchronized void addToBufferSize(long size) {
        Server.CURR_BUFFER_SIZE += size;
    }

    public static void receiveFile(String fileName, int chunksize, String providedUserName, ObjectOutputStream out,
            String publicOrPrivate, DataInputStream dataInputStream, ObjectInputStream in)
            throws Exception {
        int bytes = 0;
        String fileNewName = "./" + providedUserName + "/" + publicOrPrivate + "/" + fileName;
        String logFileName = "./" + providedUserName + "/logs.txt";
        File file = new File(fileNewName);
        File logfile = new File(logFileName);

        // Create parent directory if missing
        file.getParentFile().mkdirs();
        logfile.getParentFile().mkdirs();

        // write to log file
        BufferedWriter logWriter = new BufferedWriter(
                new FileWriter(logfile, true));
        LocalDateTime myObj = LocalDateTime.now();

        String str = "upload," + fileName + "," + myObj.toString() + ",";

        // Writing on output stream
        logWriter.write(str);

        FileOutputStream fileOutputStream = new FileOutputStream(file);
        System.out.println("here reached");
        long size = dataInputStream.readLong(); // read file size
        System.out.println("file size: " + size);

        // reducing the size of the buffer
        addToBufferSize(size);
        long initSize = size;
        System.out.println("Server Current Buffer Size: " + Server.CURR_BUFFER_SIZE);
        byte[] buffer = new byte[chunksize];
        int checkSize = 0;
        try {
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

            String st = (String) in.readObject(); // acknowledgement from client -> upload done
            System.out.println(st);
            // Here we received file
            System.out.println("File is Received");

            // buffer size is restoring to the previous size
            addToBufferSize(-1 * initSize);
            fileOutputStream.close();
            if (checkSize == initSize) {
                String s = "successful reception of whole file of size " + initSize + " bytes";
                out.writeObject(s);
                logWriter.append("success\n");
            } else {
                String s = "Didn't get all the bytes, deleting garbage chunks";
                out.writeObject(s);
                logWriter.append("failure\n");
                // delete the chunks ->how?
                boolean a = file.delete();
                System.out.println("Didn't get all the bytes, deleting garbage chunks");

                if (a) {
                    System.out.println("Deleted successfully");
                } else {
                    System.out.println("Not successfully deleted");
                }
            }
        } catch (Exception e) {
            fileOutputStream.close();
            // TODO: handle exception
            boolean a = file.delete(); // exception came so deleting chunks
            System.out.println("Deleting chunks, as unexpected exception came from client side");
            logWriter.append("failure\n");

            if (a) {
                System.out.println("Deleted successfully");
            } else {
                System.out.println("Not successfully deleted");
            }
            addToBufferSize(-1 * initSize); // reducing buffer size from the size that was added
        }

        System.out.println("Server Current Buffer Size: " + Server.CURR_BUFFER_SIZE);

        logWriter.close();
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

        // while (Server.CURR_BUFFER_SIZE + Integer.parseInt(p[1]) >
        // Server.MAX_BUFFER_SIZE) {
        // serverResponse = "wrong choice! Cannot upload this file due to its size right
        // now. Provide another file_name";
        // out.writeObject(serverResponse);
        // System.out.println("Buffer Size exceeded! Cannot upload file to server");
        // clientResponse = (String) in.readObject();
        // p = clientResponse.split(",");
        // }

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
        incrementFileId();
        serverResponse = String.valueOf(chunksize) + "," + fileId;
        out.writeObject(serverResponse);
        receiveFile(p[0], chunksize, providedUserName, out, publicOrPrivate, dataInputStream, in);
        // adding the size of the buffer again (the )
    }

    public static int sendFile(String path, DataOutputStream dataOutputStream, ObjectInputStream in, String fileName,
            String userName)
            throws Exception {
        int bytes = 0;
        // Open the File where he located in your pc
        File file = new File(path);
        File logfile = new File("./" + userName + "/logs.txt");

        logfile.getParentFile().mkdirs();

        // write to log file
        BufferedWriter logWriter = new BufferedWriter(
                new FileWriter(logfile, true));
        LocalDateTime myObj = LocalDateTime.now();

        String str = "download," + fileName + "," + myObj.toString() + ",";

        // Writing on output stream
        logWriter.write(str);

        System.out.println(file);
        FileInputStream fileInputStream = new FileInputStream(file);
        // Here we send the File to Server
        dataOutputStream.writeLong(file.length());
        // Here we break file into chunks
        byte[] buffer = new byte[Server.MAX_CHUNK_SIZE];
        try {
            while ((bytes = fileInputStream.read(buffer)) != -1) {
                // Send the file to Server Socket
                dataOutputStream.write(buffer, 0, bytes);
                dataOutputStream.flush();
            }

            logWriter.append("success\n");
            // close the file here
            fileInputStream.close();
            logWriter.close();
            return 1;

        } catch (Exception e) {
            // TODO: handle exception
            fileInputStream.close();

            logWriter.append("failure\n");
            logWriter.close();
            return 0;
        }

    }

    private static String formatLogEntries(ArrayList<String> logEntries, String type) {
        if (logEntries.isEmpty()) {
            return "No " + type.toLowerCase() + " found.\n";
        }

        StringBuilder formatted = new StringBuilder();
        formatted.append("=== ").append(type).append(" ===\n\n");
        int count = 1;

        for (String entry : logEntries) {
            String[] parts = entry.split(",");
            if (parts.length >= 4) {
                String fileName = parts[1].replace("./", "");
                String dateTime = parts[2].replace("T", " ").substring(0, 19);
                String status = parts[3].equalsIgnoreCase("success") ? "✓ Success" : "✗ Failed";

                formatted.append(String.format("[%d] %s\n", count, fileName));
                formatted.append(String.format("    Date/Time: %s\n", dateTime));
                formatted.append(String.format("    Status: %s\n\n", status));
                count++;
            }
        }

        return formatted.toString();
    }

}
