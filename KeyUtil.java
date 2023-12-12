import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.*;

public class KeyUtil {
    public static void generateKeys(String name) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        try (FileOutputStream fos = new FileOutputStream(name + "Public.key")) {
            fos.write(publicKey.getEncoded());
        }
        try (FileOutputStream fos = new FileOutputStream(name + "Private.key")) {
            fos.write(privateKey.getEncoded());
        }
    }

    public static PublicKey getPublicKey(String path) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        File publicKeyFile = new File(path);
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());

        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static PrivateKey getPrivateKey(String path) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        File privateKeyFile = new File(path);
        byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());

        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return keyFactory.generatePrivate(privateKeySpec);
    }
}
