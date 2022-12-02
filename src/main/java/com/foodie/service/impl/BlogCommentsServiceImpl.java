package com.foodie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.entity.BlogComments;
import com.foodie.mapper.BlogCommentsMapper;
import com.foodie.service.IBlogCommentsService;
import org.springframework.stereotype.Service;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:26
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {
}
