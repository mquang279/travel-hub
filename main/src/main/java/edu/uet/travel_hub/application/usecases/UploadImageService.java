package edu.uet.travel_hub.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.request.UploadFileRequest;
import edu.uet.travel_hub.application.port.in.UploadImageUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.FileStorage;
import edu.uet.travel_hub.domain.model.UploadModel;

@Service
public class UploadImageService implements UploadImageUseCase {
    private final FileStorage fileStorage;
    private final CurrentUserProvider currentUserProvider;

    public UploadImageService(FileStorage fileStorage, CurrentUserProvider currentUserProvider) {
        this.fileStorage = fileStorage;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<UploadModel> getPresignedUrls(UploadFileRequest file) {
        return this.fileStorage.getPresignedUrl(
                file.folderName(),
                currentUserProvider.getCurrentUserId(),
                file.files());
    }

}
