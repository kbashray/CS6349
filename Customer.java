import java.io.*;
import java.net.*;
import java.util.*;
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
        //KeyUtil.generateKeys("c" + id);

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

        String eMsg = MsgUtil.encryptAndSignMsg("chal" + r, bKey, privateKey);
        dos.writeUTF("broker#" + eMsg);

        String chalRe = MsgUtil.decryptMsg(dis.readUTF(), bKey, privateKey);
        if (!chalRe.equals(r))
            throw new Exception("broker validation failed");
        else
            System.out.println("broker validated");

        String dMsg = MsgUtil.decryptMsg(dis.readUTF(), bKey, privateKey);
        eMsg = MsgUtil.encryptAndSignMsg(dMsg, bKey, privateKey);
        dos.writeUTF(eMsg);

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
