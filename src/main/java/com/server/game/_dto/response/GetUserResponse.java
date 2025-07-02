package com.server.game._dto.response;

import lombok.Data;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetUserResponse {
    String id;
    String name;
    String email;
    String role;
}
