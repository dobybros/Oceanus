package chat.encrypter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface EncryptService {
	void encrypt(InputStream is, OutputStream os) throws IOException;
	void decrypt(InputStream is, OutputStream os) throws IOException;
	byte[] decrypt(byte[] date) throws IOException;
	byte[] encrypt(byte[] data) throws IOException;
}
