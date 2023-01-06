package com.foodie.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.Result;
import com.foodie.entity.Shop;
import com.foodie.mapper.ShopMapper;
import com.foodie.service.IShopService;
import com.foodie.utils.CacheClient;
import com.foodie.utils.RedisConstants;
import com.foodie.utils.RedisData;
import com.foodie.utils.SystemConstants;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.foodie.utils.RedisConstants.*;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:37
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, LOCK_SHOP_KEY, this::getById, 10L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("商铺信息不存在！");
        }

        return Result.ok(shop);
    }

/*    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //逻辑过期解决缓存击穿
    private Shop queryWithLogicalExpire(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //1. 从Redis查询店铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        //2. 判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            //3. 不存在，直接返回null
            return null;
        }

        //4. 命中，需要先把JSON反序列化为对象
        RedisData shopRedisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) shopRedisData.getData(), Shop.class);
        LocalDateTime expireTime = shopRedisData.getExpireTime();

        //5. 判断是否过期
        if (LocalDateTime.now().isBefore(expireTime)) {
            //5.1. 未过期，直接返回店铺信息
            return shop;
        }
        //5.2. 已过期，需要缓存重建
        //6. 缓存重建
        //6.1. 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        //6.2. 判断是否获取锁成功
        if (isLock) {
            //6.3. 成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存
                    this.saveShop2Redis(id, 20L);//这里过期时间设置得短，是为了做测试
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        //6.4. 返回过期的商铺信息
        return shop;

    }

    //缓存穿透
    public Shop queryWithPassThrough(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;

        //1.从Redis查询shopJson
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //2.判断Redis中是否存在shopJson
        if (StrUtil.isNotBlank(shopJson)) {
            //3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //4.判断Redis中的shopJson是否为空值(防止缓存穿透)
        //不等于null，说明为空值""
        if (shopJson != null) {
            return null;
        }

        //5.根据id查数据库
        Shop shop = getById(id);
        //6.数据库没有
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //7.数据库有
        stringRedisTemplate.opsForValue().set(shopKey, shopJson, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;

    }

    //互斥锁解决缓存击穿
    private Shop queryWithMutex(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //1. 从Redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        //2. 判断是否为非空字符串
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //3. 判断是否为""(缓存穿透)
        if (shopJson != null) {
            return null;
        }

        //4. 缓存重建
        //4.1 尝试获取互斥锁
        String lockShopKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockShopKey);
            //4.2 获取互斥锁失败
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            //4.3 获取互斥锁成功，再次检测Redis
            String shopJson1 = stringRedisTemplate.opsForValue().get(shopKey);
            if (StrUtil.isNotBlank(shopJson1)) {
                return JSONUtil.toBean(shopJson1, Shop.class);
            }

            //4.4 访问数据库查询
            shop = getById(id);

            //模拟重建的延时
            Thread.sleep(200);
            //4.5 数据库不存在，返回null
            if (shop == null) {//缓存穿透
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            //4.6 数据库存在shop信息
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //5.释放锁
            unlock(lockShopKey);
        }

        //6. 返回
        return shop;

    }*/

    @Override
    @Transactional
    public Result update(Shop shop) {

        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }

        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);

        return Result.ok();
    }



    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1. 判断是否传入坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，直接查数据库
            Page<Shop> page = query().eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        //2. 计算分页数据
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        //3. 查询Redis
        String key = SHOP_GEO_KEY + typeId;
        //GEOSEARCH key BYLONLAT x y BYRADIUS 5000 WITHDISTANCE
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key,//代表从这个key里存的geo数据中寻找
                        GeoReference.fromCoordinate(x, y),//以这个点为中心
                        new Distance(5000),//寻找距离5000m以内的
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance()//WITHDISTANCE 页面还要显示店铺距离多远，所以需要Distance数据
                                .limit(end)
                );

        //4. 解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            //没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        //4.1 截取 from - end的部分
        ArrayList<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            //4.2 获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));//这里把5公里以内的shopId都收集起来了
            //4.3 获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        //5. 根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        //6. 返回
        //最终要的是，加上了distance数据的shops集合
        return Result.ok(shops);
    }

    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        //1. 查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(1000);

        //2. 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        //3. 写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }


/*    public boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    public void unlock(String key) {
        stringRedisTemplate.delete(key);
    }*/


}
