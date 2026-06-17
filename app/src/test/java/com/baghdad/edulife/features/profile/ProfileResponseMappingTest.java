package com.baghdad.edulife.features.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.baghdad.edulife.features.profile.model.AvatarUploadResponse;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Host-JVM tests for profile and avatar response mapping. Verifies the @SerializedName contract
 * and that avatar URLs (including absolute HTTPS CDN URLs) round-trip exactly, while a missing
 * avatarUrl deserializes to null so the UI can fall back to initials. No network is used.
 */
public class ProfileResponseMappingTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesFullProfileIncludingAvatarUrlAndCounts() {
        String json = "{"
                + "\"userId\":\"u-1\","
                + "\"email\":\"learner@example.com\","
                + "\"displayName\":\"Sam Learner\","
                + "\"bio\":\"Hello\","
                + "\"avatarUrl\":\"https://res.cloudinary.com/demo/image/upload/v1/avatar.jpg\","
                + "\"enrolledCourses\":3,"
                + "\"completedLessons\":12,"
                + "\"certificates\":1}";

        ProfileResponse profile = gson.fromJson(json, ProfileResponse.class);

        assertEquals("u-1", profile.userId);
        assertEquals("learner@example.com", profile.email);
        assertEquals("Sam Learner", profile.displayName);
        assertEquals("Hello", profile.bio);
        assertEquals(3, profile.enrolledCourses);
        assertEquals(12, profile.completedLessons);
        assertEquals(1, profile.certificates);
    }

    @Test
    public void cloudinaryHttpsAvatarUrl_isPreservedExactly() {
        String url = "https://res.cloudinary.com/demo/image/upload/v1700000000/avatars/u-1.webp";
        String json = "{\"userId\":\"u-1\",\"avatarUrl\":\"" + url + "\"}";

        ProfileResponse profile = gson.fromJson(json, ProfileResponse.class);

        assertEquals(url, profile.avatarUrl);
        assertTrue(profile.avatarUrl.startsWith("https://"));
    }

    @Test
    public void missingAvatarUrl_parsesToNull_forFallbackHandling() {
        String json = "{\"userId\":\"u-1\",\"displayName\":\"Sam\"}";

        ProfileResponse profile = gson.fromJson(json, ProfileResponse.class);

        assertNull(profile.avatarUrl);
        // Numeric counts default to 0 when absent rather than crashing.
        assertEquals(0, profile.enrolledCourses);
        assertEquals(0, profile.certificates);
    }

    @Test
    public void avatarUploadResponse_parsesAvatarUrl() {
        String json = "{\"avatarUrl\":"
                + "\"https://res.cloudinary.com/demo/image/upload/v1/new-avatar.jpg\"}";

        AvatarUploadResponse response = gson.fromJson(json, AvatarUploadResponse.class);

        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/new-avatar.jpg",
                response.avatarUrl);
    }

    @Test
    public void avatarUploadResponse_missingUrl_parsesToNull() {
        AvatarUploadResponse response = gson.fromJson("{}", AvatarUploadResponse.class);

        assertNull(response.avatarUrl);
    }
}
