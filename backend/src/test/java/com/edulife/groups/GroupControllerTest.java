package com.edulife.groups;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.groups.controller.GroupController;
import com.edulife.groups.dto.GroupCourseDto;
import com.edulife.groups.dto.GroupDetailDto;
import com.edulife.groups.dto.GroupDto;
import com.edulife.groups.dto.GroupJoinRequestDto;
import com.edulife.groups.dto.GroupMemberDetailDto;
import com.edulife.groups.dto.GroupMemberDto;
import com.edulife.groups.dto.GroupSummaryDto;
import com.edulife.groups.model.GroupJoinRequestStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class GroupControllerTest {

    private static final UUID GROUP_ID  = UUID.fromString("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa");
    private static final UUID USER_ID   = UUID.fromString("bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb");
    private static final UUID COURSE_ID = UUID.fromString("cccccccc-3333-3333-3333-cccccccccccc");
    private static final UUID REQUEST_ID = UUID.fromString("dddddddd-4444-4444-4444-dddddddddddd");

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
    void submitJoinRequestReturns201ForTeacher() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.submitJoinRequest(eq(GROUP_ID), any()))
                .willReturn(joinRequestDto(GroupJoinRequestStatus.PENDING));

        mockMvc.perform(post("/api/v1/groups/{groupId}/join-requests", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivation\":\"I want institute backing for course review.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()));
    }

    @Test
    void submitJoinRequestReturns403ForLearner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.LEARNER);

        mockMvc.perform(post("/api/v1/groups/{groupId}/join-requests", GROUP_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivation\":\"Let me join.\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(groupService);
    }

    @Test
    void listMyJoinRequestsReturns200ForTeacher() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(groupService.listMyJoinRequests()).willReturn(java.util.List.of(
                joinRequestDto(GroupJoinRequestStatus.PENDING)));

        mockMvc.perform(get("/api/v1/groups/join-requests/mine")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$[0].groupName").value("Institute A"));
    }

    @Test
    void listGroupJoinRequestsReturns200ForGroupAdmin() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(groupService.listGroupJoinRequests(GROUP_ID, GroupJoinRequestStatus.PENDING))
                .willReturn(java.util.List.of(joinRequestDto(GroupJoinRequestStatus.PENDING)));

        mockMvc.perform(get("/api/v1/groups/{groupId}/join-requests?status=PENDING", GROUP_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requesterEmail").value("teacher@edulife.test"));
    }

    @Test
    void approveJoinRequestReturns200ForGroupAdmin() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(groupService.approveJoinRequest(GROUP_ID, REQUEST_ID))
                .willReturn(joinRequestDto(GroupJoinRequestStatus.APPROVED));

        mockMvc.perform(put("/api/v1/groups/{groupId}/join-requests/{requestId}/approve", GROUP_ID, REQUEST_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectJoinRequestReturns200ForGroupAdmin() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(groupService.rejectJoinRequest(eq(GROUP_ID), eq(REQUEST_ID), any()))
                .willReturn(joinRequestDto(GroupJoinRequestStatus.REJECTED));

        mockMvc.perform(put("/api/v1/groups/{groupId}/join-requests/{requestId}/reject", GROUP_ID, REQUEST_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminNote\":\"Profile does not match this institute.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void listGroupsReturns200ForGroupAdmin() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(groupService.listMyGroups()).willReturn(java.util.List.of(
                new GroupSummaryDto(GROUP_ID, "Bac SM 2026", Instant.parse("2026-05-28T10:00:00Z"), 4, 2)));

        mockMvc.perform(get("/api/v1/groups")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(GROUP_ID.toString()))
                .andExpect(jsonPath("$[0].memberCount").value(4))
                .andExpect(jsonPath("$[0].courseCount").value(2));
    }

    @Test
    void listGroupsReturns403ForLearner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.LEARNER);

        mockMvc.perform(get("/api/v1/groups")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(groupService);
    }

    @Test
    void getGroupDetailReturns200ForOwner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(groupService.getGroupDetail(GROUP_ID)).willReturn(new GroupDetailDto(
                GROUP_ID,
                "Bac SM 2026",
                Instant.parse("2026-05-28T10:00:00Z"),
                java.util.List.of(new GroupMemberDetailDto(
                        USER_ID, "student@edulife.test", UserRole.LEARNER,
                        Instant.parse("2026-05-28T10:05:00Z"))),
                java.util.List.of()));

        mockMvc.perform(get("/api/v1/groups/{groupId}", GROUP_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bac SM 2026"))
                .andExpect(jsonPath("$.members[0].email").value("student@edulife.test"));
    }

    @Test
    void getGroupDetailReturns403ForNonOwner() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(groupService.getGroupDetail(GROUP_ID))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the group owner"));

        mockMvc.perform(get("/api/v1/groups/{groupId}", GROUP_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
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

    private GroupJoinRequestDto joinRequestDto(GroupJoinRequestStatus status) {
        return new GroupJoinRequestDto(
                REQUEST_ID,
                GROUP_ID,
                "Institute A",
                USER_ID,
                "teacher@edulife.test",
                status,
                "I want institute backing for course review.",
                status == GroupJoinRequestStatus.REJECTED ? "Profile does not match this institute." : null,
                status == GroupJoinRequestStatus.PENDING ? null : USER_ID,
                status == GroupJoinRequestStatus.PENDING ? null : "groupadmin@edulife.test",
                Instant.parse("2026-06-13T10:00:00Z"),
                status == GroupJoinRequestStatus.PENDING ? null : Instant.parse("2026-06-13T11:00:00Z"));
    }
}
