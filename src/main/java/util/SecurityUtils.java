package util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Please note that this class is not needed for Lab 1, but can later be
 * used in Lab 2.
 * 
 * Provides security provider related utility methods.
 */
public final class SecurityUtils {
	public enum Encryption {RAW, RSA, AES, HMAC};

	private SecurityUtils() {
	}

	/**
	 * Registers the {@link BouncyCastleProvider} as the primary security
	 * provider if necessary.
	 */
	public static synchronized void registerBouncyCastle() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.insertProviderAt(new BouncyCastleProvider(), 0);
		}
	}
	
	/**
	 * @brief Base64-encodes data
	 * @param data
	 * @return encoded data
	 */
	public static byte[] encodeB64(byte[] data) {
		try {
			return Base64.encode(data);
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @brief Base64-deccodes data
	 * @param data
	 * @return decoded data
	 */
	public static byte[] decodeB64(byte[] data) {
		try {
			return Base64.decode(data);
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	/**
	 * @brief RSA-encrypts data using a public key
	 * @param data
	 * @param publicKey
	 * @return encrypted data
	 */
	public static byte[] encryptRSA(byte[] data, PublicKey publicKey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @brief RSA-decrypts data using the private key
	 * @param data
	 * @param privateKey
	 * @return decrypted data
	 */
	public static byte[] decryptRSA(byte[] data, PrivateKey privateKey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	/**
	 * @brief AES-encrypts data using a secret key
	 * @param data
	 * @param secretKey
	 * @return encrypted data
	 */
	public static byte[] encryptAES(byte[] data, SecretKey secretKey, IvParameterSpec initVector) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, initVector);
			return cipher.doFinal(data);
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @brief AES-decrypts data using a secret key
	 * @param data
	 * @param secretKey
	 * @return decrypted data
	 */
	public static byte[] decryptAES(byte[] data, SecretKey secretKey, IvParameterSpec initVector) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, initVector);
			return cipher.doFinal(data);
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	/**
	 * @brief generates a secure random number
	 * @param count size of number (in bytes)
	 * @return secure random number
	 */
	public static byte[] randomNumber(int count) {
		SecureRandom secureRandom = new SecureRandom();
		final byte[] bytes = new byte[count]; 
		secureRandom.nextBytes(bytes);
		return bytes;
	}
	
	/**
	 * @param left
	 * @param right
	 * @return
	 */
	public static byte[] concat(byte[] left, byte[] right) {
		byte[] result = new byte[left.length + right.length];
		// Kopiervorgang
		System.arraycopy(left, 0, result, 0, left.length);
		System.arraycopy(right, 0, result, left.length, right.length);
		return result;
	}

	/**
	 * @param source
	 * @param start
	 * @param length
	 * @return
	 */
	public static byte[] subarray(byte[] source, int start, int length) {
		if (start > source.length) {
			return null;
		}
		if (start + length > source.length) {
			length = source.length - start;
		}
		byte[] result = new byte[length];
		// Kopiervorgang
		System.arraycopy(source, start, result, 0, length);
		return result;
	}
}
