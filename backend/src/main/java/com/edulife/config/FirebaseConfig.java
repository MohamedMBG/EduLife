package com.edulife.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes the Firebase Admin SDK from configured credentials and exposes
 * {@link FirebaseAuth} for token verification across all protected endpoints.
 */
@Configuration
@EnableConfigurationProperties(FirebaseAdminProperties.class)
@Profile("!test")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp(FirebaseAdminProperties properties) {
        if (!FirebaseApp.getApps().isEmpty()) {
            // Firebase must be initialized once so token verification uses one shared Admin SDK instance.
            return FirebaseApp.getInstance();
        }

        try (InputStream credentialsStream = openCredentialsStream(properties)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();

            FirebaseApp firebaseApp = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully.");
            return firebaseApp;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize Firebase Admin SDK from the configured credentials source.", exception);
        }
    }

    /** Opens a credentials stream from raw JSON or a file path, failing fast if neither is set. */
    private InputStream openCredentialsStream(FirebaseAdminProperties properties) throws IOException {
        String credentialsJson = properties.getCredentialsJson();
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            // Raw JSON support keeps CI and local setups out of source control when a file path is inconvenient.
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        String credentialsPath = properties.getCredentialsPath();
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            Path path = Path.of(credentialsPath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new IllegalStateException("Firebase Admin credentials file was not found at: " + path.toAbsolutePath());
            }

            return Files.newInputStream(path);
        }

        throw new IllegalStateException(
                "Firebase Admin credentials are required. Set FIREBASE_ADMIN_CREDENTIALS_PATH or FIREBASE_ADMIN_CREDENTIALS_JSON before starting the backend."
        );
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        // Token verification must use the shared FirebaseApp so all protected endpoints validate against one trusted Admin SDK instance.
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
