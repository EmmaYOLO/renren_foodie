package com.foodie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.entity.VoucherOrder;
import com.foodie.mapper.VoucherOrderMapper;
import com.foodie.service.IVoucherOrderService;
import org.springframework.stereotype.Service;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:38
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
}
