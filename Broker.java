import java.io.*;
import java.util.*;
import java.net.*;
import java.security.*;

public class Broker
{
    // Vector to store active clients
    static Vector<ClientHandler> ar = new Vector<>();

    // Public private key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

    // Customer and merchant public keys
    static PublicKey c1Key, c2Key, mKey;

    public static void main(String[] args) throws Exception {
        // Used to generate public private key pair
        /*
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        try (FileOutputStream fos = new FileOutputStream("bPublic.key")) {
            fos.write(publicKey.getEncoded());
        }
        try (FileOutputStream fos = new FileOutputStream("bPrivate.key")) {
            fos.write(privateKey.getEncoded());
        }
        */

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

            System.out.println("Creating a new handler for this client...");

            // Create a new handler object for handling this request.
            ClientHandler mtch = new ClientHandler(s, name, dis, dos);

            // Create a new Thread with this object.
            Thread t = new Thread(mtch);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            ar.add(mtch);

            // start the thread.
            t.start();
        }
    }
}

// ClientHandler class
class ClientHandler implements Runnable {
    Scanner scn = new Scanner(System.in);
    private final String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isLoggedIn;

    // constructor
    public ClientHandler(Socket s, String name, DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isLoggedIn = true;
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
                StringTokenizer st = new StringTokenizer(received, "#");
                String MsgToSend = st.nextToken();
                String recipient = st.nextToken();

                // search for the recipient in the connected devices list.
                // ar is the vector storing client of active users
                for (ClientHandler mc : Broker.ar) {
                    // if the recipient is found, write on its
                    // output stream
                    if (mc.name.equals(recipient) && mc.isLoggedIn) {
                        mc.dos.writeUTF(this.name + " : " + MsgToSend);
                        break;
                    }
                }
            } catch (IOException e) {
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
