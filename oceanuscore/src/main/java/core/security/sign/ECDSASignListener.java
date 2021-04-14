package core.security.sign;

import org.apache.commons.lang.exception.ExceptionUtils;
import core.log.LoggerHelper;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class ECDSASignListener implements SignListener {
    @Override
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(256); //key长度设置
            return keyPairGenerator.generateKeyPair();
        } catch (Throwable throwable) {
            LoggerHelper.logger.error("generateKeyPair failed, " + ExceptionUtils.getFullStackTrace(throwable));
        }
        return  null;
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
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey thePrivateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            Signature signature = Signature.getInstance("SHA1withECDSA");
            signature.initSign(thePrivateKey);
            signature.update(data);
            return signature.sign();
        } catch (Throwable throwable) {
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
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PublicKey thePublicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            Signature theSignature = Signature.getInstance("SHA1withECDSA");
            theSignature.initVerify(thePublicKey);
            theSignature.update(data);
            return theSignature.verify(signature);
        } catch (Throwable throwable) {
            LoggerHelper.logger.error("verify dataLen " + data.length + " failed, " + ExceptionUtils.getFullStackTrace(throwable));
        }
        return false;
    }

}
