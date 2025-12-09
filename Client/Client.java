package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
                    "Menu: 1. logout; \n2.Request a file \n3. Upload a file\n4.See List of Clients\n5.Unread messeges");
            int input;
            String nextInp = myObj.nextLine();
            input = Integer.parseInt(nextInp);
            if (input == 1) {
                msg = "logout";
                out.writeObject(msg);
                break;
            }
            if (input == 2) {
                msg = "req";
                out.writeObject(msg);
            }
            if (input == 3) {
                msg = "upload";
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

                int chunksize = Integer.parseInt(serverResponse);
                try {
                    sendFile(path, chunksize, dataOutputStream, in);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            if (input == 4) {
                msg = "list of clients";
                out.writeObject(msg);
                System.out.println(in.readObject());
            }

        }

        myObj.close();
        socket.close();
        dataInputStream.close();
        dataOutputStream.close();
    }

    // sendFile function defined here
    private static void sendFile(String path, int chunksize, DataOutputStream dataOutputStream, ObjectInputStream in)
            throws Exception {
        int bytes = 0;
        // Open the File where he located in your pc
        File file = new File(path);
        System.out.println(file);
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

        // sending complete, get accomplishment/failure messege
        String serverReponse = (String) in.readObject();
        System.out.println(serverReponse);

        // close the file here
        fileInputStream.close();
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

}
