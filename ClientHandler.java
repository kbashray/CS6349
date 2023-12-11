import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

class ClientHandler implements Runnable {
    private String clientType;
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isLoggedIn;
    private String secretKey = "Your_Secret_Key"; // This should be a securely generated key


    // RSA keys (for simplicity, assuming they are already generated and loaded)
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private static final Map<String, String> userCredentials = new HashMap<>();

    static {
        userCredentials.put("user1", "password1");
        userCredentials.put("user2", "password2");
        // Add more users if needed
    }

    public ClientHandler(Socket s, String clientType, DataInputStream dis, DataOutputStream dos, PrivateKey privateKey, PublicKey publicKey) {
        this.s = s;
        this.clientType = clientType;
        this.dis = dis;
        this.dos = dos;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    @Override
    public void run() {
        try {
            if (clientType.startsWith("CUSTOMER")) {
                // Authenticate Customer using username/password
                if (!authenticateCustomer()) {
                    s.close();
                    return;
                }
            }

            authenticateClient();

            String received;
            while (true) {
                received = dis.readUTF();
                System.out.println(received);

                String[] parts = received.split(":", 2);
                if (parts.length == 2) {
                    String message = parts[0];
                    String hash = parts[1];

                    if (verifyKeyedHash(message, hash)) {
                        processMessage(message);
                    } else {
                        handleSecurityIssue();
                    }
                }

                if(received.equals("logout")){
                    this.isLoggedIn = false;
                    this.s.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (this.dis != null) this.dis.close();
                if (this.dos != null) this.dos.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean authenticateCustomer() throws IOException {
        String username = dis.readUTF();
        String password = dis.readUTF();

        if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
            dos.writeUTF("Authentication Successful");
            return true;
        } else {
            dos.writeUTF("Authentication Failed");
            return false;
        }
    }

    private void authenticateClient() {
        try {
            if (clientType.startsWith("CUSTOMER")) {
                // Broker authenticates to Customer
                String brokerSignedMessage = signMessage("BrokerAuth", privateKey);
                dos.writeUTF(brokerSignedMessage);
    
                // Customer authenticates Merchant (assuming the message is signed by the Merchant)
                // The actual merchant authentication should be done on the customer's side
                // by verifying a message signed by the Merchant's private key
    
            } else if (clientType.startsWith("MERCHANT")) {
                // Broker authenticates to Merchant
                String brokerSignedMessage = signMessage("BrokerAuth", privateKey);
                dos.writeUTF(brokerSignedMessage);
    
                // Additional logic if needed
                // ...
            }
            // Additional logic for Broker authenticating Customer using username/password
            // This part depends on how you manage usernames/passwords
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    private String signMessage(String message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }
    
    private String createKeyedHash(String message) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((message + secretKey).getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyKeyedHash(String message, String hash) {
        try {
            String newHash = createKeyedHash(message);
            return newHash.equals(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "NoSuchAlgorithmException in verifyKeyedHash", e);
            return false;
        }
    }

    private void processMessage(String message) {
        String[] messageParts = message.split(" ", 2);
        String command = messageParts[0];

        switch (command) {
            case "BROWSE":
                // Handle browse request
                handleBrowseRequest();
                break;
            case "PURCHASE":
                // Handle purchase request
                String productId = messageParts[1];
                handlePurchaseRequest(productId);
                break;
                case "ADD_PRODUCT":
                String productDetails = messageParts[1];
                handleAddProduct(productDetails);
                break;
            case "REMOVE_PRODUCT":
                productId = messageParts[1];
                handleRemoveProduct(productId);
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
        System.out.println("Verified message: " + message);
    }

    private void handleSecurityIssue() {
        // Implement your security issue handling logic here
        System.out.println("Security issue detected!");
    }

    private void sendMessage(String message) throws NoSuchAlgorithmException, IOException {
        String hash = createKeyedHash(message);
        dos.writeUTF(message + ":" + hash);
    }
    private void handleBrowseRequest() {
        // Assuming productList is a List<Product> available in the Merchant's server
        List<Product> productList = getProductsList(); 

        StringBuilder productListString = new StringBuilder();
        for (Product product : productList) {
            productListString.append(product.toString()).append("\n");
        }

        try {
            sendMessage("PRODUCT_LIST " + productListString.toString());
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Handle exception
        }      
    }
    
    private void handlePurchaseRequest(String productId) {
        boolean purchaseSuccess = MerchantServer.processPurchase(productId);

    String responseMessage = purchaseSuccess ? "PURCHASE_SUCCESS " + productId : "PURCHASE_FAILED " + productId;
    try {
        sendMessage(responseMessage);
    } catch (IOException | NoSuchAlgorithmException e) {
        e.printStackTrace();
        // Handle exception
    }
    }
    private List<Product> getProductsList() {
        return MerchantServer.getProductList();
    }

    private void handleRemoveProduct(String productId) {
        MerchantServer.removeProduct(productId);
    
        try {
            sendMessage("PRODUCT_REMOVED " + productId);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Handle exception
        }
    }

    private void handleAddProduct(String productDetails) {
        // Assuming productDetails are in the format "productId:description"
        String[] details = productDetails.split(":", 2);
        if (details.length == 2) {
            String productId = details[0];
            String description = details[1];
    
            Product newProduct = new Product(productId, description);
            MerchantServer.addProduct(newProduct);
    
            try {
                sendMessage("PRODUCT_ADDED " + productId);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                // Handle exception
            }
        } else {
            // Handle invalid product details format
        }
    }
    
    
}
