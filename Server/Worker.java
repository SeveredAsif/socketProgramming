package Server;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class Worker extends Thread {
    Socket socket;
    HashMap<String, Integer> userMap;

    public Worker(Socket socket, HashMap<String, Integer> userMap) {
        this.socket = socket;
        this.userMap = userMap;
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
                    serverResponse = "your file has been uploaded!";
                    out.writeObject(serverResponse);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
