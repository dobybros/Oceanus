package core.security;

import chat.logs.LoggerEx;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
	private static final String TAG = HashUtils.class.getSimpleName();
	// Name of the hash algorithm for the block hash / mining
	public static String HASH_ALOGITHM = "SHA-256";

	public static byte[] hash(byte[] input) {
		if(input == null) {
			LoggerEx.error(TAG, "input is null");
			return null;
		}
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALOGITHM);
			md.update(input);
			return md.digest();
		} catch (Throwable throwable) {
			LoggerEx.error(TAG, "generate hash for inputLen " + input.length + " failed, " + ExceptionUtils.getFullStackTrace(throwable));
		}
		return null;
	}
	
	public static byte[] hash(String input) {
		return hash(input.getBytes());
	}
}
