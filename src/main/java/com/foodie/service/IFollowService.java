package com.foodie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodie.dto.Result;
import com.foodie.entity.Follow;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:22
 */
public interface IFollowService extends IService<Follow> {
    Result follow(Long followUserId, Boolean ifFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
