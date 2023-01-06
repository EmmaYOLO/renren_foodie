package com.foodie;

import com.foodie.entity.Shop;
import com.foodie.service.impl.ShopServiceImpl;
import com.foodie.utils.CacheClient;
import com.foodie.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.foodie.utils.RedisConstants.*;

@SpringBootTest
class RenrenFoodieApplicationTests {
    @Resource
    private CacheClient cacheClient;

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private ExecutorService es = Executors.newFixedThreadPool(500);


    @Test
    public void testIdWorker() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {

            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };

        long start = System.currentTimeMillis();

        es.submit(task);
        latch.await();

        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - start));

    }








//    @Test
//    void testIdWorker() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(300);
//
//        Runnable task = () -> {
//            for (int i = 0; i < 100; i++) {
//                long id = redisIdWorker.nextId("order");
//                System.out.println("id = " + id);
//            }
//            latch.countDown();
//        };
//
//        long begin = System.currentTimeMillis();
//        for (int i = 0; i < 300; i++) {
//            es.submit(task);
//        }
//        latch.await();
//        long end = System.currentTimeMillis();
//        System.out.println("time = " + (end - begin));
//
//    }

    @Test
    void contextLoads() {
    }

    @Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShop2Redis(1L, 10L);
    }

    @Test
    void loadShopData(){
        //现在是要把数据库中Shop的坐标、id取出来，存到Redis里
        //1. 查询店铺信息
        List<Shop> shopList = shopService.list();
        //2. 给店铺信息按照店铺类型（typeId）分组
        Map<Long, List<Shop>> map = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //3. 分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //3.1 获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            //3.2 获取同类型店铺集合
            List<Shop> shops = entry.getValue();
            //3.3 写入Redis GEOADD key 经度 纬度 member
            ArrayList<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(shops.size());
            for (Shop shop : shops) {
//                stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), String.valueOf(shop.getId()));
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        String.valueOf(shop.getId()),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

}
