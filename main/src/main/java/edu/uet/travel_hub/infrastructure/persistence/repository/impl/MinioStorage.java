package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.application.port.out.FileStorage;
import edu.uet.travel_hub.domain.model.UploadModel;
import edu.uet.travel_hub.infrastructure.config.MinioConfig;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;

@Component
public class MinioStorage implements FileStorage {
    private final MinioClient minioClient;

    private final MinioConfig minioConfig;

    public MinioStorage(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    @Override
    public List<UploadModel> getPresignedUrl(String folderName, Long userId, int totalFiles) {
        List<UploadModel> presignedUrls = new ArrayList<>();
        for (int i = 0; i < totalFiles; i++) {
            try {
                String objectName = folderName + "/" + userId.toString() + "/" + UUID.randomUUID() + ".jpg";
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(minioConfig.getBucketName())
                                .object(objectName)
                                .expiry(5, TimeUnit.MINUTES)
                                .build());
                presignedUrls.add(new UploadModel(objectName, url));
            } catch (Exception e) {
                throw new RuntimeException("Error when uploading image");
            }
        }
        return presignedUrls;
    }

}
