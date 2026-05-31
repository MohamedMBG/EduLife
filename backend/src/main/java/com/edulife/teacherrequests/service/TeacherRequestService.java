package com.edulife.teacherrequests.service;

import com.edulife.security.FirebaseAuthentication;
import com.edulife.teacherrequests.dto.ReviewTeacherRequestRequest;
import com.edulife.teacherrequests.dto.SubmitTeacherRequestRequest;
import com.edulife.teacherrequests.dto.TeacherRequestResponse;
import com.edulife.teacherrequests.entity.TeacherRequest;
import com.edulife.teacherrequests.exception.AlreadyTeacherOrAdminException;
import com.edulife.teacherrequests.exception.TeacherRequestAlreadyPendingException;
import com.edulife.teacherrequests.exception.TeacherRequestNotFoundException;
import com.edulife.teacherrequests.exception.TeacherRequestNotPendingException;
import com.edulife.teacherrequests.model.RequestStatus;
import com.edulife.teacherrequests.repository.TeacherRequestRepository;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherRequestService {

    private final TeacherRequestRepository requestRepository;
    private final UserRepository userRepository;

    public TeacherRequestService(
            TeacherRequestRepository requestRepository,
            UserRepository userRepository
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeacherRequestResponse submit(SubmitTeacherRequestRequest body) {
        User user = resolveCurrentUser();

        if (user.getRole() != UserRole.LEARNER) {
            throw new AlreadyTeacherOrAdminException();
        }

        if (requestRepository.existsByUserIdAndStatus(user.getId(), RequestStatus.PENDING)) {
            throw new TeacherRequestAlreadyPendingException();
        }

        TeacherRequest request = requestRepository.save(new TeacherRequest(user, body.motivation()));
        return TeacherRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public Optional<TeacherRequestResponse> getMyLatestRequest() {
        User user = resolveCurrentUser();
        return requestRepository.findFirstByUserIdOrderByRequestedAtDesc(user.getId())
                .map(TeacherRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TeacherRequestResponse> listRequests(RequestStatus statusFilter, Pageable pageable) {
        Page<TeacherRequest> page = (statusFilter != null)
                ? requestRepository.findAllByStatus(statusFilter, pageable)
                : requestRepository.findAll(pageable);
        return page.map(TeacherRequestResponse::from);
    }

    @Transactional
    public TeacherRequestResponse approve(UUID requestId) {
        User admin = resolveCurrentUser();
        TeacherRequest request = findOrThrow(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new TeacherRequestNotPendingException();
        }

        request.approve(admin);
        // Promote the learner atomically within the same transaction.
        request.getUser().setRole(UserRole.TEACHER);

        return TeacherRequestResponse.from(requestRepository.save(request));
    }

    @Transactional
    public TeacherRequestResponse reject(UUID requestId, ReviewTeacherRequestRequest body) {
        User admin = resolveCurrentUser();
        TeacherRequest request = findOrThrow(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new TeacherRequestNotPendingException();
        }

        request.reject(admin, body.adminNote());
        return TeacherRequestResponse.from(requestRepository.save(request));
    }

    private TeacherRequest findOrThrow(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(TeacherRequestNotFoundException::new);
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required.");
        }

        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found. Call /auth/sync first."));
    }
}
