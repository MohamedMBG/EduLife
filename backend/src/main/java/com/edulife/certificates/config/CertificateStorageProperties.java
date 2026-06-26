package com.edulife.certificates.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for certificate PDF storage location and public URL prefix. */
@ConfigurationProperties(prefix = "edulife.certificates")
public class CertificateStorageProperties {

    private String storageDir = "storage/certificates";
    private String publicBaseUrl = "http://localhost:8080/api/v1/certificates";

    public String getStorageDir() { return storageDir; }
    public void setStorageDir(String storageDir) { this.storageDir = storageDir; }

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
}
