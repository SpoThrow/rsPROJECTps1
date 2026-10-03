package core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

/**
 * Pins the two password encodings that PlayerSave writes and accepts. Phase 6
 * replaces them with salted hashing and a lazy upgrade, which is only safe if
 * the legacy formats stay byte-identical in the meantime.
 */
class MiscTest {

	@Test
	void md5HashMatchesTheRfc1321Vectors() {
		// Canonical MD5 test vectors, so the digest is pinned against something
		// external rather than against the implementation under test.
		assertEquals("d41d8cd98f00b204e9800998ecf8427e", Misc.md5Hash(""));
		assertEquals("0cc175b9c0f1b6a831c399e269772661", Misc.md5Hash("a"));
		assertEquals("900150983cd24fb0d6963f7d28e17f72", Misc.md5Hash("abc"));
		assertEquals("f96b697d7cb7938d525a2f31aaf161d0", Misc.md5Hash("message digest"));
	}

	@Test
	void md5HashIsAlways32LowercaseHexDigits() {
		// The formatter is Integer.toHexString((b & 0xFF) | 0x100).substring(1,3),
		// which is what keeps leading-zero bytes at two digits.
		String hash = Misc.md5Hash("swordfish");
		assertEquals(32, hash.length());
		assertTrue(hash.matches("[0-9a-f]{32}"), "expected lowercase hex, got: " + hash);
		assertEquals(Misc.md5Hash("swordfish"), hash, "hashing must be deterministic");
		assertNotEquals(hash, Misc.md5Hash("swordfisg"));
	}

	@Test
	void md5HashDependsOnThePlatformDefaultCharset() throws NoSuchAlgorithmException {
		// getBytes() with no argument uses the JVM default charset. That was Cp1252
		// under the old Java 8 launch and is UTF-8 from Java 18 onward, so a password
		// with non-ASCII characters hashes differently across the migration. ASCII
		// passwords -- all the 317 client allows -- are unaffected.
		byte[] utf8 = "pässword".getBytes(StandardCharsets.UTF_8);
		StringBuilder expected = new StringBuilder();
		for (byte b : MessageDigest.getInstance("MD5").digest(utf8)) {
			expected.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
		}
		assertEquals(expected.toString(), Misc.md5Hash("pässword"));
	}

	@Test
	void basicEncryptConcatenatesDecimalCharCodes() {
		// Not encryption: a reversible, un delimited decimal dump. Accepted on load
		// as a legacy fallback alongside plaintext and md5Hash.
		assertEquals("979899", Misc.basicEncrypt("abc"));
		assertEquals("65", Misc.basicEncrypt("A"));
		assertEquals("", Misc.basicEncrypt(""));
		assertEquals("115119111114100102105115104", Misc.basicEncrypt("swordfish"));
	}
}
