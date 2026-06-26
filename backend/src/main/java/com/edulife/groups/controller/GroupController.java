package com.edulife.groups.controller;

import com.edulife.groups.dto.AddMemberRequest;
import com.edulife.groups.dto.AttachCourseRequest;
import com.edulife.groups.dto.CreateGroupRequest;
import com.edulife.groups.dto.CreateGroupJoinRequest;
import com.edulife.groups.dto.GroupCourseDto;
import com.edulife.groups.dto.GroupDetailDto;
import com.edulife.groups.dto.GroupDto;
import com.edulife.groups.dto.GroupJoinRequestDto;
import com.edulife.groups.dto.GroupMemberDto;
import com.edulife.groups.dto.GroupSummaryDto;
import com.edulife.groups.dto.ReviewGroupJoinRequest;
import com.edulife.groups.model.GroupJoinRequestStatus;
import com.edulife.groups.service.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for institute/group management: creation, membership, course attachment, and join requests.
 * Restricted to TEACHER, GROUP_ADMIN, and ADMIN roles.
 */
@RestController
@RequestMapping("/api/v1/groups")
// GROUP_ADMIN manages institute groups; TEACHER manages their own cohorts; ADMIN can manage all.
@PreAuthorize("hasAnyRole('TEACHER','GROUP_ADMIN','ADMIN')")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /** GET /api/v1/groups — groups owned by the caller (ADMIN sees all). */
    @GetMapping
    public List<GroupSummaryDto> listMyGroups() {
        return groupService.listMyGroups();
    }

    /** GET /api/v1/groups/join-requests/mine - teacher's own institute requests. */
    @GetMapping("/join-requests/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public List<GroupJoinRequestDto> listMyJoinRequests() {
        return groupService.listMyJoinRequests();
    }

    /** GET /api/v1/groups/{groupId} — members and attached courses; owner or ADMIN only. */
    @GetMapping("/{groupId}")
    public GroupDetailDto getGroupDetail(@PathVariable UUID groupId) {
        return groupService.getGroupDetail(groupId);
    }

    @PostMapping
    public ResponseEntity<GroupDto> create(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(request));
    }

    /**
     * Teachers request institute membership here; approval later makes them visible in that
     * group admin's course review queue.
     */
    @PostMapping("/{groupId}/join-requests")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GroupJoinRequestDto> submitJoinRequest(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateGroupJoinRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.submitJoinRequest(groupId, request));
    }

    /** Group owners/admins review teachers asking to join an institute. */
    @GetMapping("/{groupId}/join-requests")
    public List<GroupJoinRequestDto> listGroupJoinRequests(
            @PathVariable UUID groupId,
            @RequestParam(required = false) GroupJoinRequestStatus status
    ) {
        return groupService.listGroupJoinRequests(groupId, status);
    }

    @PutMapping("/{groupId}/join-requests/{requestId}/approve")
    public ResponseEntity<GroupJoinRequestDto> approveJoinRequest(
            @PathVariable UUID groupId,
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(groupService.approveJoinRequest(groupId, requestId));
    }

    @PutMapping("/{groupId}/join-requests/{requestId}/reject")
    public ResponseEntity<GroupJoinRequestDto> rejectJoinRequest(
            @PathVariable UUID groupId,
            @PathVariable UUID requestId,
            @Valid @RequestBody ReviewGroupJoinRequest request
    ) {
        return ResponseEntity.ok(groupService.rejectJoinRequest(groupId, requestId, request));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberDto> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.addMember(groupId, request));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId
    ) {
        groupService.removeMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/courses")
    public ResponseEntity<GroupCourseDto> attachCourse(
            @PathVariable UUID groupId,
            @Valid @RequestBody AttachCourseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.attachCourse(groupId, request));
    }
}
