package edu.uet.travel_hub.application.usecases;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.uet.travel_hub.application.port.in.UploadAvatarUseCase;
import edu.uet.travel_hub.infrastructure.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadAvatarService implements UploadAvatarUseCase {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file must not be empty");
        }

        String objectName = buildAvatarObjectName(userId, file.getOriginalFilename(), file.getContentType());

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(new ByteArrayInputStream(file.getBytes()), file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload avatar", e);
        }

        String uploadedUrl = buildPublicAvatarUrl(objectName);

        return uploadedUrl;
    }

    private String buildAvatarObjectName(Long userId, String originalFilename, String contentType) {
        String extension = extractExtension(originalFilename, contentType);
        return "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).trim();
            if (!extension.isBlank()) {
                return extension.toLowerCase();
            }
        }

        if (contentType != null) {
            if (contentType.equalsIgnoreCase("image/png")) {
                return "png";
            }
            if (contentType.equalsIgnoreCase("image/webp")) {
                return "webp";
            }
            if (contentType.equalsIgnoreCase("image/gif")) {
                return "gif";
            }
        }

        return "jpg";
    }

    private String buildPublicAvatarUrl(String objectName) {
        String publicUrl = minioConfig.getPublicUrl();
        String bucketName = minioConfig.getBucketName();

        if (publicUrl.endsWith("/")) {
            publicUrl = publicUrl.substring(0, publicUrl.length() - 1);
        }

        return publicUrl + "/" + bucketName + "/" + objectName;
    }
}
