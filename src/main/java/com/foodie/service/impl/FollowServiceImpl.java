package com.foodie.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.Result;
import com.foodie.dto.UserDTO;
import com.foodie.entity.Follow;
import com.foodie.mapper.FollowMapper;
import com.foodie.service.IFollowService;
import com.foodie.service.IUserService;
import com.foodie.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:36
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

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
            boolean isSuccess = save(follow);
            //如果关注成功，就要在Redis的set中加入follow信息
            if(isSuccess){
                stringRedisTemplate.opsForSet().add("follow:" + userId, String.valueOf(followUserId));
            }
            return Result.ok("关注成功");
        }else{
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            //如果取关成功，Redis中也要删除
            if(isSuccess){
                stringRedisTemplate.opsForSet().remove("follow:" + userId, String.valueOf(followUserId));
            }
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

    @Override
    public Result followCommons(Long id) {
        //1. 获取当前用户id
        Long userId = UserHolder.getUser().getId();
        String key = "follow:" + userId;

        //2. 求交集
        String key2 = "follow:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);

        if(intersect == null || intersect.isEmpty()){
            // 无交集
            return Result.ok(Collections.emptyList());
        }

        //3. 解析id集合
        List<Long> userIds = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        //4. 查询用户
        List<UserDTO> userDTOS = userService.listByIds(userIds)
                .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }
}
