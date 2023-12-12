import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;
import java.util.Base64;

public class MsgUtil {
    public static String encryptMsgRSA(String message, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] eBytes = cipher.doFinal(Base64.getEncoder().encode(message.getBytes()));
        return Base64.getEncoder().encodeToString(eBytes);
    }

    public static String decryptMsgRSA(String message, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] dBytes = cipher.doFinal(Base64.getDecoder().decode(message));
        return new String(Base64.getDecoder().decode(dBytes));
    }

    public static String encryptMsgAES(String message, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] eBytes = cipher.doFinal(Base64.getEncoder().encode(message.getBytes()));
        return Base64.getEncoder().encodeToString(eBytes);
    }

    public static String decryptMsgAES(String message, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] dBytes = cipher.doFinal(Base64.getDecoder().decode(message));
        return new String(Base64.getDecoder().decode(dBytes));
    }

    public static String encryptAndSignMsg(String message, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] eBytes = cipher.doFinal(Base64.getEncoder().encode(message.getBytes()));
        String eMsg = Base64.getEncoder().encodeToString(eBytes);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(eBytes);
        byte[] sBytes = signature.sign();
        String sMsg = Base64.getEncoder().encodeToString(sBytes);

        return eMsg + ":" + sMsg;
    }

    public static String decryptAndVerifyMsg(String message, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        String[] msg = message.split(":");

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(Base64.getDecoder().decode(msg[0]));
        if (!signature.verify(Base64.getDecoder().decode(msg[1])))
            throw new Exception("Invalid signature");

        return decryptMsgRSA(msg[0], privateKey);
    }
}
