package com.edulife.groups;

import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.groups.dto.AddMemberRequest;
import com.edulife.groups.dto.AttachCourseRequest;
import com.edulife.groups.dto.GroupCourseDto;
import com.edulife.groups.dto.GroupMemberDto;
import com.edulife.groups.dto.GroupSummaryDto;
import com.edulife.groups.entity.Group;
import com.edulife.groups.entity.GroupCourse;
import com.edulife.groups.entity.GroupMember;
import com.edulife.groups.repository.GroupCourseRepository;
import com.edulife.groups.repository.GroupJoinRequestRepository;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.groups.repository.GroupRepository;
import com.edulife.groups.service.GroupService;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb");
    private static final UUID GROUP_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa");

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
    private final GroupCourseRepository groupCourseRepository = mock(GroupCourseRepository.class);
    private final GroupJoinRequestRepository groupJoinRequestRepository = mock(GroupJoinRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);

    private final GroupService service = new GroupService(
            groupRepository,
            groupMemberRepository,
            groupCourseRepository,
            groupJoinRequestRepository,
            userRepository,
            courseRepository
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMyGroupsCreatesDefaultGroupForNewGroupAdmin() {
        User groupAdmin = user(UserRole.GROUP_ADMIN);
        Group createdGroup = group("My Institute");
        given(userRepository.findByFirebaseUid("firebase-group-admin")).willReturn(Optional.of(groupAdmin));
        given(groupRepository.findAllByCreatedBy(USER_ID))
                .willReturn(List.of())
                .willReturn(List.of(createdGroup));
        given(groupRepository.save(any(Group.class))).willReturn(createdGroup);
        given(groupMemberRepository.countByGroupId(GROUP_ID)).willReturn(0L);
        given(groupCourseRepository.countByGroupId(GROUP_ID)).willReturn(0L);
        authenticate("firebase-group-admin");

        List<GroupSummaryDto> groups = service.listMyGroups();

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getName()).isEqualTo("My Institute");
        assertThat(groupCaptor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().name()).isEqualTo("My Institute");
    }

    @Test
    void listMyGroupsDoesNotAutoCreateForTeacher() {
        User teacher = user(UserRole.TEACHER);
        given(userRepository.findByFirebaseUid("firebase-teacher")).willReturn(Optional.of(teacher));
        given(groupRepository.findAllByCreatedBy(USER_ID)).willReturn(List.of());
        authenticate("firebase-teacher");

        List<GroupSummaryDto> groups = service.listMyGroups();

        assertThat(groups).isEmpty();
        verify(groupRepository).findAllByCreatedBy(USER_ID);
        verifyNoMoreInteractions(groupMemberRepository, groupCourseRepository);
    }

    // ── P2-3: attachCourse scope ────────────────────────────────────────────────

    @Test
    void attachCourseAllowsCourseAuthoredByGroupOwner() {
        given(userRepository.findByFirebaseUid("firebase-group-admin")).willReturn(Optional.of(user(UserRole.GROUP_ADMIN)));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course(USER_ID)));
        given(groupCourseRepository.existsByGroupIdAndCourseId(GROUP_ID, COURSE_ID)).willReturn(false);
        given(groupCourseRepository.save(any(GroupCourse.class))).willAnswer(i -> i.getArgument(0));
        authenticate("firebase-group-admin");

        GroupCourseDto dto = service.attachCourse(GROUP_ID, new AttachCourseRequest(COURSE_ID));

        assertThat(dto.courseId()).isEqualTo(COURSE_ID);
    }

    @Test
    void attachCourseAllowsCourseAuthoredByManagedTeacher() {
        given(userRepository.findByFirebaseUid("firebase-group-admin")).willReturn(Optional.of(user(UserRole.GROUP_ADMIN)));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course(TEACHER_ID)));
        given(groupMemberRepository.existsMemberManagedBy(USER_ID, TEACHER_ID)).willReturn(true);
        given(groupCourseRepository.existsByGroupIdAndCourseId(GROUP_ID, COURSE_ID)).willReturn(false);
        given(groupCourseRepository.save(any(GroupCourse.class))).willAnswer(i -> i.getArgument(0));
        authenticate("firebase-group-admin");

        GroupCourseDto dto = service.attachCourse(GROUP_ID, new AttachCourseRequest(COURSE_ID));

        assertThat(dto.courseId()).isEqualTo(COURSE_ID);
    }

    @Test
    void attachCourseRejectsUnrelatedTeacherCourse() {
        given(userRepository.findByFirebaseUid("firebase-group-admin")).willReturn(Optional.of(user(UserRole.GROUP_ADMIN)));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course(UNRELATED_ID)));
        given(groupMemberRepository.existsMemberManagedBy(USER_ID, UNRELATED_ID)).willReturn(false);
        authenticate("firebase-group-admin");

        assertThatThrownBy(() -> service.attachCourse(GROUP_ID, new AttachCourseRequest(COURSE_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You can only attach courses");
    }

    @Test
    void attachCourseRejectsUnpublishedUnrelatedCourse() {
        given(userRepository.findByFirebaseUid("firebase-group-admin")).willReturn(Optional.of(user(UserRole.GROUP_ADMIN)));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        // DRAFT (unpublished) course authored by an unrelated teacher — blocked by the scope check
        // regardless of status.
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(draftCourse(UNRELATED_ID)));
        given(groupMemberRepository.existsMemberManagedBy(USER_ID, UNRELATED_ID)).willReturn(false);
        authenticate("firebase-group-admin");

        assertThatThrownBy(() -> service.attachCourse(GROUP_ID, new AttachCourseRequest(COURSE_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You can only attach courses");
    }

    @Test
    void attachCourseAllowsPlatformAdminForAnyCourse() {
        User admin = userWith(ADMIN_ID, UserRole.ADMIN, "firebase-admin", "admin-platform@edulife.test");
        given(userRepository.findByFirebaseUid("firebase-admin")).willReturn(Optional.of(admin));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course(UNRELATED_ID)));
        given(groupCourseRepository.existsByGroupIdAndCourseId(GROUP_ID, COURSE_ID)).willReturn(false);
        given(groupCourseRepository.save(any(GroupCourse.class))).willAnswer(i -> i.getArgument(0));
        authenticateAs("firebase-admin", "admin-platform@edulife.test");

        GroupCourseDto dto = service.attachCourse(GROUP_ID, new AttachCourseRequest(COURSE_ID));

        assertThat(dto.courseId()).isEqualTo(COURSE_ID);
    }

    // ── P2-3: addMember enumeration / arbitrary add ─────────────────────────────

    @Test
    void addMemberByUserIdSucceedsForOwner() {
        given(userRepository.findByFirebaseUid("firebase-group-admin")).willReturn(Optional.of(user(UserRole.GROUP_ADMIN)));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        given(userRepository.existsById(TEACHER_ID)).willReturn(true);
        given(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, TEACHER_ID)).willReturn(false);
        given(groupMemberRepository.save(any(GroupMember.class))).willAnswer(i -> i.getArgument(0));
        authenticate("firebase-group-admin");

        GroupMemberDto dto = service.addMember(GROUP_ID, new AddMemberRequest(TEACHER_ID, null));

        assertThat(dto.userId()).isEqualTo(TEACHER_ID);
    }

    @Test
    void addMemberByUnknownEmailDoesNotLeakUserExistence() {
        given(userRepository.findByFirebaseUid("firebase-group-admin")).willReturn(Optional.of(user(UserRole.GROUP_ADMIN)));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        authenticate("firebase-group-admin");

        // A generic 400 is returned for any email, so a caller cannot tell registered from
        // unregistered addresses (no 404 "no user with that email").
        assertThatThrownBy(() -> service.addMember(GROUP_ID, new AddMemberRequest(null, "stranger@edulife.test")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unable to add a member with the provided details")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void addMemberRejectedForNonOwner() {
        User stranger = userWith(STRANGER_ID, UserRole.GROUP_ADMIN, "firebase-stranger", "stranger-admin@edulife.test");
        given(userRepository.findByFirebaseUid("firebase-stranger")).willReturn(Optional.of(stranger));
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group("Institute")));
        authenticateAs("firebase-stranger", "stranger-admin@edulife.test");

        assertThatThrownBy(() -> service.addMember(GROUP_ID, new AddMemberRequest(TEACHER_ID, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the group owner");
    }

    private static final UUID COURSE_ID = UUID.fromString("cccccccc-3333-3333-3333-cccccccccccc");
    private static final UUID TEACHER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNRELATED_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID STRANGER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static Course course(UUID author) {
        return new Course(COURSE_ID, "slug", "Title", "short", "desc", "en", "Bac", null, author);
    }

    private static Course draftCourse(UUID author) {
        // Course defaults to DRAFT status on construction, so this is an unpublished course.
        return course(author);
    }

    private static User userWith(UUID id, UserRole role, String firebaseUid, String email) {
        User user = new User(firebaseUid, email);
        ReflectionTestUtils.setField(user, "id", id);
        user.setRole(role);
        return user;
    }

    private static void authenticateAs(String firebaseUid, String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication(firebaseUid, email, null));
    }

    private static User user(UserRole role) {
        User user = new User("firebase-user", role.name().toLowerCase() + "@edulife.test");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setRole(role);
        return user;
    }

    private static Group group(String name) {
        Group group = new Group(name, USER_ID);
        ReflectionTestUtils.setField(group, "id", GROUP_ID);
        ReflectionTestUtils.setField(group, "createdAt", Instant.parse("2026-06-13T10:00:00Z"));
        return group;
    }

    private static void authenticate(String firebaseUid) {
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication(firebaseUid, "groupadmin@edulife.test", null));
    }
}
