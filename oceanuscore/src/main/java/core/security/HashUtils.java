package core.security;

import org.apache.commons.lang.exception.ExceptionUtils;
import core.log.LoggerHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
	// Name of the hash algorithm for the block hash / mining
	public static String HASH_ALOGITHM = "SHA-256";

	public static byte[] hash(byte[] input) {
		if(input == null) {
			LoggerHelper.logger.error("input is null");
			return null;
		}
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALOGITHM);
			md.update(input);
			return md.digest();
		} catch (Throwable throwable) {
			LoggerHelper.logger.error("generate hash for inputLen " + input.length + " failed, " + ExceptionUtils.getFullStackTrace(throwable));
		}
		return null;
	}
	
	public static byte[] hash(String input) {
		return hash(input.getBytes());
	}
}
