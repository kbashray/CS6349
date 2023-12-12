import com.sun.net.httpserver.Authenticator;

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

            dMsg = MsgUtil.decryptMsg(dis.readUTF(), bKey, privateKey);
            if (dMsg.equals("success"))
                loggedIn = true;
            else
                System.out.println("Username or pass was incorrect. Please try again.");
        } while (!loggedIn);
        System.out.println("Login successful");

        // RSA authentication with merchant
        bytes = new byte[32];
        new Random().nextBytes(bytes);
        r = Base64.getEncoder().encodeToString(bytes);

        eMsg = MsgUtil.encryptAndSignMsg("chal" + r, mKey, privateKey);
        dos.writeUTF("merchant#" + eMsg);

        dMsg = MsgUtil.decryptMsg(dis.readUTF().split("#")[1], mKey, privateKey);
        if (!dMsg.equals(r))
            throw new Exception("merchant validation failed");
        else
            System.out.println("merchant validated");

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
