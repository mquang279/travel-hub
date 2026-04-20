package edu.uet.travel_hub.infrastructure.config;

import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;

@Component
public class MinioInitializer {
    private final MinioConfig minioConfig;

    private final MinioClient minioClient;

    public MinioInitializer(MinioConfig minioConfig, MinioClient minioClient) {
        this.minioConfig = minioConfig;
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void initBucket() {
        try {
            String policy = """
                    {
                      "Version":"2012-10-17",
                      "Statement":[
                        {
                          "Effect":"Allow",
                          "Principal":"*",
                          "Action":["s3:GetObject"],
                          "Resource":["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                    """.formatted(minioConfig.getBucketName());

            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build());

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build());
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .config(policy)
                                .build());
            }

        } catch (Exception e) {
            throw new RuntimeException("Init MinIO bucket failed", e);
        }
    }
}
