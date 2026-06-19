package com.edulife.admin.service;

import com.edulife.courses.entity.Course;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Centralised authorization for CMS read access to a course's authoring data (exam with
 * correct-answer flags, sections, lessons). Having one rule here prevents the per-service
 * read paths from drifting and re-introducing the cross-tenant disclosure that the audit found.
 *
 * <p>Read access is granted to:
 * <ul>
 *   <li>platform {@code ADMIN};</li>
 *   <li>the {@code TEACHER} who authored the course;</li>
 *   <li>a {@code GROUP_ADMIN} only for courses authored by a teacher inside one of their groups.</li>
 * </ul>
 * Everyone else — including a non-owner teacher and an unrelated group admin — gets 403. The
 * mutation paths keep their stricter admin/owner rule; this read rule is a superset that also
 * lets a group admin review (but not edit) the courses they are responsible for publishing.</p>
 */
@Component
public class CmsCourseAccessGuard {

    private final GroupMemberRepository groupMemberRepository;

    public CmsCourseAccessGuard(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    /** Throws 403 unless the caller may read the given course's CMS authoring data. */
    public void requireReadAccess(User user, Course course) {
        if (!hasReadAccess(user, course)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the course owner");
        }
    }

    private boolean hasReadAccess(User user, Course course) {
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }
        UUID authorId = course.getCreatedByUserId();
        if (authorId == null) {
            return false;
        }
        if (authorId.equals(user.getId())) {
            return true;
        }
        return user.getRole() == UserRole.GROUP_ADMIN
                && groupMemberRepository.existsMemberManagedBy(user.getId(), authorId);
    }
}
