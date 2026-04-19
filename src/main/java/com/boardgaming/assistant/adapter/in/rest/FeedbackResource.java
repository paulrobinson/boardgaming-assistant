package com.boardgaming.assistant.adapter.in.rest;

import com.boardgaming.assistant.application.dto.ErrorResponse;
import com.boardgaming.assistant.application.dto.FeedbackRequest;
import com.boardgaming.assistant.application.dto.FeedbackResponse;
import com.boardgaming.assistant.application.usecase.SubmitFeedbackUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/feedback")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FeedbackResource {

    @Inject
    SubmitFeedbackUseCase submitFeedbackUseCase;

    @POST
    public Response submit(FeedbackRequest request) {
        if (request == null || request.estimateId() == null || request.estimateId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("estimateId is required"))
                    .build();
        }

        FeedbackResponse result = submitFeedbackUseCase.execute(request);
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Estimate not found: " + request.estimateId()))
                    .build();
        }

        return Response.ok(result).build();
    }
}
