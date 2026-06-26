package com.edulife.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalized properties for locating Firebase Admin SDK credentials (file path or raw JSON). */
@ConfigurationProperties(prefix = "firebase.admin")
public class FirebaseAdminProperties {

    private String credentialsPath;
    private String credentialsJson;

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public String getCredentialsJson() {
        return credentialsJson;
    }

    public void setCredentialsJson(String credentialsJson) {
        this.credentialsJson = credentialsJson;
    }
}
