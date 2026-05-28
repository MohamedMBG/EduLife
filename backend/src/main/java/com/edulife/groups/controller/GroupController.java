package com.edulife.groups.controller;

import com.edulife.groups.dto.AddMemberRequest;
import com.edulife.groups.dto.AttachCourseRequest;
import com.edulife.groups.dto.CreateGroupRequest;
import com.edulife.groups.dto.GroupCourseDto;
import com.edulife.groups.dto.GroupDto;
import com.edulife.groups.dto.GroupMemberDto;
import com.edulife.groups.service.GroupService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
// All group management endpoints sit behind TEACHER/ADMIN so LEARNERs cannot create or mutate cohorts.
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupDto> create(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(request));
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
