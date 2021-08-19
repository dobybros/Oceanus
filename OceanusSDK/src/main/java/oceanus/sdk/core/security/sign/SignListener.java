package oceanus.sdk.core.security.sign;

import java.security.KeyPair;

public interface SignListener {
    KeyPair generateKeyPair();
    byte[] sign(byte[] data, byte[] privateKey);
    boolean verify(byte[] data, byte[] signature, byte[] publicKey);
}
