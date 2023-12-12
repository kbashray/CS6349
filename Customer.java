import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import java.security.*;

public class Customer  {
    final static int ServerPort = 1234;

    // Public private key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

    // Broker and merchant public keys
    static PublicKey bKey, mKey;

    public static void main(String[] args) throws Exception {
        int id = Integer.parseInt(args[0]);

        // Used to generate public private key pair
        /*
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        try (FileOutputStream fos = new FileOutputStream("c" + id + "Public.key")) {
            fos.write(publicKey.getEncoded());
        }
        try (FileOutputStream fos = new FileOutputStream("c" + id + "Private.key")) {
            fos.write(privateKey.getEncoded());
        }
        */

        // Read public private keys from files
        publicKey = KeyUtil.getPublicKey("c" + id + "Public.key");
        privateKey = KeyUtil.getPrivateKey("c" + id + "Private.key");

        bKey = KeyUtil.getPublicKey("bPublic.key");

        mKey = KeyUtil.getPublicKey("mPublic.key");

        Scanner scn = new Scanner(System.in);

        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        Socket s = new Socket(ip, ServerPort);

        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        dos.writeUTF("customer" + id);

        byte[] bytes = new byte[32];
        new Random().nextBytes(bytes);
        String r = Base64.getEncoder().encodeToString(bytes);

        String msg = MsgUtil.encryptAndSignMsg("chal#" + r, bKey, privateKey);

        dos.writeUTF("broker#" + msg);

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
