package com.sprint.mission.discodeit.dto;

import java.util.UUID;

public record UserUpdateDto(
        UUID id,
        String username,
        Integer age,
        String email,
        String password,
        BinaryContentDto profile
) {
}
