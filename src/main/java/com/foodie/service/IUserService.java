package com.foodie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodie.dto.LoginFormDTO;
import com.foodie.dto.Result;
import com.foodie.entity.User;

import javax.servlet.http.HttpSession;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:24
 */
public interface IUserService extends IService<User> {
    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
