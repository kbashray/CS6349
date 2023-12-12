import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

public class Merchant {
    final static int ServerPort = 1234;

    //Private public key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

    // Broker public key
    static PublicKey bKey, c1Key, c2Key;

    public static void main(String[] args) throws Exception {
        // Used to generate public private key pair
        //KeyUtil.generateKeys("m");

        // Read public private keys from files
        publicKey = KeyUtil.getPublicKey("mPublic.key");
        privateKey = KeyUtil.getPrivateKey("mPrivate.key");

        bKey = KeyUtil.getPublicKey("bPublic.key");
        c1Key = KeyUtil.getPublicKey("c1Public.key");
        c2Key = KeyUtil.getPublicKey("c2Public.key");

        Scanner scn = new Scanner(System.in);

        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        Socket s = new Socket(ip, ServerPort);

        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        dos.writeUTF("merchant");

        // RSA authentication with broker
        byte[] bytes = new byte[32];
        new Random().nextBytes(bytes);
        String r = Base64.getEncoder().encodeToString(bytes);

        String eMsg = MsgUtil.encryptAndSignMsg("chal" + r, bKey, privateKey);
        dos.writeUTF("broker#" + eMsg);

        String dMsg = MsgUtil.decryptMsg(dis.readUTF(), bKey, privateKey);
        if (!dMsg.equals(r))
            throw new Exception("broker validation failed");
        else
            System.out.println("broker validated");

        dMsg = MsgUtil.decryptMsg(dis.readUTF(), bKey, privateKey);
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
                        String received = dis.readUTF();
                        System.out.println(received);

                        String[] msg = received.split("#");
                        String sender = msg[0];
                        PublicKey key = switch (sender) {
                            case "customer1" -> c1Key;
                            case "customer2" -> c2Key;
                            default -> bKey;
                        };
                        String dMsg = MsgUtil.decryptMsg(msg[1], key, privateKey);

                        String code = dMsg.substring(0, 4);
                        dMsg = dMsg.substring(4);
                        switch (code) {
                            case "chal":
                                String eMsg = MsgUtil.encryptAndSignMsg(dMsg, key, privateKey);
                                dos.writeUTF(sender + "#" + eMsg);
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
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
