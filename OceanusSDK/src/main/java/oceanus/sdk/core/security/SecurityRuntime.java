package oceanus.sdk.core.security;

import oceanus.sdk.core.common.CoreRuntime;
import oceanus.sdk.core.security.asymmetricalencryption.AsymmetricalEncryptionFactory;
import oceanus.sdk.core.security.asymmetricalencryption.AsymmetricalEncryptionListener;
import oceanus.sdk.core.security.asymmetricalencryption.RSAAsymmetricalEncryption;
import oceanus.sdk.core.security.sign.RSASignListener;
import oceanus.sdk.core.security.sign.SignFactory;
import oceanus.sdk.core.security.sign.SignListener;
import oceanus.sdk.logger.LoggerEx;

public class SecurityRuntime extends CoreRuntime {
    private static final String TAG = SecurityRuntime.class.getSimpleName();
    private static AsymmetricalEncryptionFactory asymmetricalEncryptionFactory = new AsymmetricalEncryptionFactory();
    private static SignFactory signFactory = new SignFactory();

    private static Class<? extends AsymmetricalEncryptionListener> asymmetricalEncryptionClass;
    private static Class<? extends SignListener> signListenerClass;

    public static byte[] generateHash(String data) {
        return HashUtils.hash(data);
    }
    public static byte[] generateHash(byte[] data) {
        return HashUtils.hash(data);
    }

    public static AsymmetricalEncryptionListener getAsymmetricalEncryption() {
        if(asymmetricalEncryptionClass == null) {
            String asymmetricalEncryptionStr = System.getProperty("starfish.security.asymmetrical.encryption.class");
            if(asymmetricalEncryptionStr != null) {
                try {
                    Class<? extends AsymmetricalEncryptionListener> clazz = (Class<? extends AsymmetricalEncryptionListener>) Class.forName(asymmetricalEncryptionStr);
                    asymmetricalEncryptionClass = clazz;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    LoggerEx.error(TAG, "Class not found while read from system property \"starfish.security.asymmetrical.encryption.class\", " + asymmetricalEncryptionStr);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Unknown error occurred while read from system property \"starfish.security.asymmetrical.encryption.class\", " + asymmetricalEncryptionStr + " error " + t.getMessage());
                }
            }
            if(asymmetricalEncryptionClass == null)
                asymmetricalEncryptionClass = RSAAsymmetricalEncryption.class;
            LoggerEx.info(TAG, "asymmetricalEncryptionClass is " + asymmetricalEncryptionClass);
        }
        return asymmetricalEncryptionFactory.getAsymmetricalEncryption(asymmetricalEncryptionClass);
    }

    public static SignListener getSignListener() {
        if(signListenerClass == null) {
            String signListenerStr = System.getProperty("starfish.security.sign.class");
            if(signListenerStr != null) {
                try {
                    Class<? extends SignListener> clazz = (Class<? extends SignListener>) Class.forName(signListenerStr);
                    signListenerClass = clazz;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    LoggerEx.error(TAG, "Class not found while read from system property \"starfish.security.sign.class\", " + signListenerStr);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Unknown error occurred while read from system property \"starfish.sign.encryption.class\", " + signListenerStr + " error " + t.getMessage());
                }
            }
            if(signListenerClass == null)
                signListenerClass = RSASignListener.class;
            LoggerEx.info(TAG, "signListenerClass is " + signListenerClass);
        }
        return signFactory.getSignListener(signListenerClass);
    }
}
