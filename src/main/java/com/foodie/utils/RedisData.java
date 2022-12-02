package com.foodie.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 1:40
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;

}
