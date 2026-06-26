package com.edulife.profiles.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for avatar storage: directory path, public URL prefix, and max file size (5 MB default).
 */
@ConfigurationProperties(prefix = "edulife.avatar")
public class AvatarStorageProperties {

    /** Filesystem directory where uploaded avatars are persisted, relative to the working dir or absolute. */
    private String storageDir = "uploads/avatars";

    /** Public base URL prefix used to build the returned avatarUrl; must match the static resource mapping. */
    private String publicBaseUrl = "http://localhost:8080/uploads/avatars";

    /** Maximum accepted file size in bytes. Defaults to 5MB per the P4 contract. */
    private long maxFileBytes = 5L * 1024 * 1024;

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
    }
}
