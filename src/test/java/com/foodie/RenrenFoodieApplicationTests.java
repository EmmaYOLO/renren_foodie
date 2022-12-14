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
        //???????????????????????????Shop????????????id??????????????????Redis???
        //1. ??????????????????
        List<Shop> shopList = shopService.list();
        //2. ????????????????????????????????????typeId?????????
        Map<Long, List<Shop>> map = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //3. ??????????????????Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //3.1 ????????????id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            //3.2 ???????????????????????????
            List<Shop> shops = entry.getValue();
            //3.3 ??????Redis GEOADD key ?????? ?????? member
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

    @Test
    void testHyperLogLog(){
        String[] values = new String[1000];

        int j = 0;
        for (int i = 0; i < 1000000; i++) {
            j = i % 1000;
            values[j] = "user_" + i;
            if(j == 999){
                //?????????Redis
                stringRedisTemplate.opsForHyperLogLog().add("h12", values);
            }

        }

        Long count = stringRedisTemplate.opsForHyperLogLog().size("h12");
        System.out.println("count = " + count);


    }

}
