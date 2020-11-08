package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public final class Server {
    private static String id;
    private static ServerSocket server;

    private Server() {
        // Empty Constructor
    }

    /**
     * A function to check if the required command line arguments were passed or not
     * It also creates the socket for listening connections
     * @param args command line arguments
     */
    private static void handleArgs(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected Server Id and Port Number");
            System.exit(1);
        }
        id = args[0];
        server = null;
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Port is not a valid number");
            return;
        }
        // try creating a ServerSocket
        try {
            server = new ServerSocket(port);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * A function that handles a particular client
     * This function opens a client and returns a thread
     * @param socket the socket representing a client
     * @return thread created to handle client
     */
    private static Thread handleConnection(Socket socket) {
        System.out.println("Connection request from: " + socket.getRemoteSocketAddress().toString());
        Thread th = new ClientThread(socket, id);
        try {
            th.start();
        } catch (Exception e) {
            return null;
        }
        return th;
    }

    /**
     * Main function to start a client running
     * @param args command line arguments
     */
    public static void main(String[] args) {
        handleArgs(args);
        if (server == null) {
            System.out.println("Unable to initiate a server");
            System.exit(1);
        }
        ArrayList<Thread> clientThreads = new ArrayList<>();
        // server is ready
        /* Documentation */
        System.out.println("Server Started At: " + server.getLocalPort());
        int connections = 2;
        // The main loop to listen for connections
        while (connections > 0) {
            Socket socket = null;
            try {
                socket = server.accept();
            } catch (Exception e) {
                System.out.println("Unable to accept request");
            }
            if (socket != null) {
                Thread newClient = handleConnection(socket);
                if(newClient != null)
                    clientThreads.add(newClient);
                else
                    System.out.println("The client connection refused unable to allocate a new thread");
                connections--;
            } else {
                System.out.println("Connection Error");
            }
        }
        for(int i = 0; i < clientThreads.size(); i++){
            try {
                clientThreads.get(i).join();
            }catch (InterruptedException e){
                System.out.println("A client thread was interrupted");
            }
        }
        // Close the server/ listening socket
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(0);
            }
        }
    }
}
