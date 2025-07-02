package com.server.game.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.server.game.dto.response.AuthenticationResponse;
import com.server.game.model.User;

@Mapper(componentModel = "spring")
public interface AuthenticationMapper  { 
    @Mapping(target = "token", ignore = true) // Set token later in controller or service
    AuthenticationResponse toAuthenticationResponse(User user, @Context String token);

    @AfterMapping
    default void setToken(@MappingTarget AuthenticationResponse response, @Context String token) {
        response.setToken(token);
    }
}