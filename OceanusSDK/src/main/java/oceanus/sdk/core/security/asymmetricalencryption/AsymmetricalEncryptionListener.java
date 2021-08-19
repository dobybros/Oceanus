package oceanus.sdk.core.security.asymmetricalencryption;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface AsymmetricalEncryptionListener {
    KeyPair generateKeyPair();
    byte[] encrypt(byte[] data, byte[] publicKey);
    byte[] decrypt(byte[] data, byte[] privateKey);
}
