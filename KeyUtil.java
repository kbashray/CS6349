import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyUtil {
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
