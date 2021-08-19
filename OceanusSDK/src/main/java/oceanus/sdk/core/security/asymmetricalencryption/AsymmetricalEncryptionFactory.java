package oceanus.sdk.core.security.asymmetricalencryption;

import oceanus.sdk.core.common.AbstractFactory;

public class AsymmetricalEncryptionFactory extends AbstractFactory<AsymmetricalEncryptionListener> {
    public AsymmetricalEncryptionListener getAsymmetricalEncryption(Class<? extends AsymmetricalEncryptionListener> asymmetricalEncryptionClass) {
        return get(asymmetricalEncryptionClass);
    }
}
