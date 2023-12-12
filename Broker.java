import java.io.*;
import java.util.*;
import java.net.*;
import java.security.*;

public class Broker {
    // Hash map to store active clients
    static Map<String, ClientHandler> ar = new HashMap<>();

    // Hash map to store usernames and passwords
    static Map<String, String> ac = new HashMap<>();

    // Public private key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

    // Customer and merchant public keys
    static PublicKey c1Key, c2Key, mKey;

    public static void main(String[] args) throws Exception {
        // Load usernames and passwords from file
        Scanner sc = new Scanner(new File("passwords.txt"));
        while (sc.hasNextLine()) {
            String[] account = sc.nextLine().split(":");
            ac.put(account[0], account[1]);
        }

        // Used to generate public private key pair
        //KeyUtil.generateKeys("b");

        // Read keys from files
        publicKey = KeyUtil.getPublicKey("bPublic.key");
        privateKey = KeyUtil.getPrivateKey("bPrivate.key");

        c1Key = KeyUtil.getPublicKey("c1Public.key");
        c2Key = KeyUtil.getPublicKey("c2Public.key");

        mKey = KeyUtil.getPublicKey("mPublic.key");

        // server is listening on port 1234
        ServerSocket ss = new ServerSocket(1234);

        Socket s;

        // running infinite loop for getting
        // client request
        while (true) {
            // Accept the incoming request
            s = ss.accept();

            System.out.println("New client request received : " + s);

            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            String name = dis.readUTF();

            PublicKey key = switch (name) {
                case "customer1" -> c1Key;
                case "customer2" -> c2Key;
                default -> mKey;
            };

            System.out.println("Creating a new handler for this client...");

            // Create a new handler object for handling this request.
            ClientHandler mtch = new ClientHandler(s, name, key, dis, dos);

            // Create a new Thread with this object.
            Thread t = new Thread(mtch);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            ar.put(name, mtch);

            // start the thread.
            t.start();
        }
    }
}

// ClientHandler class
class ClientHandler implements Runnable {
    Scanner scn = new Scanner(System.in);
    private final String name;
    private final PublicKey key;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isLoggedIn;

    // constructor
    public ClientHandler(Socket s, String name, PublicKey key, DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.key = key;
        this.name = name;
        this.s = s;
        this.isLoggedIn = name.equals("merchant"); // merchant starts logged in, customers have to log in manually
    }

    @Override
    public void run() {
        String received;
        while (true) {
            try {
                // receive the string
                received = dis.readUTF();

                System.out.println(received);

                if(received.equals("logout")) {
                    this.isLoggedIn = false;
                    this.s.close();
                    break;
                }

                // break the string into message and recipient part
                String[] msg = received.split("#");

                if (msg[0].equals("broker")) {
                    String dMsg = MsgUtil.decryptMsg(msg[1], key, Broker.privateKey);
                    String code = dMsg.substring(0, 4);
                    dMsg = dMsg.substring(4);
                    switch (code) {
                        case "chal":
                            String eMsg = MsgUtil.encryptAndSignMsg(dMsg, key, Broker.privateKey);
                            dos.writeUTF(eMsg);

                            byte[] bytes = new byte[32];
                            new Random().nextBytes(bytes);
                            String r = Base64.getEncoder().encodeToString(bytes);

                            eMsg = MsgUtil.encryptAndSignMsg(r, key, Broker.privateKey);
                            dos.writeUTF(eMsg);

                            dMsg = MsgUtil.decryptMsg(dis.readUTF(), key, Broker.privateKey);
                            if (!dMsg.equals(r))
                                throw new Exception(name + " validation failed");
                            else
                                System.out.println(name + " validated");
                            break;
                        case "lgin":
                            String user = dMsg;
                            String pass = MsgUtil.decryptMsg(dis.readUTF(), key, Broker.privateKey);

                            if (Broker.ac.containsKey(user) && Broker.ac.get(user).equals(pass)) {
                                eMsg = MsgUtil.encryptAndSignMsg("success", key, Broker.privateKey);
                                this.isLoggedIn = true;
                            } else
                                eMsg = MsgUtil.encryptAndSignMsg("failure", key, Broker.privateKey);
                            dos.writeUTF(eMsg);
                            break;
                        default:
                            break;
                    }
                }
                else {
                    ClientHandler recipient = Broker.ar.get(msg[0]);
                    if (recipient.isLoggedIn) {
                        recipient.dos.writeUTF(this.name + "#" + msg[1]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            // closing resources
            this.dis.close();
            this.dos.close();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
