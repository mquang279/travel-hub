package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import edu.uet.travel_hub.application.port.out.FileStorage;
import edu.uet.travel_hub.domain.model.UploadModel;
import edu.uet.travel_hub.infrastructure.config.MinioConfig;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;

@Component
public class MinioStorage implements FileStorage {
    private final MinioClient minioClient;

    private final MinioClient minioPresignedClient;

    private final MinioConfig minioConfig;

    public MinioStorage(
            MinioClient minioClient,
            @Qualifier("minioPresignedClient") MinioClient minioPresignedClient,
            MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioPresignedClient = minioPresignedClient;
        this.minioConfig = minioConfig;
    }

    @Override
    public List<UploadModel> getPresignedUrl(String folderName, Long userId, int totalFiles) {
        List<UploadModel> presignedUrls = new ArrayList<>();
        for (int i = 0; i < totalFiles; i++) {
            try {
                String objectName = folderName + "/" + userId.toString() + "/" + UUID.randomUUID() + ".jpg";
                String url = minioPresignedClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(minioConfig.getBucketName())
                                .object(objectName)
                                .region(minioConfig.getRegion())
                                .expiry(5, TimeUnit.MINUTES)
                                .build());
                presignedUrls.add(new UploadModel(objectName, url));
            } catch (Exception e) {
                throw new RuntimeException("Error when generating presigned image URL", e);
            }
        }
        return presignedUrls;
    }

    @Override
    public UploadModel upload(String folderName, Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Proof file must not be empty");
        }
        try {
            String extension = extensionOf(file.getOriginalFilename());
            String objectName = folderName + "/" + userId + "/" + UUID.randomUUID() + extension;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            String publicUrl = minioConfig.getPublicUrl();
            String imageUrl = publicUrl.endsWith("/")
                    ? publicUrl + minioConfig.getBucketName() + "/" + objectName
                    : publicUrl + "/" + minioConfig.getBucketName() + "/" + objectName;
            return new UploadModel(objectName, imageUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload payment proof", e);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || filename.isBlank()) {
            return ".jpg";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return ".jpg";
        }
        return filename.substring(dot);
    }
}
