package com.example.pandatribe.utils;

import com.example.pandatribe.configs.EveDataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Base58 must stay byte-compatible with bitcoinj (which it replaced) or old appraisal links break.
 */
class EncodingUtilTest {

    private final EncodingUtil encodingUtil = new EncodingUtil();

    @Test
    void matchesStandardBase58Vectors() {
        assertThat(EncodingUtil.encode("Hello World".getBytes(StandardCharsets.UTF_8))).isEqualTo("JxF12TrwUP45BMd");
        byte[] address = HexFormat.of().parseHex("00eb15231dfceb60925886b67d065299925915aeb172c06647");
        assertThat(EncodingUtil.encode(address)).isEqualTo("1NS17iag9jJgTHD1VXjvLCEnZuQ3rJDE9L");
        assertThat(EncodingUtil.decode("1NS17iag9jJgTHD1VXjvLCEnZuQ3rJDE9L")).isEqualTo(address);
        assertThat(EncodingUtil.encode(new byte[]{0, 0, 0})).isEqualTo("111");
        assertThat(EncodingUtil.decode("111")).isEqualTo(new byte[]{0, 0, 0});
    }

    @Test
    void roundTripsUuidsIncludingLeadingZeroBytes() {
        for (UUID uuid : new UUID[]{UUID.randomUUID(), new UUID(0, 1), new UUID(0x00FFL, -1L)}) {
            assertThat(encodingUtil.decompressUUID(encodingUtil.compressUUID(uuid))).isEqualTo(uuid);
        }
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> encodingUtil.decompressUUID("not-base58!")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> encodingUtil.decompressUUID("abc")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marketKeyParsingFallsBackInsteadOfThrowing() {
        EveDataConfig config = new EveDataConfig();
        ReflectionTestUtils.setField(config, "defaultRegionId", 10000002);
        ReflectionTestUtils.setField(config, "defaultLocationId", 60003760L);

        assertThat(config.resolveRegionId("10000043_60008494")).isEqualTo(10000043);
        assertThat(config.resolveLocationId("10000043_60008494")).isEqualTo(60008494L);
        for (String bad : new String[]{null, "", "10000043", "a_b", "1_2_3", "99999999999_1"}) {
            assertThat(config.resolveRegionId(bad)).isEqualTo(10000002);
            assertThat(config.resolveLocationId(bad)).isEqualTo(60003760L);
        }
    }
}
