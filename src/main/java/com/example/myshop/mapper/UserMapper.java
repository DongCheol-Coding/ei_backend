package com.example.myshop.mapper;

import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserDto.Request dto);

    @Mapping(source = "roles", target = "roles")
    UserDto.Response toResponse(User user);
}
