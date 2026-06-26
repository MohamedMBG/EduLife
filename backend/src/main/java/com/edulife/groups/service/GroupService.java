package com.edulife.groups.service;

import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.groups.dto.AddMemberRequest;
import com.edulife.groups.dto.AttachCourseRequest;
import com.edulife.groups.dto.CreateGroupRequest;
import com.edulife.groups.dto.CreateGroupJoinRequest;
import com.edulife.groups.dto.GroupCourseDetailDto;
import com.edulife.groups.dto.GroupCourseDto;
import com.edulife.groups.dto.GroupDetailDto;
import com.edulife.groups.dto.GroupDto;
import com.edulife.groups.dto.GroupJoinRequestDto;
import com.edulife.groups.dto.GroupMemberDetailDto;
import com.edulife.groups.dto.GroupMemberDto;
import com.edulife.groups.dto.GroupSummaryDto;
import com.edulife.groups.dto.ReviewGroupJoinRequest;
import com.edulife.groups.entity.Group;
import com.edulife.groups.entity.GroupCourse;
import com.edulife.groups.entity.GroupJoinRequest;
import com.edulife.groups.entity.GroupMember;
import com.edulife.groups.model.GroupJoinRequestStatus;
import com.edulife.groups.repository.GroupCourseRepository;
import com.edulife.groups.repository.GroupJoinRequestRepository;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.groups.repository.GroupRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Core service for group lifecycle: creation, membership, course attachment, and join-request workflow.
 * Enforces ownership checks so only the group creator (or platform ADMIN) can manage a group.
 */
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupCourseRepository groupCourseRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupCourseRepository groupCourseRepository,
            GroupJoinRequestRepository groupJoinRequestRepository,
            UserRepository userRepository,
            CourseRepository courseRepository
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupCourseRepository = groupCourseRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    /** Creates a new group owned by the authenticated user. */
    @Transactional
    public GroupDto createGroup(CreateGroupRequest request) {
        User currentUser = resolveCurrentUser();
        Group saved = groupRepository.save(new Group(request.name(), currentUser.getId()));
        return toDto(saved);
    }

    /** Lists groups owned by the caller (ADMIN sees all); lazily creates a default group for new GROUP_ADMINs. */
    @Transactional
    public List<GroupSummaryDto> listMyGroups() {
        User currentUser = resolveCurrentUser();
        // A verified institute admin needs a manageable institute record before either web or
        // Android can show group data. Creating the first empty group lazily avoids relying on
        // Flyway seeds that may have run before the Firebase-backed user row existed.
        ensureDefaultGroupForNewGroupAdmin(currentUser);

        List<Group> groups = currentUser.getRole() == UserRole.ADMIN
                ? groupRepository.findAll()
                : groupRepository.findAllByCreatedBy(currentUser.getId());

        return groups.stream()
                .sorted(Comparator.comparing(Group::getCreatedAt))
                .map(group -> new GroupSummaryDto(
                        group.getId(),
                        group.getName(),
                        group.getCreatedAt(),
                        groupMemberRepository.countByGroupId(group.getId()),
                        groupCourseRepository.countByGroupId(group.getId())
                ))
                .toList();
    }

    /** Submits a join request for a teacher to join an institute group; prevents duplicates. */
    @Transactional
    public GroupJoinRequestDto submitJoinRequest(UUID groupId, CreateGroupJoinRequest request) {
        User currentUser = resolveCurrentUser();
        if (currentUser.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher role required");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (group.getCreatedBy().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already own this group");
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already in group");
        }
        if (groupJoinRequestRepository.existsByGroupIdAndRequesterUserIdAndStatus(
                group.getId(), currentUser.getId(), GroupJoinRequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Join request already pending");
        }

        // Joining an institute is reviewer-controlled so a teacher cannot self-attach to
        // a group and route their future course approvals around the platform admin.
        GroupJoinRequest saved = groupJoinRequestRepository.save(
                new GroupJoinRequest(group.getId(), currentUser.getId(), request.motivation()));
        return toJoinRequestDto(saved, group, currentUser, null);
    }

    @Transactional
    public List<GroupJoinRequestDto> listMyJoinRequests() {
        User currentUser = resolveCurrentUser();
        return toJoinRequestDtos(groupJoinRequestRepository
                .findAllByRequesterUserIdOrderByRequestedAtDesc(currentUser.getId()));
    }

    @Transactional
    public List<GroupJoinRequestDto> listGroupJoinRequests(UUID groupId, GroupJoinRequestStatus status) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);
        List<GroupJoinRequest> requests = status == null
                ? groupJoinRequestRepository.findAllByGroupIdOrderByRequestedAtDesc(group.getId())
                : groupJoinRequestRepository.findAllByGroupIdAndStatusOrderByRequestedAtDesc(group.getId(), status);
        return toJoinRequestDtos(requests);
    }

    /** Approves a pending join request, adding the requester as a group member in the same transaction. */
    @Transactional
    public GroupJoinRequestDto approveJoinRequest(UUID groupId, UUID requestId) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);
        GroupJoinRequest request = loadPendingJoinRequest(group.getId(), requestId);

        // Approval and membership creation are transactional: course approval scope must not
        // change unless the request state and group membership are persisted together.
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), request.getRequesterUserId())) {
            groupMemberRepository.save(new GroupMember(group.getId(), request.getRequesterUserId()));
        }
        request.approve(currentUser.getId());
        return toJoinRequestDto(request);
    }

    /** Rejects a pending join request with an optional admin note. */
    @Transactional
    public GroupJoinRequestDto rejectJoinRequest(UUID groupId, UUID requestId, ReviewGroupJoinRequest review) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);
        GroupJoinRequest request = loadPendingJoinRequest(group.getId(), requestId);
        request.reject(currentUser.getId(), review.adminNote());
        return toJoinRequestDto(request);
    }

    /** Returns full group detail including enriched member and course lists; owner or ADMIN only. */
    @Transactional
    public GroupDetailDto getGroupDetail(UUID groupId) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);

        List<GroupMember> members = groupMemberRepository.findAllByGroupId(group.getId());
        Map<UUID, User> usersById = userRepository
                .findAllById(members.stream().map(GroupMember::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<GroupMemberDetailDto> memberDtos = members.stream()
                .sorted(Comparator.comparing(GroupMember::getAddedAt))
                .map(member -> {
                    User user = usersById.get(member.getUserId());
                    return new GroupMemberDetailDto(
                            member.getUserId(),
                            user != null ? user.getEmail() : "(deleted user)",
                            user != null ? user.getRole() : null,
                            member.getAddedAt()
                    );
                })
                .toList();

        List<GroupCourse> attachments = groupCourseRepository.findAllByGroupId(group.getId());
        Map<UUID, Course> coursesById = courseRepository
                .findAllById(attachments.stream().map(GroupCourse::getCourseId).toList())
                .stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        List<GroupCourseDetailDto> courseDtos = attachments.stream()
                .sorted(Comparator.comparing(GroupCourse::getAttachedAt))
                .map(attachment -> {
                    Course course = coursesById.get(attachment.getCourseId());
                    return new GroupCourseDetailDto(
                            attachment.getCourseId(),
                            course != null ? course.getTitle() : "(deleted course)",
                            course != null ? course.getStatus().name() : null,
                            attachment.getAttachedAt()
                    );
                })
                .toList();

        return new GroupDetailDto(group.getId(), group.getName(), group.getCreatedAt(), memberDtos, courseDtos);
    }

    /** Adds a user to a group by internal user ID; rejects add-by-email to prevent account enumeration. */
    @Transactional
    public GroupMemberDto addMember(UUID groupId, AddMemberRequest request) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);

        UUID memberUserId = resolveMemberUserId(request);

        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), memberUserId)) {
            // 409 keeps add-member idempotent from a learner-already-in-cohort perspective.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already in group");
        }

        GroupMember saved = groupMemberRepository.save(new GroupMember(group.getId(), memberUserId));
        return new GroupMemberDto(saved.getGroupId(), saved.getUserId(), saved.getAddedAt());
    }

    private UUID resolveMemberUserId(AddMemberRequest request) {
        boolean hasUserId = request.userId() != null;
        boolean hasEmail = request.email() != null && !request.email().isBlank();

        if (hasUserId == hasEmail) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one of userId or email");
        }

        if (hasUserId) {
            // Internal ids are opaque (not enumerable), so an authorized owner may add a known user.
            if (!userRepository.existsById(request.userId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            return request.userId();
        }

        // Direct add-by-email is intentionally not supported. Branching on whether the email maps to
        // a user would let an authorized owner enumerate registered accounts (existing vs missing)
        // and silently add arbitrary unrelated users without consent. A single generic message is
        // returned regardless of the email, disclosing nothing. Members are added by internal id
        // (authorized + verified) or join through the consent-based join-request flow.
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unable to add a member with the provided details. Add by user id or use a join request.");
    }

    /** Removes a member from a group; throws 404 if the member is not found. */
    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);

        long deleted = groupMemberRepository.deleteByGroupIdAndUserId(group.getId(), userId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in group");
        }
    }

    /** Attaches a course to a group after verifying the course is authored by the owner or a managed teacher. */
    @Transactional
    public GroupCourseDto attachCourse(UUID groupId, AttachCourseRequest request) {
        User currentUser = resolveCurrentUser();
        Group group = loadGroupForManagement(groupId, currentUser);

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        requireCourseAttachableToGroup(currentUser, group, course);

        if (groupCourseRepository.existsByGroupIdAndCourseId(group.getId(), request.courseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course already attached to group");
        }

        GroupCourse saved = groupCourseRepository.save(new GroupCourse(group.getId(), request.courseId()));
        return new GroupCourseDto(saved.getGroupId(), saved.getCourseId(), saved.getAttachedAt());
    }

    /**
     * A group may only contain courses that belong to its authorized scope: courses authored by the
     * group owner, or by a teacher the group manages. Without this an owner could attach an
     * unrelated teacher's course (including unpublished/private ones) and pull its cohort into the
     * group's analytics. Platform admins manage every group, so existing admin rules let them attach
     * any course.
     */
    private void requireCourseAttachableToGroup(User currentUser, Group group, Course course) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return;
        }
        UUID authorId = course.getCreatedByUserId();
        UUID groupOwnerId = group.getCreatedBy();
        boolean authoredByGroupOwner = authorId != null && authorId.equals(groupOwnerId);
        boolean authoredByManagedTeacher = authorId != null
                && groupMemberRepository.existsMemberManagedBy(groupOwnerId, authorId);
        if (!authoredByGroupOwner && !authoredByManagedTeacher) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only attach courses authored by you or by a teacher in your group");
        }
    }

    /** Loads a group and verifies the caller is its owner or a platform ADMIN. */
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

    private void ensureDefaultGroupForNewGroupAdmin(User currentUser) {
        if (currentUser.getRole() != UserRole.GROUP_ADMIN) {
            return;
        }
        if (!groupRepository.findAllByCreatedBy(currentUser.getId()).isEmpty()) {
            return;
        }

        // This is only for a brand-new GROUP_ADMIN account. Teachers must still choose whether
        // to create a group, request an institute, or stay independent for platform-admin review.
        groupRepository.save(new Group("My Institute", currentUser.getId()));
    }

    private GroupJoinRequest loadPendingJoinRequest(UUID groupId, UUID requestId) {
        GroupJoinRequest request = groupJoinRequestRepository.findByIdAndGroupId(requestId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Join request not found"));
        if (request.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Join request already reviewed");
        }
        return request;
    }

    private List<GroupJoinRequestDto> toJoinRequestDtos(List<GroupJoinRequest> requests) {
        Map<UUID, Group> groupsById = groupRepository
                .findAllById(requests.stream().map(GroupJoinRequest::getGroupId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Group::getId, Function.identity()));
        Map<UUID, User> usersById = userRepository
                .findAllById(requests.stream()
                        .flatMap(request -> java.util.stream.Stream.of(
                                request.getRequesterUserId(), request.getReviewedByUserId()))
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return requests.stream()
                .map(request -> toJoinRequestDto(
                        request,
                        groupsById.get(request.getGroupId()),
                        usersById.get(request.getRequesterUserId()),
                        usersById.get(request.getReviewedByUserId())))
                .toList();
    }

    private GroupJoinRequestDto toJoinRequestDto(GroupJoinRequest request) {
        Group group = groupRepository.findById(request.getGroupId()).orElse(null);
        User requester = userRepository.findById(request.getRequesterUserId()).orElse(null);
        User reviewer = request.getReviewedByUserId() == null
                ? null
                : userRepository.findById(request.getReviewedByUserId()).orElse(null);
        return toJoinRequestDto(request, group, requester, reviewer);
    }

    private GroupJoinRequestDto toJoinRequestDto(
            GroupJoinRequest request,
            Group group,
            User requester,
            User reviewer
    ) {
        return new GroupJoinRequestDto(
                request.getId(),
                request.getGroupId(),
                group != null ? group.getName() : "(deleted group)",
                request.getRequesterUserId(),
                requester != null ? requester.getEmail() : "(deleted user)",
                request.getStatus(),
                request.getMotivation(),
                request.getAdminNote(),
                request.getReviewedByUserId(),
                reviewer != null ? reviewer.getEmail() : null,
                request.getRequestedAt(),
                request.getReviewedAt()
        );
    }
}
