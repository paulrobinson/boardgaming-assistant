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
        return barcodeResolution.resolveBarcode(request.barcode())
                .map(game -> new ScanResponse(
                        game.gameId(),
                        game.name(),
                        game.officialPlayTimeMinutes(),
                        game.minPlayers(),
                        game.maxPlayers(),
                        true))
                .orElseGet(() -> ScanResponse.unsupported(request.barcode()));
    }
}
