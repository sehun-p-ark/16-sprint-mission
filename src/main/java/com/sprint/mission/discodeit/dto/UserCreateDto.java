package com.sprint.mission.discodeit.dto;

public record UserCreateDto(
    String username,
    int age,
    String email,
    String password,
    BinaryContentDto profile
) {
}
