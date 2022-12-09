package com.foodie.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.Result;
import com.foodie.entity.ShopType;
import com.foodie.mapper.ShopTypeMapper;
import com.foodie.service.IShopTypeService;
import com.foodie.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.foodie.utils.RedisConstants.CACHE_SHOPTYPE_KEY;
import static com.foodie.utils.RedisConstants.CACHE_SHOPTYPE_TTL;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:37
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {


        List<String> shopTypeList = stringRedisTemplate.opsForList().range(CACHE_SHOPTYPE_KEY, 0, -1);

        if(shopTypeList != null && shopTypeList.size() != 0){
            return Result.ok(shopTypeList);
        }

        List<ShopType> shopList = list();
        if(shopList == null){
            return Result.fail("无分类数据！");
        }

        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOPTYPE_KEY, String.valueOf(shopList));
        stringRedisTemplate.expire(CACHE_SHOPTYPE_KEY, CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);

        return Result.ok(shopList);
    }
}
