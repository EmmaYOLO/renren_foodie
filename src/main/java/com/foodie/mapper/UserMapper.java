package com.foodie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodie.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:18
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
