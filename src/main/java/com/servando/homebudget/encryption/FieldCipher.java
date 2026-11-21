package com.servando.homebudget.encryption;

import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.codecs.DocumentCodec;
import org.bson.types.Binary;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Encrypts single field values for MongoDB, and makes the keyed name hash that unique names are
 * checked against. Plain Java and the Mongo driver, no Spring, so the inspect tool can use it too.
 *
 * <p>One 32-byte master key ({@code BUDGET_ENCRYPTION_KEY}, base64) gives two subkeys: one for
 * AES-256-GCM and one for HMAC-SHA256 name hashes. A stored value is BSON binary subtype
 * {@value #SUBTYPE}: a version byte, a random 12-byte IV, then the ciphertext and tag. The plaintext
 * is the value as a one-field BSON document, so a Double comes back a Double and an Integer an
 * Integer. Each value is bound to its collection, record id and field, so one can't be copied onto
 * another record or field and still decrypt.
 *
 * <p>Losing the master key loses the data: nothing can decrypt it without the key.
 */
public final class FieldCipher {

    /** BSON's user-defined binary subtypes start at 0x80; this one marks our ciphertext. */
    public static final byte SUBTYPE = (byte) 0x80;
    private static final byte VERSION = 1;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final DocumentCodec CODEC = new DocumentCodec();

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec nameIndexKey;
    private final SecureRandom random = new SecureRandom();

    public FieldCipher(byte[] masterKey) {
        if (masterKey == null || masterKey.length != 32) {
            throw new IllegalArgumentException("The encryption key must be 32 bytes.");
        }
        this.encryptionKey = new SecretKeySpec(hmac(masterKey, "home-budget field encryption v1"), "AES");
        this.nameIndexKey = new SecretKeySpec(hmac(masterKey, "home-budget name index v1"), "HmacSHA256");
    }

    /** From base64 (generate one with {@code openssl rand -base64 32}). */
    public static FieldCipher fromBase64(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set BUDGET_ENCRYPTION_KEY to 32 random bytes in base64 (openssl rand -base64 32).");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("BUDGET_ENCRYPTION_KEY isn't valid base64.", e);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("BUDGET_ENCRYPTION_KEY must decode to 32 bytes, not " + bytes.length + ".");
        }
        return new FieldCipher(bytes);
    }

    public static boolean isEncrypted(Object value) {
        return value instanceof Binary b && b.getType() == SUBTYPE;
    }

    public Binary encrypt(Object value, String collection, Object id, String field) {
        byte[] plaintext = toBson(value);
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(collection, id, field));
            byte[] sealed = cipher.doFinal(plaintext);
            return new Binary(SUBTYPE, ByteBuffer.allocate(1 + IV_BYTES + sealed.length)
                    .put(VERSION).put(iv).put(sealed).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Couldn't encrypt " + collection + "." + field, e);
        }
    }

    public Object decrypt(Binary stored, String collection, Object id, String field) {
        byte[] data = stored.getData();
        if (data.length < 1 + IV_BYTES || data[0] != VERSION) {
            throw new IllegalStateException("Unknown encrypted format in " + collection + " " + id + "." + field);
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, data, 1, IV_BYTES));
            cipher.updateAAD(aad(collection, id, field));
            byte[] plaintext = cipher.doFinal(data, 1 + IV_BYTES, data.length - 1 - IV_BYTES);
            return new RawBsonDocument(plaintext).decode(CODEC).get("v");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Couldn't decrypt " + collection + " " + id + "." + field
                    + ": wrong key, or the value was changed or moved.", e);
        }
    }

    /** Decrypts every encrypted top-level field of a stored document in place. */
    public Document decryptDocument(Document document, String collection) {
        Object id = document.get("_id");
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (isEncrypted(entry.getValue())) {
                entry.setValue(decrypt((Binary) entry.getValue(), collection, id, entry.getKey()));
            }
        }
        return document;
    }

    /**
     * A keyed hash of a household's record name: equal names in one household hash the same, so
     * a unique index can hold, but without the key it says nothing about the name. The household
     * is part of it, so the same name in two households doesn't match either.
     */
    public byte[] nameIndex(String householdId, String name) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(nameIndexKey);
            return mac.doFinal((householdId + "\0" + name).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] aad(String collection, Object id, String field) {
        if (id == null) {
            throw new IllegalStateException("A record needs its id before " + collection + "." + field + " is encrypted.");
        }
        return (collection + "\0" + id + "\0" + field).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] toBson(Object value) {
        RawBsonDocument raw = new RawBsonDocument(new Document("v", value), CODEC);
        ByteBuffer buffer = raw.getByteBuffer().asNIO();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private static byte[] hmac(byte[] key, String label) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(label.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
