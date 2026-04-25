package com.boardgaming.assistant.adapter.out.catalog;

import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryGameCatalogAdapterTest {

    private InMemoryGameCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryGameCatalogAdapter(new ObjectMapper());
        adapter.loadSeedData();
    }

    @Test
    void loadsAllSeededGames() {
        assertTrue(adapter.findByGameId("catan").isPresent());
        assertTrue(adapter.findByGameId("ticket-to-ride").isPresent());
        assertTrue(adapter.findByGameId("pandemic").isPresent());
        assertTrue(adapter.findByGameId("wingspan").isPresent());
        assertTrue(adapter.findByGameId("azul").isPresent());
    }

    @Test
    void resolvesBarcodeToCorrectGame() {
        Optional<Game> game = adapter.resolveBarcode("0029877030712");

        assertTrue(game.isPresent());
        assertEquals("catan", game.get().gameId());
        assertEquals("Catan", game.get().name());
    }

    @Test
    void returnsEmptyForUnknownBarcode() {
        Optional<Game> game = adapter.resolveBarcode("0000000000000");

        assertFalse(game.isPresent());
    }

    @Test
    void returnsEmptyForUnknownGameId() {
        Optional<Game> game = adapter.findByGameId("nonexistent");

        assertFalse(game.isPresent());
    }

    @Test
    void seededGameHasExpectedFields() {
        Game catan = adapter.findByGameId("catan").orElseThrow();

        assertEquals("0029877030712", catan.barcode());
        assertEquals("Catan", catan.name());
        assertEquals(3, catan.minPlayers());
        assertEquals(4, catan.maxPlayers());
        assertEquals(60, catan.officialPlayTimeMinutes());
        assertEquals(10, catan.officialMinAge());
        assertEquals(2.3, catan.complexityWeight(), 0.01);
        assertNotNull(catan.playerCountSummary());
        assertEquals(Fit.GOOD, catan.playerCountSummary().get(3));
        assertEquals(Fit.BEST, catan.playerCountSummary().get(4));
        assertEquals("Trading and building on the island of Catan", catan.notes());
    }

    @Test
    void allBarcodesResolveToCorrectGames() {
        assertEquals("catan", adapter.resolveBarcode("0029877030712").orElseThrow().gameId());
        assertEquals("ticket-to-ride", adapter.resolveBarcode("0824968717912").orElseThrow().gameId());
        assertEquals("pandemic", adapter.resolveBarcode("0681706711003").orElseThrow().gameId());
        assertEquals("wingspan", adapter.resolveBarcode("0850000576100").orElseThrow().gameId());
        assertEquals("azul", adapter.resolveBarcode("0826956600107").orElseThrow().gameId());
    }
}
