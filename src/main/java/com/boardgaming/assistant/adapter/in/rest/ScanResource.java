package com.boardgaming.assistant.adapter.in.rest;

import com.boardgaming.assistant.application.dto.ErrorResponse;
import com.boardgaming.assistant.application.dto.ScanRequest;
import com.boardgaming.assistant.application.dto.ScanResponse;
import com.boardgaming.assistant.application.usecase.ResolveBarcodeUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/scan")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ScanResource {

    @Inject
    ResolveBarcodeUseCase resolveBarcodeUseCase;

    @POST
    @Path("/resolve")
    public Response resolve(ScanRequest request) {
        if (request == null || request.barcode() == null || request.barcode().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("barcode is required"))
                    .build();
        }

        ScanResponse result = resolveBarcodeUseCase.execute(request);
        return Response.ok(result).build();
    }
}
