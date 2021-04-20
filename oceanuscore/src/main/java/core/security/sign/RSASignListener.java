package core.security.sign;

import org.apache.commons.lang.exception.ExceptionUtils;
import core.log.LoggerHelper;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSASignListener implements SignListener {
    @Override
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            return kpg.genKeyPair();
        } catch (Throwable throwable) {
            LoggerHelper.logger.error("generateKeyPair failed, " + ExceptionUtils.getFullStackTrace(throwable));
        }
        return null;
    }

    @Override
    public byte[] sign(byte[] data, byte[] privateKey) {
        if(data == null || privateKey == null) {
            StringBuilder builder = new StringBuilder();
            if(data == null)
                builder.append("data is null; ");
            if(privateKey == null)
                builder.append("privateKey is null.");
            LoggerHelper.logger.error(builder.toString());
            return null;
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
            signature.initSign(rsaPrivateKey);
            signature.update(data);
            return signature.sign();
        } catch(Throwable throwable) {
            LoggerHelper.logger.error("sign dataLen " + data.length + " failed, " + ExceptionUtils.getFullStackTrace(throwable));
        }
        return null;
    }

    @Override
    public boolean verify(byte[] data, byte[] signature, byte[] publicKey) {
        if(data == null || publicKey == null || signature == null) {
            StringBuilder builder = new StringBuilder();
            if(data == null)
                builder.append("data is null; ");
            if(signature == null)
                builder.append("signature is null; ");
            if(publicKey == null)
                builder.append("privateKey is null.");
            LoggerHelper.logger.error(builder.toString());
            return false;
        }
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
            sig.initVerify(pubKey);
            sig.update(data);
            return sig.verify(signature);
        } catch(Throwable throwable) {
            LoggerHelper.logger.error("verify dataLen " + data.length + " failed, " + ExceptionUtils.getFullStackTrace(throwable));
        }
        return false;
    }
}