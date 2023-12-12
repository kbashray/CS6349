import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
    static PublicKey bKey;

    // Hash maps to store customer symmetric keys
    static Map<String, SecretKey> skMap = new HashMap<>();
    static Map<String, IvParameterSpec> ivMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Used to generate public private key pair
        //KeyUtil.generateRSAKeys("m");

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

        // RSA authentication with broker
        byte[] bytes = new byte[32];
        new Random().nextBytes(bytes);
        String r = Base64.getEncoder().encodeToString(bytes);

        String eMsg = MsgUtil.encryptAndSignMsg("chal" + r, bKey, privateKey);
        dos.writeUTF("broker#" + eMsg);

        String dMsg = MsgUtil.decryptAndVerifyMsg(dis.readUTF(), bKey, privateKey);
        if (!dMsg.equals(r))
            throw new Exception("broker validation failed");
        else
            System.out.println("broker validated");

        dMsg = MsgUtil.decryptAndVerifyMsg(dis.readUTF(), bKey, privateKey);
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
                        if (msg[1].substring(0, 4).equals("chal")) {
                            msg = msg[1].substring(4).split(":");
                            String dMsg = MsgUtil.decryptMsgRSA(msg[0], privateKey);
                            String dKey = MsgUtil.decryptMsgRSA(msg[1], privateKey);
                            String dIv = MsgUtil.decryptMsgRSA(msg[2], privateKey);

                            SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(dKey), "AES");
                            IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode(dIv));

                            Merchant.skMap.put(sender, key);
                            Merchant.ivMap.put(sender, iv);

                            String eMsg = MsgUtil.encryptMsgAES(dMsg, key, iv);
                            dos.writeUTF(sender + "#" + eMsg);
                        } else {
                            String dMsg = MsgUtil.decryptMsgAES(msg[1], Merchant.skMap.get(sender), Merchant.ivMap.get(sender));
                            String code = dMsg.substring(0, 4);
                            dMsg = dMsg.substring(4);
                            switch (code) {
                                case "chal":

                                    break;
                                default:
                                    break;
                            }
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
