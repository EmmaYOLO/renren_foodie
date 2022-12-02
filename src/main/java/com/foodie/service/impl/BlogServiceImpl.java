package com.foodie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.entity.Blog;
import com.foodie.mapper.BlogMapper;
import com.foodie.service.IBlogService;
import org.springframework.stereotype.Service;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:34
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
}
