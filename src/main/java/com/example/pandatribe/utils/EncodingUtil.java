package com.example.pandatribe.utils;

import org.bitcoinj.core.Base58;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Utility class for encoding and compressing data.
 * Provides Base58 encoding for integers and UUIDs to create shorter URL-safe strings.
 */
@Component
public class EncodingUtil {

    /**
     * Compresses an Integer into a Base58 string.
     * Useful for creating shorter, URL-safe IDs.
     *
     * @param number The integer to compress
     * @return Base58 encoded string
     */
    public String compressInteger(Integer number) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4); // Integer = 4 bytes
        byteBuffer.putInt(number);
        return Base58.encode(byteBuffer.array());
    }

    /**
     * Decompresses a Base58 string back into an Integer.
     *
     * @param shortNumber The Base58 encoded string
     * @return The original integer
     */
    public Integer decompressInteger(String shortNumber) {
        byte[] bytes = Base58.decode(shortNumber);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getInt();
    }

    /**
     * Compresses a UUID into a Base58 string.
     * Converts a 36-character UUID into a shorter URL-safe format.
     *
     * @param uuid The UUID to compress
     * @return Base58 encoded string (typically 22 characters)
     */
    public String compressUUID(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base58.encode(byteBuffer.array());
    }

    /**
     * Decompresses a Base58 string back into a UUID.
     *
     * @param shortUuid The Base58 encoded UUID string
     * @return The original UUID
     */
    public UUID decompressUUID(String shortUuid) {
        byte[] bytes = Base58.decode(shortUuid);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long mostSigBits = byteBuffer.getLong();
        long leastSigBits = byteBuffer.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Generates a short ID from a UUID.
     * Convenience method that combines UUID generation and compression.
     *
     * @return A new compressed UUID string
     */
    public String generateShortId() {
        return compressUUID(UUID.randomUUID());
    }

    /**
     * Validates if a string is a valid Base58 encoded value.
     *
     * @param encoded The string to validate
     * @return true if valid Base58, false otherwise
     */
    public boolean isValidBase58(String encoded) {
        try {
            Base58.decode(encoded);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
