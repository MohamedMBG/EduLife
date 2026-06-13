package com.edulife.admin;

import com.edulife.admin.controller.CmsCourseController;
import com.edulife.admin.dto.CourseAdminDto;
import com.edulife.admin.service.CmsCourseService;
import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.courses.model.CourseStatus;
import com.edulife.security.SecurityConfig;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsCourseController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class CmsCoursePublishTest {

    private static final UUID COURSE_ID = UUID.fromString("cccccccc-3333-3333-3333-cccccccccccc");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CmsCourseService cmsCourseService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    @Test
    void publishReturns200ForGroupAdmin() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(cmsCourseService.publishCourse(COURSE_ID)).willReturn(publishedDto());

        mockMvc.perform(put("/api/v1/cms/courses/{id}/publish", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishReturns403WhenGroupAdminDoesNotManageAuthor() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(cmsCourseService.publishCourse(COURSE_ID)).willThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You can only approve courses from teachers in your groups"));

        mockMvc.perform(put("/api/v1/cms/courses/{id}/publish", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You can only approve courses from teachers in your groups"));
    }

    @Test
    void publishReturns403ForTeacher() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        mockMvc.perform(put("/api/v1/cms/courses/{id}/publish", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cmsCourseService);
    }

    @Test
    void publishReturns200ForAdmin() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.ADMIN);

        given(cmsCourseService.publishCourse(COURSE_ID)).willReturn(publishedDto());

        mockMvc.perform(put("/api/v1/cms/courses/{id}/publish", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void archiveStaysAdminOnly() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        mockMvc.perform(put("/api/v1/cms/courses/{id}/archive", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cmsCourseService);
    }

    private CourseAdminDto publishedDto() {
        return new CourseAdminDto(
                COURSE_ID, "course-slug", "Course", "Short", "Long description",
                "en", "Beginner", null, CourseStatus.PUBLISHED,
                Instant.parse("2026-06-13T10:00:00Z"), USER_ID, "teacher@edulife.test",
                Instant.parse("2026-06-12T10:00:00Z"), Instant.parse("2026-06-13T10:00:00Z"));
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("groupadmin@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }

    private void mockUserRole(UserRole role) {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        given(user.getRole()).willReturn(role);
        given(userRepository.findByFirebaseUid("firebase-uid-123")).willReturn(Optional.of(user));
    }
}
