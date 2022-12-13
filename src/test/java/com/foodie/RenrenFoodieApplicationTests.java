package com.foodie;

import com.foodie.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RenrenFoodieApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Test
    void contextLoads() {
    }

    @Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShop2Redis(1L, 10L);
    }

}
