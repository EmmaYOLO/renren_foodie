package com.foodie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.Result;
import com.foodie.entity.Follow;
import com.foodie.mapper.FollowMapper;
import com.foodie.service.IFollowService;
import com.foodie.utils.UserHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:36
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Override
    public Result follow(Long followUserId, Boolean ifFollow) {
        //1. 获取当前用户id
        Long userId = UserHolder.getUser().getId();

        //2. 判断是要关注还是取关
        if(ifFollow){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            follow.setCreateTime(LocalDateTime.now());
            save(follow);
            return Result.ok("关注成功");
        }else{
            remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            return Result.ok("已取关");
        }
    }

    @Override
    public Result isFollow(Long followUserId) {
        //1.获取用户id
        Long userId = UserHolder.getUser().getId();
        //2.查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        int count = count(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", followUserId));
        //3.判断
        return Result.ok(count > 0);
    }
}
