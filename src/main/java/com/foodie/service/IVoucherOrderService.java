package com.foodie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodie.dto.Result;
import com.foodie.entity.VoucherOrder;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:24
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);
}
