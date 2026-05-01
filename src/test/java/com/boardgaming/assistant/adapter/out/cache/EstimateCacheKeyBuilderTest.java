package com.boardgaming.assistant.adapter.out.cache;

import com.boardgaming.assistant.application.dto.GroupProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EstimateCacheKeyBuilderTest {

    private EstimateCacheKeyBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new EstimateCacheKeyBuilder();
    }

    @Test
    void producesStableKey() {
        var profile = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);

        String key1 = builder.build("catan", profile);
        String key2 = builder.build("catan", profile);

        assertEquals(key1, key2);
    }

    @Test
    void keyContainsAllProfileFields() {
        var profile = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);

        String key = builder.build("catan", profile);

        assertEquals("catan:4:mixed:average:moderate:false", key);
    }

    @Test
    void normalizesLeadingAndTrailingWhitespace() {
        var clean = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);
        var padded = new GroupProfileDto(4, "  mixed  ", " average ", "  moderate  ", false, null);

        assertEquals(builder.build("catan", clean), builder.build("catan", padded));
    }

    @Test
    void normalizesMixedCase() {
        var lower = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);
        var upper = new GroupProfileDto(4, "MIXED", "Average", "MODERATE", false, null);

        assertEquals(builder.build("catan", lower), builder.build("catan", upper));
    }

    @Test
    void normalizesGameIdCase() {
        var profile = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);

        assertEquals(builder.build("catan", profile), builder.build("CATAN", profile));
    }

    @Test
    void normalizesGameIdWhitespace() {
        var profile = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);

        assertEquals(builder.build("catan", profile), builder.build("  catan  ", profile));
    }

    @Test
    void differentGameIdsProduceDifferentKeys() {
        var profile = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);

        assertNotEquals(builder.build("catan", profile), builder.build("pandemic", profile));
    }

    @Test
    void differentPlayerCountsProduceDifferentKeys() {
        var three = new GroupProfileDto(3, "mixed", "average", "moderate", false, null);
        var four = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);

        assertNotEquals(builder.build("catan", three), builder.build("catan", four));
    }

    @Test
    void differentFamiliarityProducesDifferentKeys() {
        var mixed = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);
        var newb = new GroupProfileDto(4, "new", "average", "moderate", false, null);

        assertNotEquals(builder.build("catan", mixed), builder.build("catan", newb));
    }

    @Test
    void childrenIncludedProducesDifferentKey() {
        var without = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);
        var with = new GroupProfileDto(4, "mixed", "average", "moderate", true, null);

        assertNotEquals(builder.build("catan", without), builder.build("catan", with));
    }

    @Test
    void notesAreNotPartOfKey() {
        var noNotes = new GroupProfileDto(4, "mixed", "average", "moderate", false, null);
        var withNotes = new GroupProfileDto(4, "mixed", "average", "moderate", false, "some notes");

        assertEquals(builder.build("catan", noNotes), builder.build("catan", withNotes));
    }

    @Test
    void handlesNullStringsGracefully() {
        var profile = new GroupProfileDto(4, null, null, null, false, null);

        String key = builder.build(null, profile);

        assertEquals(":4::::false", key);
    }
}
