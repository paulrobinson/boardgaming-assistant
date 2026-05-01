package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.ScanRequest;
import com.boardgaming.assistant.application.dto.ScanResponse;
import com.boardgaming.assistant.application.port.out.BarcodeResolutionPort;
import com.boardgaming.assistant.application.port.out.EventSinkPort;
import com.boardgaming.assistant.domain.model.Event;
import com.boardgaming.assistant.domain.model.EventType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ResolveBarcodeUseCase {

    private static final Logger LOG = Logger.getLogger(ResolveBarcodeUseCase.class.getName());

    private final BarcodeResolutionPort barcodeResolution;
    private final EventSinkPort eventSink;

    @Inject
    public ResolveBarcodeUseCase(BarcodeResolutionPort barcodeResolution,
                                 EventSinkPort eventSink) {
        this.barcodeResolution = barcodeResolution;
        this.eventSink = eventSink;
    }

    public ScanResponse execute(ScanRequest request) {
        return barcodeResolution.resolveBarcode(request.barcode())
                .map(game -> {
                    publishSafely(Event.of(EventType.SCAN_RESOLVED, Map.of(
                            "barcode", request.barcode(),
                            "gameId", game.gameId(),
                            "gameName", game.name())));
                    return new ScanResponse(
                            game.gameId(),
                            game.name(),
                            game.officialPlayTimeMinutes(),
                            game.minPlayers(),
                            game.maxPlayers(),
                            true);
                })
                .orElseGet(() -> {
                    publishSafely(Event.of(EventType.SCAN_NOT_SUPPORTED, Map.of(
                            "barcode", request.barcode())));
                    return ScanResponse.unsupported(request.barcode());
                });
    }

    private void publishSafely(Event event) {
        try {
            eventSink.publish(event);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to publish event: " + event.type(), e);
        }
    }
}
