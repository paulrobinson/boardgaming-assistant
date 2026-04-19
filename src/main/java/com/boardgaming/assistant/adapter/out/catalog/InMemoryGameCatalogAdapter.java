package com.boardgaming.assistant.adapter.out.catalog;

import com.boardgaming.assistant.application.port.out.BarcodeResolutionPort;
import com.boardgaming.assistant.application.port.out.GameCatalogPort;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class InMemoryGameCatalogAdapter implements GameCatalogPort, BarcodeResolutionPort {

    private final Map<String, Game> gamesById = new LinkedHashMap<>();
    private final Map<String, Game> gamesByBarcode = new LinkedHashMap<>();

    public InMemoryGameCatalogAdapter() {
        seed(new Game("catan", "0029877030712", "Catan", 3, 4, 60, 10, 2.3,
                Map.of(3, Fit.GOOD, 4, Fit.BEST),
                "Trading and building on the island of Catan"));

        seed(new Game("ticket-to-ride", "0824968717912", "Ticket to Ride", 2, 5, 45, 8, 1.8,
                Map.of(2, Fit.OK, 3, Fit.GOOD, 4, Fit.BEST, 5, Fit.GOOD),
                "Cross-country train adventure"));

        seed(new Game("pandemic", "0681706711003", "Pandemic", 2, 4, 45, 8, 2.4,
                Map.of(2, Fit.GOOD, 3, Fit.GOOD, 4, Fit.BEST),
                "Cooperative disease-fighting game"));

        seed(new Game("wingspan", "0850000576100", "Wingspan", 1, 5, 55, 10, 2.4,
                Map.of(1, Fit.OK, 2, Fit.GOOD, 3, Fit.BEST, 4, Fit.GOOD, 5, Fit.OK),
                "Competitive bird-collection engine builder"));

        seed(new Game("azul", "0826956600107", "Azul", 2, 4, 35, 8, 1.8,
                Map.of(2, Fit.BEST, 3, Fit.GOOD, 4, Fit.GOOD),
                "Abstract tile-drafting and pattern-building"));
    }

    private void seed(Game game) {
        gamesById.put(game.gameId(), game);
        gamesByBarcode.put(game.barcode(), game);
    }

    @Override
    public Optional<Game> findByGameId(String gameId) {
        return Optional.ofNullable(gamesById.get(gameId));
    }

    @Override
    public Optional<Game> resolveBarcode(String barcode) {
        return Optional.ofNullable(gamesByBarcode.get(barcode));
    }
}
