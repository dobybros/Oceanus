package oceanus.sdk.core.security.asymmetricalencryption;

import oceanus.sdk.logger.LoggerEx;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAAsymmetricalEncryption implements AsymmetricalEncryptionListener {
    private static final String TAG = RSAAsymmetricalEncryption.class.getSimpleName();

    @Override
    public KeyPair generateKeyPair() {
        try {
            // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            // 初始化密钥对生成器，密钥大小为96-1024位
            keyPairGen.initialize(1024, new SecureRandom());
            // 生成一个密钥对，保存在keyPair中
            return keyPairGen.generateKeyPair();
        } catch(Throwable throwable) {
            LoggerEx.error(TAG, "generateKeyPair failed, " + throwable.getMessage());
            return null;
        }
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] publicKey) {
        if(data == null || publicKey == null) {
            StringBuilder builder = new StringBuilder();
            if(data == null)
                builder.append("data is null; ");
            if(publicKey == null)
                builder.append("publicKey is null.");
            LoggerEx.error(TAG, builder.toString());
            return null;
        }
        if(data.length > 117) {
            LoggerEx.error(TAG, "Data must not be longer than 117 bytes");
            return null;
        }
        try {
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
            //RSA加密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            return cipher.doFinal(data);
        } catch(Throwable throwable) {
            LoggerEx.error(TAG, "encrypt byteLen " + data.length + " failed, " + throwable.getMessage());
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] data, byte[] privateKey) {
        try {
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            return cipher.doFinal(data);
        } catch (Throwable throwable) {
            LoggerEx.error(TAG, "decrypt byteLen " + data.length + " failed, " + throwable.getMessage());
            return null;
        }
    }

    public static void main(String... args) throws UnsupportedEncodingException {
//        AsymmetricalEncryptionListener asymmetricalEncryptionListener = new RSAAsymmetricalEncryption();
        AsymmetricalEncryptionListener asymmetricalEncryptionListener = new AsymmetricalEncryptionFactory().getAsymmetricalEncryption(RSAAsymmetricalEncryption.class);
        KeyPair keyPair = asymmetricalEncryptionListener.generateKeyPair();
        System.out.println("Public key " + Base64.encodeBase64String(keyPair.getPublic().getEncoded()));
        System.out.println("Private key " + Base64.encodeBase64String(keyPair.getPrivate().getEncoded()));

        String content = "lalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaa";
        System.out.println("Encrypt content length " + content.length());
        byte[] encryptedData = asymmetricalEncryptionListener.encrypt(content.getBytes("utf8"), keyPair.getPublic().getEncoded());
        if(encryptedData == null)
            return;
        System.out.println("Encrypt content " + content + " length " + content.length() + " to length " + encryptedData.length);
        byte[] decryptedData = asymmetricalEncryptionListener.decrypt(encryptedData, keyPair.getPrivate().getEncoded());
        if(decryptedData == null)
            return;
        System.out.println("decryptedData " + new String(decryptedData));
    }

}
