package edu.uet.travel_hub.application.port.in;

import java.util.List;

import edu.uet.travel_hub.application.dto.request.UploadFileRequest;
import edu.uet.travel_hub.domain.model.UploadModel;

public interface UploadImageUseCase {
    List<UploadModel> getPresignedUrls(UploadFileRequest file);
}