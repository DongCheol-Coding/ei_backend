package com.example.ei_backend.mapper;

import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // 회원가입 요청 → 엔티티
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "isSocial", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserDto.Request dto);

    // 사용자 응답용 DTO
    @Mapping(source = "roles", target = "roles")
    UserDto.Response toResponse(User user);


}
