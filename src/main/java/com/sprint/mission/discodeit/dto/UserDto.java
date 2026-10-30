package com.sprint.mission.discodeit.dto;

public record UserDto(
        String username,
        int age,
        String email,
        boolean online
) {
}
