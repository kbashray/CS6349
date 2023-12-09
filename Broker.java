import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.net.*;
import java.security.*;
import java.security.spec.*;

public class Broker
{

    // Vector to store active clients
    static Vector<ClientHandler> ar = new Vector<>();

    // counter for clients
    static int i = 0;

    // Public private key pair
    static PublicKey publicKey;
    static PrivateKey privateKey;

    // Customer and merchant public keys
    static PublicKey cust1Key, cust2Key, merchKey;

    public static void main(String[] args) throws IOException
    {
        // Used to generate public private key pair
        /*
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            try (FileOutputStream fos = new FileOutputStream("broker\\public.key")) {
                fos.write(publicKey.getEncoded());
            }
            try (FileOutputStream fos = new FileOutputStream("broker\\private.key")) {
                fos.write(privateKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        */

        // Read keys from files
        publicKey = getPublicKey("broker\\public.key");
        privateKey = getPrivateKey("broker\\private.key");

        cust1Key = getPublicKey("customer\\public1.key");
        cust2Key = getPublicKey("customer\\public2.key");

        merchKey = getPublicKey("merchant\\public.key");

        // server is listening on port 1234
        ServerSocket ss = new ServerSocket(1234);

        Socket s;

        // running infinite loop for getting
        // client request
        while (true)
        {
            // Accept the incoming request
            s = ss.accept();

            System.out.println("New client request received : " + s);

            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            System.out.println("Creating a new handler for this client...");

            // Create a new handler object for handling this request.
            ClientHandler mtch = new ClientHandler(s,"client " + i, dis, dos);

            // Create a new Thread with this object.
            Thread t = new Thread(mtch);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            ar.add(mtch);

            // start the thread.
            t.start();

            // increment i for new client.
            // i is used for naming only, and can be replaced
            // by any naming scheme
            i++;

        }
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

// ClientHandler class
class ClientHandler implements Runnable
{
    Scanner scn = new Scanner(System.in);
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isloggedin;

    // constructor
    public ClientHandler(Socket s, String name,
                         DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin=true;
    }

    @Override
    public void run() {

        String received;
        while (true)
        {
            try
            {
                // receive the string
                received = dis.readUTF();

                System.out.println(received);

                if(received.equals("logout")){
                    this.isloggedin=false;
                    this.s.close();
                    break;
                }

                // break the string into message and recipient part
                StringTokenizer st = new StringTokenizer(received, "#");
                String MsgToSend = st.nextToken();
                String recipient = st.nextToken();

                // search for the recipient in the connected devices list.
                // ar is the vector storing client of active users
                for (ClientHandler mc : Broker.ar)
                {
                    // if the recipient is found, write on its
                    // output stream
                    if (mc.name.equals(recipient) && mc.isloggedin)
                    {
                        mc.dos.writeUTF(this.name+" : "+MsgToSend);
                        break;
                    }
                }
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
        try
        {
            // closing resources
            this.dis.close();
            this.dos.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
