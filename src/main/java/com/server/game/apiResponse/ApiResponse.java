package com.server.game.apiResponse;

import java.time.ZonedDateTime;
import java.time.ZoneId;

import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    int status;
    String message;
    T data;
    ZonedDateTime timestamp;

    public ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}


