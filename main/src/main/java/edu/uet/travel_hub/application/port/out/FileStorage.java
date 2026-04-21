package edu.uet.travel_hub.application.port.out;

import java.util.List;

import edu.uet.travel_hub.domain.model.UploadModel;

public interface FileStorage {
    List<UploadModel> getPresignedUrl(String folderName, Long userId, int totalFiles);
}
