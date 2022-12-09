package com.foodie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodie.dto.Result;
import com.foodie.entity.Shop;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:23
 */
public interface IShopService extends IService<Shop> {
    Result queryById(Long id);

    Result update(Shop shop);
}
