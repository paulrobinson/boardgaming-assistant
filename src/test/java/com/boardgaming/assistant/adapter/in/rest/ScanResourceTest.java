package com.boardgaming.assistant.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class ScanResourceTest {

    @Test
    void resolvesKnownBarcode() {
        given()
                .contentType("application/json")
                .body("{\"barcode\": \"0029877030712\"}")
        .when()
                .post("/api/v1/scan/resolve")
        .then()
                .statusCode(200)
                .body("supported", is(true))
                .body("gameId", equalTo("catan"))
                .body("name", equalTo("Catan"))
                .body("officialPlayTimeMinutes", equalTo(60))
                .body("minPlayers", equalTo(3))
                .body("maxPlayers", equalTo(4));
    }

    @Test
    void resolvesTicketToRide() {
        given()
                .contentType("application/json")
                .body("{\"barcode\": \"0824968717912\"}")
        .when()
                .post("/api/v1/scan/resolve")
        .then()
                .statusCode(200)
                .body("supported", is(true))
                .body("gameId", equalTo("ticket-to-ride"))
                .body("name", equalTo("Ticket to Ride"));
    }

    @Test
    void returnsUnsupportedForUnknownBarcode() {
        given()
                .contentType("application/json")
                .body("{\"barcode\": \"9999999999999\"}")
        .when()
                .post("/api/v1/scan/resolve")
        .then()
                .statusCode(200)
                .body("supported", is(false))
                .body("gameId", nullValue())
                .body("name", nullValue());
    }

    @Test
    void returnsBadRequestForMissingBarcode() {
        given()
                .contentType("application/json")
                .body("{}")
        .when()
                .post("/api/v1/scan/resolve")
        .then()
                .statusCode(400);
    }

    @Test
    void returnsBadRequestForBlankBarcode() {
        given()
                .contentType("application/json")
                .body("{\"barcode\": \"  \"}")
        .when()
                .post("/api/v1/scan/resolve")
        .then()
                .statusCode(400);
    }
}
