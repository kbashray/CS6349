import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

public class MsgUtil {
    public static String encryptAndSignMsg(String message, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] eBytes = encryptCipher.doFinal(Base64.getDecoder().decode(message));
        String eMsg = Base64.getEncoder().encodeToString(eBytes);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(eBytes);
        byte[] sBytes = signature.sign();
        String sMsg = Base64.getEncoder().encodeToString(sBytes);

        return eMsg + ":" + sMsg;
    }

    public static String decryptMsg(String message, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        String[] msg = message.split(":");

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(Base64.getDecoder().decode(msg[0]));
        if (!signature.verify(Base64.getDecoder().decode(msg[1])))
            throw new Exception("Invalid signature");

        Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] dBytes = decryptCipher.doFinal(Base64.getDecoder().decode(msg[0]));

        return Base64.getEncoder().encodeToString(dBytes);
    }
}
