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
    // @Mapping(target = "token", expression = "java(setToken(token))") // Set token later below
    // AuthenticationResponse toAuthenticationResponse(String token);

    // @AfterMapping
    // default void setToken(@MappingTarget AuthenticationResponse response, String token) {
    //     response.setToken(token);
    // }
}