package edu.uet.travel_hub.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UploadModel {
    private String objectName;

    private String url;

    private String publicUrl;

    public UploadModel(String objectName, String url) {
        this.objectName = objectName;
        this.url = url;
        this.publicUrl = url;
    }

    public UploadModel(String objectName, String url, String publicUrl) {
        this.objectName = objectName;
        this.url = url;
        this.publicUrl = publicUrl;
    }
}
