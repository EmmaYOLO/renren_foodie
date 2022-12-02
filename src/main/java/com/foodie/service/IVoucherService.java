package com.foodie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodie.dto.Result;
import com.foodie.entity.Voucher;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:25
 */
public interface IVoucherService extends IService<Voucher> {
    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
