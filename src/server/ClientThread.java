package server;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;

public final class ClientThread extends Thread {
    private static final long threadSleep = 3000; // 3 seconds
    private static final BigInteger P;
    private static final BigInteger G;
    private final String serverId;
    private final Socket socket;
    private final String clientAddress;
    private BigInteger sessionKey;
    DataInputStream in = null;
    DataOutputStream out = null;

    /**
     * The G and P constants same for
     * both client and server and available to all.
     */
    static {
        P = new BigInteger("B10B8F96A080E01DDE92DE5EAE5D54EC52C99FBCFB06A3C69A6A9DCA52D23B616073E28675A23D189838EF1E2EE652C013ECB4AEA906112324975C3CD49B83BFACCBDD7D90C4BD7098488E9C219A73724EFFD6FAE5644738FAA31A4FF55BCCC0A151AF5F0DC8B4BD45BF37DF365C1A65E68CFDA76D4DA708DF1FB2BC2E4A4371", 16);
        G = new BigInteger("A4D1CBD5C3FD34126765A442EFB99905F8104DD258AC507FD6406CFF14266D31266FEA1E5C41564B777E690F5504F213160217B4B01B886A5E91547F9E2749F4D7FBD7D3B9A92EE1909D0D2263F80A76A6A24C087A091F531DBF0A0169B6A28AD662A4D18E73AFA32D779D5918D08BC8858F4DCEF97C2A24855E6EEB22B3B2E5", 16);
    }

    ClientThread(Socket socket, String id) {
        this.serverId = id;
        this.socket = socket;
        this.clientAddress = socket.getRemoteSocketAddress().toString();
    }

    /**
     * To see if the I/O Channels are available for a given client
     * and if yes open them
     */
    private void establishIO() {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                System.out.println("Unable to close Input Stream");
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                System.out.println("Unable to close Output Stream");
            }
        }
        try {
            if (!socket.isInputShutdown())
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            if (!socket.isInputShutdown())
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (Exception e) {
            System.out.println("Unable to open communication streams");
            return;
        }
        if (in == null || out == null) {
            System.out.println("Communication Channels Unavailable");
        }
    }

    /**
     * To run this particular client thread
     * Entry point of client communication
     */
    public void run() {
        int noOfKeyExchanges = 1;
        establishIO();
        for(int i = 0; i < noOfKeyExchanges; i++) {
            keyExchange();
        }
        //communicate();
        try {
            sleep(threadSleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        close();
    }

    /**
     * A utility to close the connection and
     * release the resources
     */
    private void close() {
        try {
            if (socket != null)
                socket.close();
            if (out != null)
                out.close();
            if (in != null)
                in.close();
        } catch (Exception e) {
            System.out.println("Unable to Close the Connection");
        }
        /* Documentation */
        System.out.println("Connection Closed of: " + this.clientAddress);
    }

    /**
     * A utility to handle keu exchange with client
     *
     * The function handles the cases of receiving key from client
     * and then also send back the keys.
     */
    private void keyExchange() {
        if (in == null || out == null) {
            establishIO();
        }
        String[] clientInfo;
        // wait for KeyExchange to Initiate for 2 Minutes;
        long waitStart = System.currentTimeMillis();
        while(true) {
            try {
                clientInfo = in.readUTF().split("\\s+");
                break;
            } catch (IOException e) {
                // 2 minutes
                long maxWait = 2 * 60 * 1000;
                if(System.currentTimeMillis() - waitStart > maxWait){
                    return;
                }
            }
        }
        // if message is received then break the message
        // into client id client's public session key received
        String clientId = clientInfo[0];
        BigInteger clientKey;
        if (clientInfo.length == 2) {
            try {
                clientKey = new BigInteger(clientInfo[1]);
            } catch (Exception e) {
                System.out.println("Invalid keys provided");
                return;
            }
        } else {
            System.out.println("Invalid Key Exchange");
            return;
        }
        /* Documentation */
        System.out.println("Key Exchange Request Received form: " + this.clientAddress);

        BigInteger privateKey = calcPrivateSessionKey();
        BigInteger publicKey = calcPublicKey(privateKey);

        /* Documentation */
        System.out.println("Sending Public Key back to Client: " + this.clientAddress);

        // sending back keys
        // format of key change
        // "server-id public-key-server"
        StringBuilder buf = new StringBuilder();
        buf.append(serverId);
        buf.append(' ');
        buf.append(publicKey.toString());
        buf.append('\n');
        try {
            out.writeUTF(buf.toString());
        } catch (Exception e) {
            System.out.println("Unable to send keys back");
            return;
        }
        try {
            out.flush();
        } catch (Exception e) {
            System.out.println("Unable to send keys back");
        }

        sessionKey = calcSessionKey(clientKey, privateKey);
        /* Documentation */
        System.out.println("Session Key Established");
        System.out.println("Session Key: " + sessionKey.toString());
    }

    /**
     * A utility to communicate with client
     */
    private void communicate() {
    }

    /**
     * A utility function
     * @return A random key that will be private session key
     * for this session with this client
     */
    private static BigInteger calcPrivateSessionKey() {
        Random rand = new Random();
        BigInteger randomLong = BigInteger.valueOf(rand.nextLong());
        BigInteger midState = P.multiply(randomLong);
        return midState.divideAndRemainder(BigInteger.valueOf(Long.MAX_VALUE))[0];
    }

    /**
     * To calculate the public key of server
     * @param privateKey the private key of server
     * @return the calculated public key
     */
    private static BigInteger calcPublicKey(BigInteger privateKey) {
        return G.modPow(privateKey, P);
    }

    /**
     * A function to calculate session key
     * @param clientPublicKey the client's public key
     * @param privateKey the server's private session key
     * @return the session key
     */
    private static BigInteger calcSessionKey(BigInteger clientPublicKey, BigInteger privateKey) {
        return clientPublicKey.modPow(privateKey, P);
    }
}
