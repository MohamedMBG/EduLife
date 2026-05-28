package com.edulife.groups;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.groups.controller.GroupController;
import com.edulife.groups.dto.GroupCourseDto;
import com.edulife.groups.dto.GroupDto;
import com.edulife.groups.dto.GroupMemberDto;
import com.edulife.groups.service.GroupService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class GroupControllerTest {

    private static final UUID GROUP_ID  = UUID.fromString("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa");
    private static final UUID USER_ID   = UUID.fromString("bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb");
    private static final UUID COURSE_ID = UUID.fromString("cccccccc-3333-3333-3333-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupService groupService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    @Test
    void createGroupReturns201ForTeacher() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.createGroup(any()))
                .willReturn(new GroupDto(GROUP_ID, "Bac SM 2026", USER_ID, Instant.parse("2026-05-28T10:00:00Z")));

        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bac SM 2026\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(GROUP_ID.toString()))
                .andExpect(jsonPath("$.name").value("Bac SM 2026"))
                .andExpect(jsonPath("$.createdBy").value(USER_ID.toString()));
    }

    @Test
    void createGroupReturns201ForAdmin() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.ADMIN);

        given(groupService.createGroup(any()))
                .willReturn(new GroupDto(GROUP_ID, "Admin cohort", USER_ID, Instant.parse("2026-05-28T10:00:00Z")));

        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin cohort\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createGroupReturns403ForLearner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.LEARNER);

        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Forbidden\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(groupService);
    }

    @Test
    void createGroupReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bac SM\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(groupService);
    }

    @Test
    void createGroupRejectsBlankName() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(groupService);
    }

    @Test
    void addMemberReturns201ForTeacherOwner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.addMember(eq(GROUP_ID), any()))
                .willReturn(new GroupMemberDto(GROUP_ID, USER_ID, Instant.parse("2026-05-28T10:05:00Z")));

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + USER_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void addMemberReturns409WhenAlreadyInGroup() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.addMember(eq(GROUP_ID), any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User already in group"));

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + USER_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already in group"));
    }

    @Test
    void addMemberReturns403ForNonOwnerTeacher() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.addMember(eq(GROUP_ID), any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the group owner"));

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + USER_ID + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not the group owner"));
    }

    @Test
    void removeMemberReturns204ForOwner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        willDoNothing().given(groupService).removeMember(GROUP_ID, USER_ID);

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{userId}", GROUP_ID, USER_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeMemberReturns404WhenAbsent() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in group"))
                .given(groupService).removeMember(GROUP_ID, USER_ID);

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{userId}", GROUP_ID, USER_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeMemberReturns403ForLearner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.LEARNER);

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{userId}", GROUP_ID, USER_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(groupService);
    }

    @Test
    void attachCourseReturns201ForOwner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.attachCourse(eq(GROUP_ID), any()))
                .willReturn(new GroupCourseDto(GROUP_ID, COURSE_ID, Instant.parse("2026-05-28T10:10:00Z")));

        mockMvc.perform(post("/api/v1/groups/{groupId}/courses", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + COURSE_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(COURSE_ID.toString()));
    }

    @Test
    void attachCourseReturns404WhenCourseMissing() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.attachCourse(eq(GROUP_ID), any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        mockMvc.perform(post("/api/v1/groups/{groupId}/courses", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + COURSE_ID + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    void attachCourseReturns403ForLearner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.LEARNER);

        mockMvc.perform(post("/api/v1/groups/{groupId}/courses", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + COURSE_ID + "\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(groupService);
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("teacher@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }

    private void mockUserRole(UserRole role) {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        given(user.getRole()).willReturn(role);
        given(userRepository.findByFirebaseUid("firebase-uid-123")).willReturn(Optional.of(user));
    }
}
