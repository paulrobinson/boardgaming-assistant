package com.boardgaming.assistant.adapter.out.catalog;

import com.boardgaming.assistant.application.port.out.BarcodeResolutionPort;
import com.boardgaming.assistant.application.port.out.GameCatalogPort;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class InMemoryGameCatalogAdapter implements GameCatalogPort, BarcodeResolutionPort {

    private final Map<String, Game> gamesById = new LinkedHashMap<>();
    private final Map<String, Game> gamesByBarcode = new LinkedHashMap<>();

    private final ObjectMapper objectMapper;

    @Inject
    public InMemoryGameCatalogAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadSeedData() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("seed/games.json")) {
            if (is == null) {
                throw new IllegalStateException("Seed file seed/games.json not found on classpath");
            }
            List<JsonNode> nodes = objectMapper.readValue(is, new TypeReference<>() {});
            for (JsonNode node : nodes) {
                Game game = parseGame(node);
                gamesById.put(game.gameId(), game);
                gamesByBarcode.put(game.barcode(), game);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load seed data from seed/games.json", e);
        }
    }

    private Game parseGame(JsonNode node) {
        Map<Integer, Fit> playerCountSummary = new LinkedHashMap<>();
        JsonNode pcs = node.get("playerCountSummary");
        if (pcs != null) {
            pcs.fields().forEachRemaining(entry ->
                    playerCountSummary.put(
                            Integer.parseInt(entry.getKey()),
                            Fit.valueOf(entry.getValue().asText())));
        }

        return new Game(
                node.get("gameId").asText(),
                node.get("barcode").asText(),
                node.get("name").asText(),
                node.get("minPlayers").asInt(),
                node.get("maxPlayers").asInt(),
                node.get("officialPlayTimeMinutes").asInt(),
                node.get("officialMinAge").asInt(),
                node.get("complexityWeight").asDouble(),
                playerCountSummary,
                node.get("notes").asText());
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
