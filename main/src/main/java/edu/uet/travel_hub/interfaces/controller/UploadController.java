package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.UploadFileRequest;
import edu.uet.travel_hub.application.dto.response.UploadResponse;
import edu.uet.travel_hub.application.port.in.UploadImageUseCase;
import edu.uet.travel_hub.domain.model.UploadModel;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private final UploadImageUseCase uploadImageUseCase;

    public UploadController(UploadImageUseCase uploadImageUseCase) {
        this.uploadImageUseCase = uploadImageUseCase;
    }

    @PostMapping("")
    public ResponseEntity<UploadResponse> getPresignedUrls(@RequestBody UploadFileRequest request) {
        List<UploadModel> urls = this.uploadImageUseCase.getPresignedUrls(request);
        return ResponseEntity.ok(new UploadResponse(urls));
    }
}
