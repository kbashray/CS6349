import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;
import java.security.*;

public class Merchant
{
    final static int ServerPort = 1234;

    //Private public key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

    // Broker public key
    static PublicKey brokerKey;

    public static void main(String args[]) throws IOException
    {
        // Used to generate public private key pair
        /*
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            try (FileOutputStream fos = new FileOutputStream("merchant\\public.key")) {
                fos.write(publicKey.getEncoded());
            }
            try (FileOutputStream fos = new FileOutputStream("merchant\\private.key")) {
                fos.write(privateKey.getEncoded());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        // Read public private keys from files
        publicKey = getPublicKey("merchant\\public.key");
        privateKey = getPrivateKey("merchant\\private.key");

        brokerKey = getPublicKey("broker\\public.key");

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
        Thread sendMessage = new Thread(new Runnable()
        {
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
        Thread readMessage = new Thread(new Runnable()
        {
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

    private static PublicKey getPublicKey(String path) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            File publicKeyFile = new File(path);
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());

            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static PrivateKey getPrivateKey(String path) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            File privateKeyFile = new File(path);
            byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());

            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
