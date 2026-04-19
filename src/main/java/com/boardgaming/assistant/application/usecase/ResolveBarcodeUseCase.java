package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.ScanRequest;
import com.boardgaming.assistant.application.dto.ScanResponse;
import com.boardgaming.assistant.application.port.out.BarcodeResolutionPort;
import com.boardgaming.assistant.domain.model.Game;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResolveBarcodeUseCase {

    private final BarcodeResolutionPort barcodeResolution;

    @Inject
    public ResolveBarcodeUseCase(BarcodeResolutionPort barcodeResolution) {
        this.barcodeResolution = barcodeResolution;
    }

    public ScanResponse execute(ScanRequest request) {
        Game game = barcodeResolution.resolveBarcode(request.barcode())
                .orElse(null);

        if (game == null) {
            return null;
        }

        return new ScanResponse(
                game.gameId(),
                game.name(),
                game.officialPlayTimeMinutes(),
                game.minPlayers(),
                game.maxPlayers(),
                true);
    }
}
