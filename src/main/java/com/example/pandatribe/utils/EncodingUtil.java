package com.example.pandatribe.utils;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * Base58 (Bitcoin alphabet) encoding of UUIDs, used for short appraisal links.
 * Byte-for-byte compatible with bitcoinj's Base58, so existing links keep working.
 */
@Component
public class EncodingUtil {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);

    public String compressUUID(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return encode(buffer.array());
    }

    /**
     * @throws IllegalArgumentException if the value is not a Base58-encoded UUID
     */
    public UUID decompressUUID(String shortUuid) {
        byte[] bytes = decode(shortUuid);
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Not a valid short id: " + shortUuid);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    static String encode(byte[] input) {
        StringBuilder sb = new StringBuilder();
        BigInteger value = new BigInteger(1, input);
        while (value.signum() > 0) {
            BigInteger[] divRem = value.divideAndRemainder(BASE);
            sb.append(ALPHABET.charAt(divRem[1].intValue()));
            value = divRem[0];
        }
        for (int i = 0; i < input.length && input[i] == 0; i++) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }

    static byte[] decode(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Short id is missing");
        }
        BigInteger value = BigInteger.ZERO;
        for (char c : input.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base58 character in: " + input);
            }
            value = value.multiply(BASE).add(BigInteger.valueOf(digit));
        }
        byte[] magnitude = value.toByteArray();
        // toByteArray() may prepend a sign byte; strip it
        if (magnitude.length > 1 && magnitude[0] == 0) {
            magnitude = Arrays.copyOfRange(magnitude, 1, magnitude.length);
        }
        if (value.signum() == 0) {
            magnitude = new byte[0];
        }
        int leadingZeros = 0;
        while (leadingZeros < input.length() && input.charAt(leadingZeros) == ALPHABET.charAt(0)) {
            leadingZeros++;
        }
        byte[] out = new byte[leadingZeros + magnitude.length];
        System.arraycopy(magnitude, 0, out, leadingZeros, magnitude.length);
        return out;
    }
}
