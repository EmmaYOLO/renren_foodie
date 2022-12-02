package com.foodie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.entity.Shop;
import com.foodie.mapper.ShopMapper;
import com.foodie.service.IShopService;
import org.springframework.stereotype.Service;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:37
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
}
