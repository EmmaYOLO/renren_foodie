package com.foodie.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.Result;
import com.foodie.dto.ScrollResult;
import com.foodie.dto.UserDTO;
import com.foodie.entity.Blog;
import com.foodie.entity.Follow;
import com.foodie.entity.User;
import com.foodie.mapper.BlogMapper;
import com.foodie.service.IBlogService;
import com.foodie.service.IFollowService;
import com.foodie.service.IUserService;
import com.foodie.utils.SystemConstants;
import com.foodie.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.foodie.utils.RedisConstants.*;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:34
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Resource
    private IFollowService followService;


    @Override
    public Result queryBlogById(Long id) {
        //1. 查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        //2. 给blog加上User信息
        queryBlogUser(blog);
        //3. 查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if(user == null){
            // 如果用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();//这里取出来的records就是blogs
        // 查询用户
        records.forEach(blog ->{
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });//这一步是把取出来的这些blog加上User的信息，以及是否已经点赞的信息
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        //1. 获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2. 判断用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        //3. 如果未点赞
        if (score == null) {
            //3.1 数据库点赞数+1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            //3.2 当前用户存到Redis的set集合中
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }else{

            //4. 如果已点赞
            //4.1 数据库点赞数-1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            //4.2 当前用户从Redis的set集合中删除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        //1. 查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        //2. 解析出其中的用户id（String转Long）
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);

        //3. 根据用户id查询用户
        List<User> users = userService
                .query().in("id", ids)
                .last("ORDER BY FIELD(id," + idStr +")").list();
        //users变成userDTOs
        List<UserDTO> userDTOS = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());

        //4. 返回UserDTO
        return Result.ok(userDTOS);
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //1. 查当前用户的收件箱
        Long userId = UserHolder.getUser().getId();
        String key = FEED_KEY + userId;

        //2. 查出收件箱里的blogId ZREVRANGEBYSCORE key max min LIMIT offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);

        //3. 非空判断
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }

        //4. 解析数据：blogId、minTime（时间戳）、offset
        List<String> blogIds = new ArrayList<>(typedTuples.size());
        long minTime = 0;

        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            // 4.1 获取id
            blogIds.add(tuple.getValue());
            // 4.2 获取分数（时间戳）minTime
            long time = tuple.getScore().longValue();
            if(minTime == time){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }

        //5. 根据blogId查blog
//        List<Blog> blogs = listByIds(blogIds);//这样查不行，因为blogIds是有序的，这样在数据库查，用的是in，会变成无序的
        String idStr = StrUtil.join(",", blogIds);
        List<Blog> blogs = query().in("id", blogIds)
                .last("ORDER BY FIELD ( id, " + idStr + ")").list();

        for (Blog blog : blogs) {
            //5.1 查询blog有关的用户
            queryBlogUser(blog);
            //5.2 查询blog是否被点赞
            isBlogLiked(blog);
        }


        //6. 封装并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);

        return Result.ok(scrollResult);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 2. 保存探店博文
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("新增笔记失败");
        }
        // 3. 查询笔记作者的粉丝id select user_id from tb_follow where follow_user_id = ?
        List<Follow> follows = followService.list(new QueryWrapper<Follow>()
                .eq("follow_user_id", user.getId()));
        // 4. 给所有粉丝feed
        for (Follow follow : follows) {
            //4.1 获取粉丝id
            Long userId = follow.getUserId();
            //4.2 推送
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet()
                    .add(key, String.valueOf(blog.getId()),
                            System.currentTimeMillis());
        }

        // 返回id
        return Result.ok(blog.getId());
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
