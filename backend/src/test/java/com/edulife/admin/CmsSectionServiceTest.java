package com.edulife.admin;

import com.edulife.admin.service.CmsCourseAccessGuard;
import com.edulife.admin.service.CmsSectionService;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/** P2: CMS section read endpoints must enforce the same ownership scope as mutations. */
@ExtendWith(MockitoExtension.class)
class CmsSectionServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID COURSE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock private CourseRepository courseRepository;
    @Mock private CourseSectionRepository sectionRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupMemberRepository groupMemberRepository;

    private CmsSectionService service;

    @BeforeEach
    void setUp() {
        service = new CmsSectionService(courseRepository, sectionRepository, userRepository,
                new CmsCourseAccessGuard(groupMemberRepository));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonOwnerTeacherCannotListSections() {
        setUpAuth("other-uid", OTHER_USER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> service.listSections(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void ownerCanListSections() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(COURSE_ID)).willReturn(List.of());

        assertThat(service.listSections(COURSE_ID)).isEmpty();
    }

    @Test
    void adminCanListSections() {
        setUpAuth("admin-uid", OTHER_USER_ID, UserRole.ADMIN);
        givenCourseOwnedBy(OWNER_ID);
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(COURSE_ID)).willReturn(List.of());

        assertThat(service.listSections(COURSE_ID)).isEmpty();
    }

    private void setUpAuth(String firebaseUid, UUID userId, UserRole role) {
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication(firebaseUid, "user@edulife.test", null));
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        given(user.getRole()).willReturn(role);
        given(userRepository.findByFirebaseUid(firebaseUid)).willReturn(Optional.of(user));
    }

    private void givenCourseOwnedBy(UUID ownerId) {
        Course course = mock(Course.class);
        lenient().when(course.getCreatedByUserId()).thenReturn(ownerId);
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
    }
}
