package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.ScanRequest;
import com.boardgaming.assistant.application.dto.ScanResponse;
import com.boardgaming.assistant.application.port.out.BarcodeResolutionPort;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveBarcodeUseCaseTest {

    private ResolveBarcodeUseCase useCase;
    private FakeBarcodeResolution fakeBarcodeResolution;

    @BeforeEach
    void setUp() {
        fakeBarcodeResolution = new FakeBarcodeResolution();
        useCase = new ResolveBarcodeUseCase(fakeBarcodeResolution);
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
    void returnsNullForUnknownBarcode() {
        ScanResponse result = useCase.execute(new ScanRequest("9999999999999"));

        assertNull(result);
    }

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
}
