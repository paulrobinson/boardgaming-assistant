package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.Game;

import java.util.Optional;

public interface BarcodeResolutionPort {
    Optional<Game> resolveBarcode(String barcode);
}
