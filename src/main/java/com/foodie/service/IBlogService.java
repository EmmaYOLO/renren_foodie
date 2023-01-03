package com.foodie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodie.dto.Result;
import com.foodie.entity.Blog;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:22
 */
public interface IBlogService extends IService<Blog> {
    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);
}
