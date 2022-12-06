package com.foodie.dto;

import com.foodie.entity.User;
import lombok.Data;

/**
 * @author Emma_Lyy
 * @create 2022-11-30 22:18
 */
@Data
public class UserDTO{
    private Long id;
    private String nickName;
    private String icon;
}
