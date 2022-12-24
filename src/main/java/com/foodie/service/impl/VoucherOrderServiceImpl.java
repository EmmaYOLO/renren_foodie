package com.foodie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.Result;
import com.foodie.entity.SeckillVoucher;
import com.foodie.entity.VoucherOrder;
import com.foodie.mapper.VoucherOrderMapper;
import com.foodie.service.ISeckillVoucherService;
import com.foodie.service.IVoucherOrderService;
import com.foodie.utils.RedisIdWorker;
import com.foodie.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:38
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1. 查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("秒杀券不存在");
        }

        //2. 判断秒杀是否开始
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        if (LocalDateTime.now().isBefore(beginTime)) {
            return Result.fail("秒杀未开始");
        }

        //3. 判断秒杀是否结束
        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (LocalDateTime.now().isAfter(endTime)) {
            return Result.fail("秒杀已结束");
        }

        //4. 判断秒杀券库存
        Integer stock = seckillVoucher.getStock();
        if (stock < 1) {
            return Result.fail("秒杀券已被抢空");
        }

        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //5.一人一单
        Long userId = UserHolder.getUser().getId();
        //5.1 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //5.2 订单数量大于零，说明已经买过这个秒杀券了
        if(count > 0){
            return Result.fail("秒杀券限购一张！");
        }

        //6. 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();

//        seckillVoucher.setStock(stock - 1);
//        boolean success = seckillVoucherService.updateById(seckillVoucher);
        if(!success){
            //扣减库存失败
            return Result.fail("库存不足！");
        }

        //6. 生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //6.2 用户id
        voucherOrder.setUserId(userId);
        //6.3 代金券id
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setCreateTime(LocalDateTime.now());
//        voucherOrder.setPayTime(LocalDateTime.now());
//        voucherOrder.setPayType(1);
        voucherOrder.setStatus(1);
        this.save(voucherOrder);

        //7. 返回订单id
        return Result.ok(orderId);
    }
}
