package com.edulife.groups.service;

import com.edulife.courses.repository.CourseRepository;
import com.edulife.groups.dto.AddMemberRequest;
import com.edulife.groups.dto.AttachCourseRequest;
import com.edulife.groups.dto.CreateGroupRequest;
import com.edulife.groups.dto.GroupCourseDto;
import com.edulife.groups.dto.GroupDto;
import com.edulife.groups.dto.GroupMemberDto;
import com.edulife.groups.entity.Group;
import com.edulife.groups.entity.GroupCourse;
import com.edulife.groups.entity.GroupMember;
import com.edulife.groups.repository.GroupCourseRepository;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.groups.repository.GroupRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupCourseRepository groupCourseRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupCourseRepository groupCourseRepository,
            UserRepository userRepository,
            CourseRepository courseRepository
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupCourseRepository = groupCourseRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public GroupDto createGroup(CreateGroupRequest request) {
        User currentUser = resolveCurrentUser();
        Group saved = groupRepository.save(new Group(request.name(), currentUser.getId()));
        return toDto(saved);
    }

    @Transactional
    public GroupMemberDto addMember(UUID groupId, AddMemberRequest request) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);

        if (!userRepository.existsById(request.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), request.userId())) {
            // 409 keeps add-member idempotent from a learner-already-in-cohort perspective.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already in group");
        }

        GroupMember saved = groupMemberRepository.save(new GroupMember(group.getId(), request.userId()));
        return new GroupMemberDto(saved.getGroupId(), saved.getUserId(), saved.getAddedAt());
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);

        long deleted = groupMemberRepository.deleteByGroupIdAndUserId(group.getId(), userId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in group");
        }
    }

    @Transactional
    public GroupCourseDto attachCourse(UUID groupId, AttachCourseRequest request) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);

        if (!courseRepository.existsById(request.courseId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        if (groupCourseRepository.existsByGroupIdAndCourseId(group.getId(), request.courseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course already attached to group");
        }

        GroupCourse saved = groupCourseRepository.save(new GroupCourse(group.getId(), request.courseId()));
        return new GroupCourseDto(saved.getGroupId(), saved.getCourseId(), saved.getAttachedAt());
    }

    private Group loadGroupForManagement(UUID groupId, User currentUser) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        boolean isPlatformAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isCreator = group.getCreatedBy().equals(currentUser.getId());
        if (!isPlatformAdmin && !isCreator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the group owner");
        }
        return group;
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication required");
        }
        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found. Call /auth/sync first."));
    }

    private GroupDto toDto(Group group) {
        return new GroupDto(group.getId(), group.getName(), group.getCreatedBy(), group.getCreatedAt());
    }
}
