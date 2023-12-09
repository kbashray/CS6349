import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;
import java.security.*;

public class Customer
{
    final static int ServerPort = 1234;

    //Private public key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

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

            try (FileOutputStream fos = new FileOutputStream("customer\\public" + id + ".key")) {
                fos.write(publicKey.getEncoded());
            }
            try (FileOutputStream fos = new FileOutputStream("customer\\private" + id + ".key")) {
                fos.write(privateKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        */

        int id = Integer.parseInt(args[0]);

        // Read public private keys from files
        try {
            File publicKeyFile = new File("customer\\public" + id + ".key");
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            publicKey = keyFactory.generatePublic(publicKeySpec);

            File privateKeyFile = new File("customer\\private" + id + ".key");
            byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());

            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Scanner scn = new Scanner(System.in);

        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        Socket s = new Socket(ip, ServerPort);

        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

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
}
