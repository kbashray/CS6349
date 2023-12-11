import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;
import java.util.Scanner;

public class Customer {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private PublicKey brokerPublicKey;
    private PrivateKey customerPrivateKey;

    public Customer(String address, int port, PrivateKey privateKey, PublicKey publicKey) {
        try {
            this.socket = new Socket(address, port);
            this.dis = new DataInputStream(socket.getInputStream());
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.customerPrivateKey = privateKey;
            this.brokerPublicKey = publicKey;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            // Authenticate with the broker
            authenticateWithBroker();

            // Start listening for messages from the server
            Thread readMessage = new Thread(() -> {
                while (true) {
                    try {
                        String msg = dis.readUTF();
                        System.out.println(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });

            readMessage.start();

            // Send requests to the broker
            Scanner scn = new Scanner(System.in);
            while (true) {
                String msgToSend = scn.nextLine();
                dos.writeUTF(msgToSend);

                if (msgToSend.equals("logout")) {
                    socket.close();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void authenticateWithBroker() throws Exception {
        // Send a message to the broker for authentication
        String authMessage = "CustomerAuth";
        String signedMessage = signMessage(authMessage, customerPrivateKey);
        dos.writeUTF(signedMessage);
    }

    private String signMessage(String message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    public static void main(String[] args) {
        try {
            // Load customer's private key and broker's public key
            PrivateKey customerPrivateKey = KeyPairUtil.loadPrivateKey("customerPrivateKey");
            PublicKey brokerPublicKey = KeyPairUtil.loadPublicKey("brokerPublicKey");

            Customer client = new Customer("127.0.0.1", 1234, customerPrivateKey, brokerPublicKey);
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
