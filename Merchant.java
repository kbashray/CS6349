import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.security.*;

public class Merchant {
    final static int ServerPort = 1234;

    //Private public key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

    // Broker public key
    static PublicKey bKey;

    public static void main(String[] args) throws Exception {
        // Used to generate public private key pair
        /*
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        try (FileOutputStream fos = new FileOutputStream("mPublic.key")) {
            fos.write(publicKey.getEncoded());
        }
        try (FileOutputStream fos = new FileOutputStream("mPrivate.key")) {
            fos.write(privateKey.getEncoded());
        }
        */

        // Read public private keys from files
        publicKey = KeyUtil.getPublicKey("mPublic.key");
        privateKey = KeyUtil.getPrivateKey("mPrivate.key");

        bKey = KeyUtil.getPublicKey("bPublic.key");

        Scanner scn = new Scanner(System.in);

        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        Socket s = new Socket(ip, ServerPort);

        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        dos.writeUTF("merchant");

        // sendMessage thread
        Thread sendMessage = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // read the message to deliver.
                    String msg = scn.nextLine();

                    try {
                        // write on the output stream
                        dos.writeUTF(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });

        // readMessage thread
        Thread readMessage = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // read the message sent to this client
                        String msg = dis.readUTF();
                        System.out.println(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });

        sendMessage.start();
        readMessage.start();
    }
}
