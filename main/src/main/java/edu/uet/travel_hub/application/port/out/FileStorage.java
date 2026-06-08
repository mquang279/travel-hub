package edu.uet.travel_hub.application.port.out;

import java.util.List;

import edu.uet.travel_hub.domain.model.UploadModel;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    List<UploadModel> getPresignedUrl(String folderName, Long userId, int totalFiles);

    UploadModel upload(String folderName, Long userId, MultipartFile file);
}
