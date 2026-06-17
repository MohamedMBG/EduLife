package com.edulife.groups;

import com.edulife.courses.repository.CourseRepository;
import com.edulife.groups.dto.GroupSummaryDto;
import com.edulife.groups.entity.Group;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
