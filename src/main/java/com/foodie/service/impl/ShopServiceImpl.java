package com.foodie.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.Result;
import com.foodie.entity.Shop;
import com.foodie.mapper.ShopMapper;
import com.foodie.service.IShopService;
import com.foodie.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.foodie.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.foodie.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:37
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryById(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;

        //1.从Redis查询shopJson
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        //2.判断Redis中是否存在shopJson
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }

        //4.没有，根据id查数据库
        Shop shop = getById(id);
        //5.数据库没有
        if(shop == null){
            return Result.fail("店铺不存在");
        }

        //6.存在，写入Redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {

        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }

        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);

        return Result.ok();
    }
}
