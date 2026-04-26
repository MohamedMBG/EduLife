package com.edulife;

import com.edulife.config.FirebaseAdminProperties;
import com.edulife.config.FirebaseConfig;
import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackendApplicationTests {

    private final FirebaseConfig firebaseConfig = new FirebaseConfig();

    @AfterEach
    void tearDown() {
        for (FirebaseApp app : FirebaseApp.getApps()) {
            app.delete();
        }
    }

    @Test
    void initializesFirebaseAppWhenValidCredentialsPathIsProvided() throws Exception {
        FirebaseAdminProperties properties = new FirebaseAdminProperties();
        properties.setCredentialsPath(createServiceAccountFile().toString());

        assertDoesNotThrow(() -> firebaseConfig.firebaseApp(properties));
    }

    @Test
    void failsFastWhenCredentialsAreMissing() {
        FirebaseAdminProperties properties = new FirebaseAdminProperties();

        assertThrows(IllegalStateException.class, () -> firebaseConfig.firebaseApp(properties));
    }

    @Test
    void failsFastWhenCredentialsPathIsInvalid() {
        FirebaseAdminProperties properties = new FirebaseAdminProperties();
        properties.setCredentialsPath("C:\\missing\\firebase-admin.json");

        assertThrows(IllegalStateException.class, () -> firebaseConfig.firebaseApp(properties));
    }

    private Path createServiceAccountFile() throws Exception {
        Path credentialsFile = Files.createTempFile("firebase-admin-test", ".json");

        // This is a structurally valid test credential so startup behavior can be verified without real secrets.
        Files.writeString(credentialsFile, """
                {
                  "type": "service_account",
                  "project_id": "edulife-test",
                  "private_key_id": "test-key-id",
                  "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDAdMsF0ggkfY9S\\n7jweBj1lXxe2foNQmpAB6M8ZrAq8m73Q8sEfR0tbZGnUWVTX5gMabL2q8sIo6iiu\\ni5P0d4nTBXBxmX9jUyjvAFh7voeisq90x5ggOVrVx1yM9ql9zNyctT6RaJS04lKH\\n0gP9Yye8+U5NQWQkFWwN4Zd8e0+88RrL2j2+RP1sUbAdfX2pbWj/FGEuLLHwQ2qH\\neNgg8wOWnMa5v7fWduR2i11p1Wuz0KcE0vW8YSCaCjjlwmQ0QyLSd4p9jMaN6+lW\\n5+ayfTzQf1qY6n+6uOtjVmnSfVpw5v7RRCuuQbLfU6t4iy8kw3vTQ7N8Hc0b5sqT\\nQ8i1tv5VAgMBAAECggEAA1ToUfkPLp1N7PKTn4Rzf6k8se8guU4l7b95/Xko7Qm6\\n9eEOKWbDF+L9lFcXNRCQwF1qsgUeQzox9IjI5I4i9N87HfSj+oeYH2XN1fLf8Rrw\\n3lTp11Ef/JZpVKZjS63Mec1N0Uz7a4DKhIq+uA84A4PXF8A/KrmmfWBTjPO8n2if\\n4QhJK7wI/Yn3sz3rPpkM3PGuNaB4R0LIn8NLzYuCJFRs6GScydLEs0B5uRYP8J1i\\nSvlv8UwTG5lS+IPww4FE+8B7ewahk5k6hQxot0xC5AVkqzjX6cNLf9mM9dk7G9ff\\naP4r4/AS2kP0Q/2oBzWf0yVSiQfR9MeZ6h0UDQDE6QKBgQD6kRIT2K2k9l6vWrh0\\nzVgD8yv7YnJARr9iP1w1P5dH1gR6G7T0YDXr6v+arYqW2aQJ/7vHTiWLJ0EE0TpB\\no08vP+6Sg4Xn4cXzIR8x9tZoM2nUwTvqZVfk5eMjkBtW27lwk1mdWGY3JSE6m6IX\\nW5PHTE2m4L1dAMR5NE4R7QLJcwKBgQDEvb0Y8eYVZnIQS7qY0V8YfVw7RmtxF9pG\\neNgVWbVjpqkb3Jx4N7n7Qv1hI4X+1zqT4LCnqvA7rwGMqWh44K1HBDq3SkzyV8p2\\nF1uLkCvQd+KY7XoXg7AX9RwXV1P1BK0cwZr0r1mlC0QcUeMZaLlaLtrGv/5Q7W3a\\nS5l4MklxXQKBgB5nL8I13x2Jz0ITk6dFqWj4TzGxG1W4IMFh2WgaN3Zb2WagkEnZ\\n8jTO5E1LS15A3jgjv+FM3b0FPXqYt0L+AQpP4Lsx1+t6dHCOvwnhgY4U2Mw2QHqf\\nXtfuEcIkzH1jFh1vq3zj7k9yiK4wz+t3GOSvG06pD8nZpLn2t4uPRlR7AoGBAMv6\\nvKZ9+JtWjkR3qFrWVx2mLx2yzm1j0rEh7Sp8WXL6SLf8x/4I2wM1pAV5hhk76iLh\\n6fh0g7j0LwDP1S7S+rA1PoD5f5uPEuP5vm8Sa5SqqSxKkxs1p5L7m6bA0d7d0QJ8\\n4TR0ZZWQ7j0MkgvtQkEV4h5KnwY4/3Uqjv+qWw/9AoGAXdJrKx+f8+yHTe/2gEzN\\n8M3DPAUh8n1L6iVpTR4Dz6Y8oPvf4P4joPhA5XIEvM1JjUmn6CkA8bJ0V+G83m2i\\n7VjITkcu9H3gsDPNmqcQmA4rY2m+eyl3x+1C7NIfyk2f4qC6i6iN2kJhrG2X0zga\\nYJ6JQ3UcY4x5b8UQBLuV9ic=\\n-----END PRIVATE KEY-----\\n",
                  "client_email": "firebase-adminsdk-test@edulife-test.iam.gserviceaccount.com",
                  "client_id": "1234567890",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token",
                  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-test%40edulife-test.iam.gserviceaccount.com"
                }
                """);

        credentialsFile.toFile().deleteOnExit();
        return credentialsFile;
    }
}
