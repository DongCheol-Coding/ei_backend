package com.example.myshop.mapper;

import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // 회원가입 요청 → 엔티티
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "creatAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    User toEntity(UserDto.Request dto);

    // 사용자 응답용 DTO
    @Mapping(source = "roles", target = "roles")
    UserDto.Response toResponse(User user);
}
