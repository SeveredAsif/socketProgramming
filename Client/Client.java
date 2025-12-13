package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Scanner myObj = new Scanner(System.in);
        Socket socket = new Socket("localhost", 6666);
        System.out.println("Connection established");
        System.out.println("Remote port: " + socket.getPort());
        System.out.println("Local port: " + socket.getLocalPort());

        // buffers
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;

        try {
            dataInputStream = new DataInputStream(
                    socket.getInputStream());
            dataOutputStream = new DataOutputStream(
                    socket.getOutputStream());
        } catch (Exception e) {
            // TODO: handle exception
        }

        System.out.println(dataOutputStream);

        //
        String msg = (String) in.readObject();
        System.out.println(msg);
        msg = myObj.nextLine();
        out.writeObject(msg);
        msg = (String) in.readObject();
        System.out.println(msg);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            // TODO: handle exception
        }
        while (true) {
            System.out.println(
                    "Menu: 1. logout; \n2. Upload a file\n3.See List of Clients\n4.Request a file\n5.Unread messages\n6.Upload in response to a request\n7.See Downloads and Uploads\n8.Download a file");
            int input;
            String nextInp = myObj.nextLine();
            input = Integer.parseInt(nextInp);
            if (input == 1) {
                msg = "logout";
                out.writeObject(msg);
                break;
            }
            if (input == 2) {

                System.out.println("1.Public Upload\n2.Private Upload");
                msg = myObj.nextLine();
                String publicOrPrivate;
                if (msg.equals("1")) {
                    publicOrPrivate = "public";
                } else {
                    publicOrPrivate = "private";
                }
                try {
                    upload(publicOrPrivate, in, out, myObj, dataOutputStream);
                } catch (Exception e) {
                    // TODO: handle exception
                }

            }
            if (input == 3) {
                msg = "list of clients";
                out.writeObject(msg);
                System.out.println(in.readObject());
            }
            if (input == 4) {
                msg = "request file";
                out.writeObject(msg);
                System.out.println("Provide file_description,recipient(individual or ALL)");
                msg = myObj.nextLine();
                String[] p = msg.split(",");
                while (p.length != 2) {
                    System.out.println("Wrong format! Provide file_description,recipient(individual or ALL)");
                    msg = myObj.nextLine();
                    p = msg.split(",");
                }
                out.writeObject(msg);

                msg = (String) in.readObject();
                System.out.println(msg);

            }
            if (input == 5) {
                msg = "read msgbox";
                out.writeObject(msg);
                msg = (String) in.readObject();
                System.out.println(msg);
            }
            if (input == 6) {
                msg = "upload in response to request";
                System.out.println(msg);
                out.writeObject(msg);
                msg = (String) in.readObject();
                System.out.println(msg);
                System.out.println("Provide a request id which you want to upload");
                String reqId = myObj.nextLine();
                System.out.println("You provided req id: " + reqId);
                try {
                    upload("public", in, out, myObj, dataOutputStream);
                } catch (Exception e) {
                    // TODO: handle exception
                    System.out.println("exception here");
                }
                out.writeObject(reqId);
            }
            if (input == 7) {
                msg = "see downloads and uploads";
                out.writeObject(msg);
                msg = (String) in.readObject();
                System.out.println(msg);

                // give choice -> DOWNLOADS or uploads
                msg = myObj.nextLine();
                out.writeObject(msg);

                // recieve the uploads/downloads logs
                msg = (String) in.readObject();
                System.out.println(msg);
            }
            if (input == 8) {
                msg = "download file";
                out.writeObject(msg);

                // downloadable files
                msg = (String) in.readObject();
                System.out.println(msg);

                // choose file to download
                msg = myObj.nextLine();
                out.writeObject(msg);

                if (msg.equalsIgnoreCase("No")) {
                    continue;
                }

                // read the filename from server
                String filename = (String) in.readObject();
                System.out.println("You chose this file to download: " + filename);

                // receive the chunksize (max buffer size from server)
                msg = (String) in.readObject();
                int MAX_BUFFER_SIZE = Integer.parseInt(msg);

                // receive the file
                System.out.println("Choose the path where you want to download: ");
                String providedPath = myObj.nextLine();

                try {
                    receiveFile(filename, MAX_BUFFER_SIZE, providedPath, out, dataInputStream);
                } catch (Exception e) {
                    // TODO: handle exception
                }

                // download done msg
                msg = (String) in.readObject();
                System.out.println(msg);

            }

        }

        myObj.close();
        socket.close();
        dataInputStream.close();
        dataOutputStream.close();
    }

    // sendFile function defined here
    private static void sendFile(String path, int chunksize, DataOutputStream dataOutputStream, ObjectInputStream in,
            ObjectOutputStream out)
            throws Exception {
        int bytes = 0;
        // Open the File where he located in your pc
        System.out.println("the path is " + path);
        File file = new File(path);
        System.out.println(file + " is the file ");
        FileInputStream fileInputStream = new FileInputStream(file);
        // Here we send the File to Server
        dataOutputStream.writeLong(file.length());
        // Here we break file into chunks
        byte[] buffer = new byte[chunksize];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            // Send the file to Server Socket
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();

            // wait for server acknowlegement of recieving this chunk
            String serverResponse = (String) in.readObject();
            System.out.println(serverResponse);
        }

        String msg = "Acknowledgement from client -> Upload completed";
        out.writeObject(msg);

        // sending complete, get accomplishment/failure messege (last chunk)
        String serverReponse = (String) in.readObject();
        System.out.println(serverReponse);

        // close the file here
        fileInputStream.close();
    }

    public static void upload(String publicOrPrivate, ObjectInputStream in, ObjectOutputStream out, Scanner myObj,
            DataOutputStream dataOutputStream) throws Exception {
        String msg = "upload," + publicOrPrivate;
        System.out.println(msg);
        out.writeObject(msg);

        // server response is -> send file name
        String serverResponse = (String) in.readObject();
        System.out.println(serverResponse);

        msg = constructFilePathWithSize(myObj);

        out.writeObject(msg);
        serverResponse = (String) in.readObject();
        System.out.println(serverResponse);
        while (serverResponse.contains("wrong")) {

            msg = constructFilePathWithSize(myObj);
            out.writeObject(msg);
            serverResponse = (String) in.readObject();
            System.out.println(serverResponse);
        }

        // the first part of msg has the path
        String[] p = msg.split(",");
        String path = p[0];

        p = serverResponse.split(",");
        int chunksize = Integer.parseInt(p[0]);
        System.out.println("the chunk size is " + chunksize);
        try {
            sendFile(path, chunksize, dataOutputStream, in, out);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    // Source - https://stackoverflow.com/q
    // Posted by gkris, modified by community. See post 'Timeline' for change
    // history
    // Retrieved 2025-12-09, License - CC BY-SA 3.0

    public static boolean checkExists(String directory, String file) {
        // File dir = new File(directory);
        // File[] dir_contents = dir.listFiles();
        boolean check = new File(file).exists();
        // System.out.println("Check" + check); // -->always says false

        return check;
    }

    public static String constructFilePathWithSize(Scanner myObj) {
        // msg contains file name
        // System.out.println("reaching before scan");
        String msg = myObj.nextLine();
        // System.out.println("reaching after scan");

        // check if it is in the directory

        // System.out.println(checkExists("./", msg));

        // if the file exists, go on. else take input again
        while (checkExists("./", msg) == false) {
            System.out.println("File doesn't exist in the current directory, please specify a correct file");
            // msg contains file name
            msg = myObj.nextLine();
        }

        // the file object with the pathname provided by user
        File file = new File("./" + msg);

        long file_size = file.length();
        System.out.println("file size: " + file_size);

        // constructing the client response to send to server
        msg = "./" + msg + "," + file_size;

        return msg;
    }

    public static void receiveFile(String fileName, int chunksize, String providedPath, ObjectOutputStream out,
            DataInputStream dataInputStream)
            throws Exception {
        int bytes = 0;
        String fileNewName = "./" + providedPath + "/" + fileName;
        File file = new File(fileNewName);

        // Create parent directory if missing
        file.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        System.out.println("here reached");
        long size = dataInputStream.readLong(); // read file size
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
        System.out.println("File is Downloaded");
        fileOutputStream.close();
    }

}
