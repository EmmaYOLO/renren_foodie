package com.foodie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodie.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:18
 */
@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
