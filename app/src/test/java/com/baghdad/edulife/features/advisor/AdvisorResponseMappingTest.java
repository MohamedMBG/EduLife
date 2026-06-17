package com.baghdad.edulife.features.advisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.baghdad.edulife.features.advisor.model.AdvisorRecommendation;
import com.baghdad.edulife.features.advisor.model.AdvisorResponse;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Host-JVM tests that the advisor JSON contract deserializes into the app models exactly as the
 * backend sends it. Uses the same Gson the Retrofit GsonConverterFactory uses, so this exercises
 * the real wire mapping without any network call to the backend or Groq.
 */
public class AdvisorResponseMappingTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesMessageRecommendationsAndGroqSource() {
        String json = "{"
                + "\"message\":\"Here are some matches\","
                + "\"source\":\"groq\","
                + "\"recommendations\":["
                + "  {\"courseId\":\"c-1\",\"reason\":\"Strong fit\",\"score\":0.92,"
                + "   \"matchedSkills\":[\"java\",\"spring\"]},"
                + "  {\"courseId\":\"c-2\",\"reason\":\"Good base\",\"score\":0.5,"
                + "   \"matchedSkills\":[\"sql\"]}"
                + "]}";

        AdvisorResponse response = gson.fromJson(json, AdvisorResponse.class);

        assertEquals("Here are some matches", response.message);
        assertEquals("groq", response.source);
        assertNotNull(response.recommendations);
        assertEquals(2, response.recommendations.size());

        AdvisorRecommendation first = response.recommendations.get(0);
        assertEquals("c-1", first.courseId);
        assertEquals("Strong fit", first.reason);
        assertEquals(0.92, first.score, 0.0001);
        assertEquals(2, first.matchedSkills.size());
        assertEquals("java", first.matchedSkills.get(0));
        assertEquals("spring", first.matchedSkills.get(1));

        assertEquals("sql", response.recommendations.get(1).matchedSkills.get(0));
    }

    @Test
    public void parsesDeterministicFallbackSource() {
        String json = "{\"message\":\"Fallback ranking\",\"source\":\"deterministic-fallback\","
                + "\"recommendations\":[]}";

        AdvisorResponse response = gson.fromJson(json, AdvisorResponse.class);

        assertEquals("deterministic-fallback", response.source);
        assertNotNull(response.recommendations);
        assertTrue(response.recommendations.isEmpty());
    }

    @Test
    public void emptyRecommendationsArray_parsesToEmptyList() {
        String json = "{\"message\":\"No matches yet\",\"source\":\"groq\",\"recommendations\":[]}";

        AdvisorResponse response = gson.fromJson(json, AdvisorResponse.class);

        assertNotNull(response.recommendations);
        assertTrue(response.recommendations.isEmpty());
    }

    @Test
    public void missingSourceField_parsesToNull_doesNotCrash() {
        String json = "{\"message\":\"Hi\",\"recommendations\":[]}";

        AdvisorResponse response = gson.fromJson(json, AdvisorResponse.class);

        assertNull(response.source);
        assertEquals("Hi", response.message);
        assertNotNull(response.recommendations);
    }

    @Test
    public void missingRecommendationsField_parsesToNull_doesNotCrash() {
        // The advisor ViewModel guards null recommendations with an empty list, so a payload
        // missing the field must deserialize to null rather than throwing here.
        String json = "{\"message\":\"Hi\",\"source\":\"groq\"}";

        AdvisorResponse response = gson.fromJson(json, AdvisorResponse.class);

        assertNull(response.recommendations);
        assertEquals("groq", response.source);
    }

    @Test
    public void recommendationWithoutMatchedSkills_parsesScoreAndNullSkills() {
        String json = "{\"message\":\"\",\"source\":\"groq\",\"recommendations\":["
                + "{\"courseId\":\"c-9\",\"reason\":\"Why\",\"score\":1.0}]}";

        AdvisorResponse response = gson.fromJson(json, AdvisorResponse.class);

        AdvisorRecommendation rec = response.recommendations.get(0);
        assertEquals("c-9", rec.courseId);
        assertEquals(1.0, rec.score, 0.0001);
        assertNull(rec.matchedSkills);
    }
}
