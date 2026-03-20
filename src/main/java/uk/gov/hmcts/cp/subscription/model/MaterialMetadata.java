package uk.gov.hmcts.cp.subscription.model;

import lombok.Data;

import java.util.UUID;

@Data
public class MaterialMetadata {
    private UUID materialId;
    private String alfrescoAssetId;
    private String fileName;
    private String mimeType;
    private String materialAddedDate;
    private String externalLink;
}