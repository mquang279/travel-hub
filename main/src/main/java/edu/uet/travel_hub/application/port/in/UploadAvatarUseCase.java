package edu.uet.travel_hub.application.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface UploadAvatarUseCase {
    String uploadAvatar(Long userId, MultipartFile file);
}
