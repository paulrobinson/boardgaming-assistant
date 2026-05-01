package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.ScanRequest;
import com.boardgaming.assistant.application.dto.ScanResponse;
import com.boardgaming.assistant.application.port.out.BarcodeResolutionPort;
import com.boardgaming.assistant.application.port.out.EventSinkPort;
import com.boardgaming.assistant.domain.model.Event;
import com.boardgaming.assistant.domain.model.EventType;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveBarcodeUseCaseTest {

    private ResolveBarcodeUseCase useCase;
    private FakeBarcodeResolution fakeBarcodeResolution;
    private FakeEventSink fakeEventSink;

    @BeforeEach
    void setUp() {
        fakeBarcodeResolution = new FakeBarcodeResolution();
        fakeEventSink = new FakeEventSink();
        useCase = new ResolveBarcodeUseCase(fakeBarcodeResolution, fakeEventSink);
    }

    @Test
    void resolvesKnownBarcode() {
        ScanResponse result = useCase.execute(new ScanRequest("0029877030712"));

        assertNotNull(result);
        assertEquals("catan", result.gameId());
        assertEquals("Catan", result.name());
        assertEquals(60, result.officialPlayTimeMinutes());
        assertEquals(3, result.minPlayers());
        assertEquals(4, result.maxPlayers());
        assertTrue(result.supported());
    }

    @Test
    void returnsUnsupportedForUnknownBarcode() {
        ScanResponse result = useCase.execute(new ScanRequest("9999999999999"));

        assertNotNull(result);
        assertFalse(result.supported());
        assertNull(result.gameId());
        assertNull(result.name());
    }

    @Test
    void emitsScanResolvedEvent() {
        useCase.execute(new ScanRequest("0029877030712"));

        assertEquals(1, fakeEventSink.events.size());
        Event event = fakeEventSink.events.get(0);
        assertEquals(EventType.SCAN_RESOLVED, event.type());
        assertEquals("0029877030712", event.payload().get("barcode"));
        assertEquals("catan", event.payload().get("gameId"));
        assertEquals("Catan", event.payload().get("gameName"));
        assertNotNull(event.timestamp());
    }

    @Test
    void emitsScanNotSupportedEvent() {
        useCase.execute(new ScanRequest("9999999999999"));

        assertEquals(1, fakeEventSink.events.size());
        Event event = fakeEventSink.events.get(0);
        assertEquals(EventType.SCAN_NOT_SUPPORTED, event.type());
        assertEquals("9999999999999", event.payload().get("barcode"));
    }

    @Test
    void eventPublishingFailureDoesNotBreakScanFlow() {
        var failingSink = new FailingEventSink();
        var useCaseWithFailingSink = new ResolveBarcodeUseCase(fakeBarcodeResolution, failingSink);

        ScanResponse result = useCaseWithFailingSink.execute(new ScanRequest("0029877030712"));

        assertNotNull(result);
        assertTrue(result.supported());
        assertEquals("catan", result.gameId());
    }

    // --- Test doubles ---

    static class FakeBarcodeResolution implements BarcodeResolutionPort {
        private static final Game CATAN = new Game(
                "catan", "0029877030712", "Catan", 3, 4, 60, 10, 2.3,
                Map.of(3, Fit.GOOD, 4, Fit.BEST), "notes");

        @Override
        public Optional<Game> resolveBarcode(String barcode) {
            if ("0029877030712".equals(barcode)) {
                return Optional.of(CATAN);
            }
            return Optional.empty();
        }
    }

    static class FakeEventSink implements EventSinkPort {
        final List<Event> events = new ArrayList<>();

        @Override
        public void publish(Event event) {
            events.add(event);
        }
    }

    static class FailingEventSink implements EventSinkPort {
        @Override
        public void publish(Event event) {
            throw new RuntimeException("Event sink unavailable");
        }
    }
}
