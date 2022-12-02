package com.foodie.dto;

import lombok.Data;

/**
 * @author Emma_Lyy
 * @create 2022-11-30 22:19
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
