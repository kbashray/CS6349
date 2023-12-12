import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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

    // Merchant symmetric key
    static SecretKey sKey;
    static IvParameterSpec iv;

    public static void main(String[] args) throws Exception {
        int id = Integer.parseInt(args[0]);

        // Used to generate public private key pair
        //KeyUtil.generateRSAKeys("c" + id);

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

        // RSA authentication with broker
        byte[] bytes = new byte[32];
        new Random().nextBytes(bytes);
        String r = Base64.getEncoder().encodeToString(bytes);

        String eMsg = MsgUtil.encryptAndSignMsg("chal" + r, bKey, privateKey);
        dos.writeUTF("broker#" + eMsg);

        String dMsg = MsgUtil.decryptAndVerifyMsg(dis.readUTF(), bKey, privateKey);
        if (!dMsg.equals(r))
            throw new Exception("broker validation failed");

        dMsg = MsgUtil.decryptAndVerifyMsg(dis.readUTF(), bKey, privateKey);
        eMsg = MsgUtil.encryptAndSignMsg(dMsg, bKey, privateKey);
        dos.writeUTF(eMsg);

        // Login
        boolean loggedIn = false;
        do {
            System.out.print("Username: ");
            String user = scn.nextLine();
            System.out.print("Password: ");
            String pass = scn.nextLine();

            eMsg = MsgUtil.encryptAndSignMsg("lgin" + user, bKey, privateKey);
            dos.writeUTF("broker#" + eMsg);
            eMsg = MsgUtil.encryptAndSignMsg(pass, bKey, privateKey);
            dos.writeUTF(eMsg);

            dMsg = MsgUtil.decryptAndVerifyMsg(dis.readUTF(), bKey, privateKey);
            if (dMsg.equals("success"))
                loggedIn = true;
            else
                System.out.println("Username or pass was incorrect. Please try again.");
        } while (!loggedIn);
        System.out.println("Login successful");

        // RSA authentication with merchant and create session key
        bytes = new byte[32];
        new Random().nextBytes(bytes);
        r = Base64.getEncoder().encodeToString(bytes);

        sKey = KeyUtil.generateAESKey();
        iv = KeyUtil.generateIv();

        String ssKey = Base64.getEncoder().encodeToString(sKey.getEncoded());
        String sIv = Base64.getEncoder().encodeToString(iv.getIV());

        eMsg = MsgUtil.encryptMsgRSA(r, mKey);
        String eKey = MsgUtil.encryptMsgRSA(ssKey, mKey);
        String eIv = MsgUtil.encryptMsgRSA(sIv, mKey);
        dos.writeUTF("merchant#chal" + eMsg + ":" + eKey + ":" + eIv);

        dMsg = MsgUtil.decryptMsgAES(dis.readUTF().split("#")[1], sKey, iv);
        if (!dMsg.equals(r))
            throw new Exception("merchant validation failed");

        // Main loop
        while (true) {
            System.out.print("Enter a command (browse, purchase) >> ");
            String command = scn.nextLine();
            if (command.equals("browse")) {
                eMsg = MsgUtil.encryptMsgAES("brow", sKey, iv);
                dos.writeUTF("merchant#" + eMsg);

                dMsg = MsgUtil.decryptMsgAES(dis.readUTF().split("#")[1], sKey, iv);
                String[] products = dMsg.split("/");
                System.out.println("Available products:");
                for (String product : products)
                    System.out.println("\t" + product);
            } else if (command.equals("purchase")) {
                System.out.print("Enter product name >> ");
                String name = scn.nextLine();
                eMsg = MsgUtil.encryptMsgAES("purc" + name, sKey, iv);
                dos.writeUTF("merchant#" + eMsg);

                dMsg = MsgUtil.decryptMsgAES(dis.readUTF().split("#")[1], sKey, iv);
                if (dMsg.equals("invalid"))
                    System.out.println("Requested product not available.");
                else {
                    String transId = dMsg;
                    System.out.print("Enter payment info >> ");
                    String payment = scn.nextLine();

                    eMsg = MsgUtil.encryptAndSignMsg("tran" + transId, bKey, privateKey);
                    dos.writeUTF("broker#" + eMsg);
                    eMsg = MsgUtil.encryptAndSignMsg(payment, bKey, privateKey);
                    dos.writeUTF(eMsg);

                    dMsg = MsgUtil.decryptMsgAES(dis.readUTF().split("#")[1], sKey, iv);
                    System.out.println(dMsg);
                }
            } else
                System.out.println("Invalid command.");
        }
    }
}
