package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.Game;

import java.util.Optional;

public interface GameCatalogPort {
    Optional<Game> findByGameId(String gameId);
}
