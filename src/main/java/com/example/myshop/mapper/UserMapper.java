package com.example.myshop.mapper;

import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.domain.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserDto.Request dto);

    UserDto.Response toResponse(User user);
}
