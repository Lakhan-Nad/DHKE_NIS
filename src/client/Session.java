/**
 * The Class to maintain a single session connected by the Client
 */
package client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;

class Session {
    private String serverId;
    private BigInteger sessionKey;
    private BigInteger privateSessionKey;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final String ip;
    private final int port;

    /**
     * @param ip the ip address or host to connect
     * @param port the port number to identify the process
     */
    public Session(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * A utility Function to calculate Session Key
     * @param p The prime number is needed to calculate the session key
     *          Session key is a random number between 1 and p-1
     * @return THe session key
     */
    private static BigInteger calcPrivateSessionKey(BigInteger p) {
        Random rand = new Random();
        BigInteger randomLong = BigInteger.valueOf(rand.nextLong());
        BigInteger midState = p.multiply(randomLong);
        return midState.divideAndRemainder(BigInteger.valueOf(Long.MAX_VALUE))[0];
    }

    /**
     * Calculates the session key
     * @param serverPublicKey public key returned by server
     * @param p the prime no
     * @return the calculated session key
     */
    private BigInteger calcSessionKey(BigInteger serverPublicKey, BigInteger p) {
        return serverPublicKey.modPow(privateSessionKey, p);
    }

    /**
     * Function called to connect to Server
     * @return if connection was established or not
     */
    public boolean connect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                System.out.println("Client busy couldn't establish new connection");
            }
        }
        /* Documentation */
        System.out.println("Sending Connection Request To Server");
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            System.out.println("Invalid Host or Port provided");
            return false;
        }
        return establishIO();
    }

    /**
     * To check if IO from server is established or not
     * @return boolean for same
     */
    private boolean establishIO() {
        if (socket == null) {
            System.out.println("Establish a Connection Before");
            return false;
        }
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                System.out.println("Input Channel Error");
                return false;
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                System.out.println("Output Channel Error");
                return false;
            }
        }
        try {
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (Exception e) {
            System.out.println("Unable to open a I/O Channel");
            return false;
        }
        return true;
    }

    /**
     * Receive Keys from the Server
     * @param p prime number
     */
    public void receiveKeys(BigInteger p) {
        System.out.println("Waiting for Server's Public Key");
        String[] receivedData;
        try {
            receivedData = in.readUTF().split("\\s+");
        } catch (Exception e) {
            System.out.println("Unable to get back Server's Public Key");
            return;
        }
        serverId = receivedData[0];
        BigInteger serverPublicKey;
        try {
            serverPublicKey = new BigInteger(receivedData[1]);
        } catch (Exception e) {
            System.out.println("Invalid key received");
            return;
        }
        sessionKey = calcSessionKey(serverPublicKey, p);
        /* Documentation */
        System.out.println("Session key Established");
        System.out.println("Session Key:" + sessionKey.toString());
    }

    /**
     * Function to start the Key Request
     * @param id the client id, to send the server a client's info
     * @param p the prime no
     * @param g the generator no
     */
    public void keyRequest(String id, BigInteger p, BigInteger g) {
        if (socket == null) {
            System.out.println("First Establish a Connection");
            return;
        }
        if (in == null || out == null) {
            establishIO();
        }
        /* Documentation */
        System.out.println("Starting Key Exchange");
        privateSessionKey = calcPrivateSessionKey(p);
        BigInteger publicSessionKey = g.modPow(privateSessionKey,p);
        // sending keys to client
        // First build the message with a particular format
        // "id g^x\n"
        StringBuilder buf = new StringBuilder();
        buf.append(id);
        buf.append(' ');
        buf.append(publicSessionKey.toString());
        buf.append('\n');
        try {
            // send the keys
            out.writeUTF(buf.toString());
            out.flush();
        } catch (Exception e) {
            System.out.println("Unable to initiate a session");
        }
        /* Documentation */
        System.out.println("Request for Key Exchange Sent to Server");
    }

    /**
     * Close the connection
     */
    public void close() {
        try {
            if (socket != null)
                socket.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        } catch (Exception e) {
            System.out.println("Unable to close Resources");
        }
        /* Documentation */
        System.out.println("Connection with Server Closed");
    }

    /**
     * A function to communicate from the server
     */
    public void communicate() {
    }
}
