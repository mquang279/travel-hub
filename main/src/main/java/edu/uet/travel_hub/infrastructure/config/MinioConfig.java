package edu.uet.travel_hub.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {
    @Value("${minio.url}")
    private String url;

    @Value("${minio.public.url}")
    private String publicUrl;

    @Value("${minio.username}")
    private String username;

    @Value("${minio.password}")
    private String password;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.region:us-east-1}")
    private String region;

    @Bean
    @Primary
    public MinioClient minioClient() {
        return MinioClient
                .builder()
                .endpoint(url)
                .credentials(username, password)
                .build();
    }

    @Bean(name = "minioPresignedClient")
    public MinioClient minioPresignedClient() {
        return MinioClient
                .builder()
                .endpoint(publicUrl)
                .credentials(username, password)
                .build();
    }
}
