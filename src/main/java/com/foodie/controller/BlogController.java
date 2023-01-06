package com.foodie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodie.dto.Result;
import com.foodie.dto.UserDTO;
import com.foodie.entity.Blog;
import com.foodie.service.IBlogService;
import com.foodie.utils.SystemConstants;
import com.foodie.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

import static com.foodie.utils.SystemConstants.MAX_PAGE_SIZE;

/**
 * @author Emma_Lyy
 * @create 2022-11-30 21:05
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;


    @PostMapping
    public Result saveBlog(@RequestBody Blog blog){
        return blogService.saveBlog(blog);
/*        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        blogService.save(blog);
        // 返回id
        return Result.ok(blog.getId());*/
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id){
        return blogService.queryBlogById(id);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return blogService.queryBlogLikes(id);
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(@RequestParam("id") Long id,
                                    @RequestParam(value = "current", defaultValue = "1") Integer current){

        Page<Blog> page = blogService.query().eq("user_id", id)
                .page(new Page<>(current, MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,
                                    @RequestParam(value = "offset", defaultValue = "0") Integer offset){
        return blogService.queryBlogOfFollow(max, offset);
    }



}
