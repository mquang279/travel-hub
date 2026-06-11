package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.UploadFileRequest;
import edu.uet.travel_hub.application.dto.response.AvatarUploadResponse;
import edu.uet.travel_hub.application.dto.response.UploadResponse;
import edu.uet.travel_hub.application.port.in.UploadImageUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.domain.model.UploadModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private final UploadImageUseCase uploadImageUseCase;
    private final CurrentUserProvider currentUserProvider;

    public UploadController(
            UploadImageUseCase uploadImageUseCase,
            CurrentUserProvider currentUserProvider) {
        this.uploadImageUseCase = uploadImageUseCase;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("")
    public ResponseEntity<UploadResponse> getPresignedUrls(@RequestBody UploadFileRequest request) {
        List<UploadModel> urls = this.uploadImageUseCase.getPresignedUrls(request);
        return ResponseEntity.ok(new UploadResponse(urls));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh đại diện không hợp lệ");
        }

        Long currentUserId = currentUserProvider.getCurrentUserId();
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? MediaType.IMAGE_JPEG_VALUE
                : file.getContentType();
        String relativePath = "avatars/" + currentUserId + "/" + UUID.randomUUID() + extensionFrom(contentType);
        Path target = Path.of("uploads").resolve(relativePath).normalize();

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tải ảnh đại diện lên. Vui lòng thử lại sau.", exception);
        }

        return ResponseEntity.ok(new AvatarUploadResponse(publicUrl(relativePath)));
    }

    private String publicUrl(String relativePath) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(relativePath)
                .toUriString();
    }

    private String extensionFrom(String contentType) {
        if (MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if (MediaType.IMAGE_GIF_VALUE.equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

}
