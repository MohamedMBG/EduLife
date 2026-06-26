package com.edulife.profiles.dto;

/** Response returned after a successful avatar upload, containing the new public URL. */
public record AvatarUploadResponse(String avatarUrl) {}
