package oceanus.sdk.core.security.sign;


import oceanus.sdk.core.common.AbstractFactory;
import oceanus.sdk.core.security.HashUtils;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;

public class SignFactory extends AbstractFactory<SignListener> {
    public SignListener getSignListener(Class<? extends SignListener> clazz) {
        return get(clazz);
    }

    public static void main(String... args) throws UnsupportedEncodingException {
//        SignListener listener = new SignFactory().getSignListener(ECDSASignListener.class);
        SignListener listener = new SignFactory().getSignListener(RSASignListener.class);
        KeyPair keyPair = listener.generateKeyPair();
        System.out.println("Public key " + Base64.encodeBase64String(keyPair.getPublic().getEncoded()));
        System.out.println("Private key " + Base64.encodeBase64String(keyPair.getPrivate().getEncoded()));

        String content = "lalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalaldddddlaalalalaalallaalalalaalallaalalalaalallaalalalaalallaalalalaalallaa";
        System.out.println("Encrypt content length " + content.length());
        long time = System.currentTimeMillis();
        byte[] signedData = listener.sign(content.getBytes("utf8"), keyPair.getPrivate().getEncoded());
        System.out.println("sign take " + (System.currentTimeMillis() - time));
        if(signedData == null)
            return;
        System.out.println("Encrypt content " + content + " length " + content.length() + " to length " + signedData.length + " signature " + Base64.encodeBase64String(signedData));
        boolean verified = true;
        time = System.currentTimeMillis();
        for(int i = 0; i < 1000; i++) {
            listener.verify(content.getBytes("utf8"), signedData, keyPair.getPublic().getEncoded());
        }
        System.out.println("verify take " + (System.currentTimeMillis() - time));
        System.out.println("verified " + verified);

        time = System.currentTimeMillis();
        byte[] hash = HashUtils.hash(content);
        System.out.println("hash takes " + (System.currentTimeMillis() - time));
        System.out.println("hash " + Base64.encodeBase64String(hash));

        time = System.currentTimeMillis();
        byte[] signature = listener.sign(hash, keyPair.getPrivate().getEncoded());
        System.out.println("sign hash take " + (System.currentTimeMillis() - time) + " signature " + Base64.encodeBase64String(signature));

        time = System.currentTimeMillis();
        verified = listener.verify(hash, signature, keyPair.getPublic().getEncoded());
        System.out.println("verify hash take " + (System.currentTimeMillis() - time));
    }
}
