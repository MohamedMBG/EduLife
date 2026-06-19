package com.edulife.admin;

import com.edulife.admin.service.CmsCourseAccessGuard;
import com.edulife.admin.service.CmsLessonService;
import com.edulife.courses.entity.Course;
import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.LessonRepository;
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

/** P2: CMS lesson read endpoints must enforce ownership through the section → course chain. */
@ExtendWith(MockitoExtension.class)
class CmsLessonServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID COURSE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECTION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock private LessonRepository lessonRepository;
    @Mock private CourseSectionRepository sectionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupMemberRepository groupMemberRepository;

    private CmsLessonService service;

    @BeforeEach
    void setUp() {
        service = new CmsLessonService(lessonRepository, sectionRepository, courseRepository, userRepository,
                new CmsCourseAccessGuard(groupMemberRepository));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonOwnerTeacherCannotListLessons() {
        setUpAuth("other-uid", OTHER_USER_ID, UserRole.TEACHER);
        givenSectionInCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> service.listLessons(SECTION_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void ownerCanListLessons() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenSectionInCourseOwnedBy(OWNER_ID);
        given(lessonRepository.findAllByCourseSectionIdOrderByDisplayOrderAsc(SECTION_ID)).willReturn(List.of());

        assertThat(service.listLessons(SECTION_ID)).isEmpty();
    }

    @Test
    void adminCanListLessons() {
        setUpAuth("admin-uid", OTHER_USER_ID, UserRole.ADMIN);
        givenSectionInCourseOwnedBy(OWNER_ID);
        given(lessonRepository.findAllByCourseSectionIdOrderByDisplayOrderAsc(SECTION_ID)).willReturn(List.of());

        assertThat(service.listLessons(SECTION_ID)).isEmpty();
    }

    private void setUpAuth(String firebaseUid, UUID userId, UserRole role) {
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication(firebaseUid, "user@edulife.test", null));
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        given(user.getRole()).willReturn(role);
        given(userRepository.findByFirebaseUid(firebaseUid)).willReturn(Optional.of(user));
    }

    private void givenSectionInCourseOwnedBy(UUID ownerId) {
        CourseSection section = mock(CourseSection.class);
        given(section.getCourseId()).willReturn(COURSE_ID);
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));

        Course course = mock(Course.class);
        lenient().when(course.getCreatedByUserId()).thenReturn(ownerId);
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
    }
}
