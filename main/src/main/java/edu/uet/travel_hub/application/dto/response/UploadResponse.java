package edu.uet.travel_hub.application.dto.response;

import java.util.List;

import edu.uet.travel_hub.domain.model.UploadModel;

public record UploadResponse(List<UploadModel> items) {

}
