package com.foodie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.entity.UserInfo;
import com.foodie.mapper.UserInfoMapper;
import com.foodie.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:37
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
}
