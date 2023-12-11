import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

public class BrokerServer {
    // Port number for the broker server
    final static int BrokerPort = 1234;

    // List to store connected customers
    static List<ClientHandler> customers = new ArrayList<>();
    // Single merchant handler
    static ClientHandler merchant = null;
    private static final Map<String, String> userCredentials = new HashMap<>();

    private static PrivateKey brokerPrivateKey;
    private static PublicKey brokerPublicKey;
    private static PublicKey customerPublicKey; // For demonstration
    private static PublicKey merchantPublicKey; // For demonstration

    static {
        // Initialize with some dummy user credentials
        userCredentials.put("user1", "password1");
        userCredentials.put("user2", "password2");
    }

    private static boolean authenticateUser(String username, String password) {
        String storedPassword = userCredentials.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    private static void generateOrLoadKeys(String type) throws Exception {
        File privateKeyFile = new File(type + "PrivateKey");
        File publicKeyFile = new File(type + "PublicKey");
        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            KeyPair keyPair = KeyPairUtil.generateKeyPair();
            KeyPairUtil.saveKeyToFile(type + "PrivateKey", keyPair.getPrivate().getEncoded());
            KeyPairUtil.saveKeyToFile(type + "PublicKey", keyPair.getPublic().getEncoded());
        }
    }

    public static void main(String[] args) throws Exception {
         // Load or generate keys for the broker
         generateOrLoadKeys("broker");
         brokerPrivateKey = KeyPairUtil.loadPrivateKey("brokerPrivateKey");
         brokerPublicKey = KeyPairUtil.loadPublicKey("brokerPublicKey");
 
         // Load or generate keys for customers and merchants
         generateOrLoadKeys("customer");
         generateOrLoadKeys("merchant");
         customerPublicKey = KeyPairUtil.loadPublicKey("customerPublicKey");
         merchantPublicKey = KeyPairUtil.loadPublicKey("merchantPublicKey");
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(BrokerPort);
            while (true) {
                Socket s = ss.accept();
                System.out.println("New connection: " + s);

                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());

                String initMsg = dis.readUTF();
                ClientHandler clientHandler;

                if (initMsg.startsWith("CUSTOMER")) {
                    clientHandler = new ClientHandler(s, initMsg, dis, dos, brokerPrivateKey, merchantPublicKey); // Customer needs Merchant's public key
                    customers.add(clientHandler);
                } else if (initMsg.startsWith("MERCHANT")) {
                    clientHandler = new ClientHandler(s, initMsg, dis, dos, brokerPrivateKey, customerPublicKey); // Merchant needs Customer's public key
                    merchant = clientHandler;
                } else {
                    continue; // Or handle unknown client type
                }

                Thread t = new Thread(clientHandler);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (ss != null && !ss.isClosed()) {
                try {
                    ss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}