package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.channel.ChannelDto;
import com.sprint.mission.discodeit.dto.channel.ChannelUpdateDto;
import com.sprint.mission.discodeit.dto.channel.PrivateChannelCreateDto;
import com.sprint.mission.discodeit.dto.channel.PublicChannelCreateDto;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.validator.CommonValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BasicChannelService implements ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final CommonValidator validator;
    private final ReadStatusRepository readStatusRepository;

    @Override
    public ChannelDto createPublicChannel(PublicChannelCreateDto dto) {
        validator.validateNotNull(
                dto,
                "채널 생성 정보는 null일 수 없습니다."
        );
        validator.validateNotBlank(
                dto.name(),
                "채널 이름은 비어있을 수 없습니다."
        );
        validator.validateNotNull(
                dto.description(),
                "채널 설명은 비어있을 수 없습니다."
        )

        Channel channel = new Channel(
                ChannelType.PUBLIC,
                dto.name(),
                dto.description()
        );

        Channel savedChannel = channelRepository.save(channel);

        return toDto(savedChannel);
    }

    @Override
    public ChannelDto createPrivateChannel(PrivateChannelCreateDto dto) {
        validator.validateNotNull(
                dto,
                "채널 생성 정보는 null일 수 없습니다."
        );
        validator.validateNotNull(
                dto.userIds(),
                "참여 사용자 정보는 null일 수 없습니다."
        );

        for (UUID userId : dto.userIds()) {
            userRepository.findById(userId)
                    .orElseThrow(() ->
                            new NoSuchElementException(
                                    "사용자가 존재하지 않습니다."
                            )
                    );
        }

        Channel channel = new Channel(
                ChannelType.PRIVATE,
                null,
                null
        );

        Channel savedChannel = channelRepository.save(channel);

        for (UUID userId : dto.userIds()) {
            ReadStatus readStatus = new ReadStatus(
                    userId,
                    savedChannel.getId(),
                    Instant.now()
            );

            readStatusRepository.save(readStatus);
        }

        return toDto(savedChannel);
    }

    @Override
    public ChannelDto findById(UUID id) {
        validator.validateNotNull(
                id,
                "채널 ID는 null일 수 없습니다."
        );

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "채널이 존재하지 않습니다."
                        )
                );
        return toDto(channel);
    }

    @Override
    public List<ChannelDto> findAll() {
        return channelRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<ChannelDto> findAllByUserId(UUID userId) {
        validator.validateNotNull(
                userId,
                "사용자 ID는 null일 수 없습니다."
        );

        userRepository.findById(userId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "사용자가 존재하지 않습니다."
                        )
                );

        return channelRepository.findAllByUserId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ChannelDto updateChannel(ChannelUpdateDto dto) {
        validator.validateNotNull(
                dto,
                "수정 정보는 null일 수 없습니다."
        );
        validator.validateNotNull(
                dto.id(),
                "채널 ID는 null일 수 없습니다."
        );
        Channel channel = channelRepository.findById(dto.id())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "채널이 존재하지 않습니다."
                        )
                );
        if (channel.getType() == ChannelType.PRIVATE) {
            throw new IllegalStateException(
                    "PRIVATE 채널은 수정할 수 없습니다."
            );
        }

        if (dto.name() != null) {
            validator.validateNotBlank(
                    dto.name(),
                    "채널 이름은 비어있을 수 없습니다."
            );
        }

        if (dto.description() != null) {
            validator.validateNotBlank(
                    dto.description(),
                    "채널 설명은 비어있을 수 없습니다."
            );
        }

        channel.update(dto.name(), dto.description());
        Channel savedChannel = channelRepository.save(channel);
        return toDto(savedChannel);
    }

    @Override
    public void deleteChannel(UUID id) {
        validator.validateNotNull(
                id,
                "채널 ID는 null일 수 없습니다."
        );
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "채널이 존재하지 않습니다."
                        )
                );
        // readStatus 삭제
        readStatusRepository.findAllByChannelId(id)
                .forEach(readStatusRepository::delete);

        //todo message와 연결된 binaryContent 삭제 구현하기

        // message 삭제
        messageRepository.findAllByChannelId(id)
                .forEach(messageRepository::delete);
        // 채널 삭제
       channelRepository.delete(channel);
    }

    private ChannelDto toDto(Channel channel) {
       Instant lastMessageAt =
                messageRepository.findAllByChannelId(channel.getId())
                        .stream()
                        .map(Message::getCreatedAt)
                        .max(Instant::compareTo)
                        .orElse(null);

       List<UUID> userIds = List.of();
       if (channel.getType() == ChannelType.PRIVATE) {
           userIds =
               readStatusRepository.findAllByChannelId(channel.getId())
                       .stream()
                       .map(ReadStatus::getUserId)
                       .toList();
       }

       return new ChannelDto(
               channel.getId(),
               channel.getType(),
               channel.getName(),
               channel.getDescription(),
               lastMessageAt,
               userIds
       );
    }
}
