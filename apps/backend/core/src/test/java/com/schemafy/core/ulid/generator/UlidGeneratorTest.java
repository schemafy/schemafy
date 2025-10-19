package com.schemafy.core.ulid.generator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UlidGeneratorTest {

    @Test
    void testGenerateUlid() {
        String ulid = UlidGenerator.generate();

        assertNotNull(ulid);
        assertEquals(26, ulid.length());

        String validChars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
        for (char c : ulid.toCharArray()) {
            assertTrue(validChars.indexOf(c) >= 0,
                    "ULID contains invalid character: " + c);
        }
    }

    @RepeatedTest(10)
    void testGenerateUniqueUlids() {
        String ulid1 = UlidGenerator.generate();
        String ulid2 = UlidGenerator.generate();

        assertNotEquals(ulid1, ulid2, "Generated ULIDs should be unique");
    }

    @Test
    void testUlidSortability() {
        String ulid1 = UlidGenerator.generate();

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String ulid2 = UlidGenerator.generate();

        assertTrue(ulid1.compareTo(ulid2) < 0,
                "ULIDs should be sortable by generation time");
    }

    @Test
    void testExtractTimestamp() {
        String ulid = UlidGenerator.generate();
        long timestamp = UlidGenerator.extractTimestamp(ulid);

        assertTrue(timestamp > 0, "Timestamp should be positive");

        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - timestamp);
        assertTrue(timeDiff < 60000,
                "Timestamp should be close to current time");
    }

    @Test
    void testExtractTimestampWithInvalidUlid() {
        assertThrows(IllegalArgumentException.class, () -> {
            UlidGenerator.extractTimestamp("invalid");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            UlidGenerator.extractTimestamp(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            UlidGenerator.extractTimestamp("short");
        });
    }
}
