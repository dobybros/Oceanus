package core.security.asymmetricalencryption;

import core.common.AbstractFactory;

public class AsymmetricalEncryptionFactory extends AbstractFactory<AsymmetricalEncryptionListener> {
    public AsymmetricalEncryptionListener getAsymmetricalEncryption(Class<? extends AsymmetricalEncryptionListener> asymmetricalEncryptionClass) {
        return get(asymmetricalEncryptionClass);
    }
}
