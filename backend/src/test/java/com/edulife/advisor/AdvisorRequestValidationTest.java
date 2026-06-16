package com.edulife.advisor;

import com.edulife.advisor.dto.AdvisorRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisorRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void rejectsNullGoal() {
        Set<ConstraintViolation<AdvisorRequest>> violations = validator.validate(new AdvisorRequest(null));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsBlankGoal() {
        Set<ConstraintViolation<AdvisorRequest>> violations = validator.validate(new AdvisorRequest("   "));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsEmptyGoal() {
        Set<ConstraintViolation<AdvisorRequest>> violations = validator.validate(new AdvisorRequest(""));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsGoalExceedingMaxLength() {
        String tooLong = "a".repeat(501);
        Set<ConstraintViolation<AdvisorRequest>> violations = validator.validate(new AdvisorRequest(tooLong));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void acceptsValidGoal() {
        Set<ConstraintViolation<AdvisorRequest>> violations = validator.validate(
                new AdvisorRequest("I want to become a software developer"));
        assertThat(violations).isEmpty();
    }

    @Test
    void acceptsGoalAtMaxLength() {
        String maxLength = "a".repeat(500);
        Set<ConstraintViolation<AdvisorRequest>> violations = validator.validate(new AdvisorRequest(maxLength));
        assertThat(violations).isEmpty();
    }
}
