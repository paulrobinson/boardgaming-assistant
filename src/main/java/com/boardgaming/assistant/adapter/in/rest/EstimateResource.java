package com.boardgaming.assistant.adapter.in.rest;

import com.boardgaming.assistant.application.dto.ErrorResponse;
import com.boardgaming.assistant.application.dto.EstimateRequest;
import com.boardgaming.assistant.application.dto.EstimateResponse;
import com.boardgaming.assistant.application.usecase.CreateTimingEstimateUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/estimates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EstimateResource {

    @Inject
    CreateTimingEstimateUseCase createTimingEstimateUseCase;

    @POST
    public Response create(EstimateRequest request) {
        if (request == null || request.gameId() == null || request.gameId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("gameId is required"))
                    .build();
        }

        if (request.groupProfile() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("groupProfile is required"))
                    .build();
        }

        try {
            EstimateResponse result = createTimingEstimateUseCase.execute(request);
            if (result == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Game not found: " + request.gameId()))
                        .build();
            }
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }
}
