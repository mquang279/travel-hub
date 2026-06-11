package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.uet.travel_hub.application.dto.request.UploadFileRequest;
import edu.uet.travel_hub.application.dto.response.AvatarUploadResponse;
import edu.uet.travel_hub.application.dto.response.UploadResponse;
import edu.uet.travel_hub.application.port.in.UploadImageUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.domain.model.UploadModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private static final Path AVATAR_UPLOAD_ROOT = Path.of("uploads", "avatars");
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UploadImageUseCase uploadImageUseCase;
    private final CurrentUserProvider currentUserProvider;

    public UploadController(UploadImageUseCase uploadImageUseCase, CurrentUserProvider currentUserProvider) {
        this.uploadImageUseCase = uploadImageUseCase;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("")
    public ResponseEntity<UploadResponse> getPresignedUrls(@RequestBody UploadFileRequest request) {
        List<UploadModel> urls = this.uploadImageUseCase.getPresignedUrls(request);
        return ResponseEntity.ok(new UploadResponse(urls));
    }

    @PostMapping("/avatar")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported avatar image type");
        }

        Long userId = this.currentUserProvider.getCurrentUserId();
        Path userDirectory = AVATAR_UPLOAD_ROOT.resolve(String.valueOf(userId));
        Files.createDirectories(userDirectory);

        String extension = extensionForContentType(contentType);
        String fileName = UUID.randomUUID() + extension;
        Path targetPath = userDirectory.resolve(fileName);
        file.transferTo(targetPath);

        String avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/avatars/")
                .path(String.valueOf(userId))
                .path("/")
                .path(fileName)
                .toUriString();
        return ResponseEntity.ok(new AvatarUploadResponse(avatarUrl));
    }

    private static String extensionForContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
