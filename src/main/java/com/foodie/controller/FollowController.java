package com.foodie.controller;

import com.foodie.dto.Result;
import com.foodie.service.IFollowService;
import com.foodie.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 1:43
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;


    @PutMapping("/{id}/{isFollow}")
    private Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean ifFollow){
        return followService.follow(followUserId, ifFollow);
    }

    @GetMapping("/or/not/{id}")
    private Result isFollow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);

    }

    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable("id") Long id){
        return followService.followCommons(id);



    }




}
