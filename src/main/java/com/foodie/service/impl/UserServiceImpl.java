package com.foodie.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodie.dto.LoginFormDTO;
import com.foodie.dto.Result;
import com.foodie.dto.UserDTO;
import com.foodie.entity.User;
import com.foodie.mapper.UserMapper;
import com.foodie.service.IUserService;
import com.foodie.utils.RedisConstants;
import com.foodie.utils.RegexPatterns;
import com.foodie.utils.RegexUtils;
import com.foodie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.foodie.utils.RedisConstants.*;
import static com.foodie.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:37
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 正则校验手机号格式
        if(RegexUtils.isPhoneInvalid(phone)){
            //2. 如果不符合，返回错误信息
            return Result.fail("请输入正确的手机号");
        }
        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4. 保存验证码到Redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5. 发送验证码
        log.debug("发送短信验证码成功，验证码：{}", code);
        //6. 返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. 校验手机号
        //这里必须重新校验手机号，因为有可能发送验证码时手机号对着，这里又填错了
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号输入错误");
        };

        //2. 校验验证码
        String code = loginForm.getCode();
        String codeInRedis = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        //3. 不一致，报错
        if(codeInRedis == null || !codeInRedis.equals(code)){
            return Result.fail("验证码错误");
        }

        //4. 一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
//        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        userLambdaQueryWrapper.eq(User::getPhone, loginForm.getPhone());
//        User user = this.getOne(userLambdaQueryWrapper);

        //5. 判断用户是否存在
        if(user == null){
            //6. 不存在，创建用户
            user = createUserWithPhone(phone);//这里的赋值保证了，user一定有值。下面才可以把user存到session里。
        }

//        this.save(user);

        //7. 将user存到Redis里
        //7.1 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
        //7.2 将User对象转换为UserDto的map存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //7.3 存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userDTOMap);
        //7.4 设置token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
        /*
        上面返回的token，前端进行了处理，每次发送axios请求时，请求头都会带上，名为"authorization"
         */

    }

    @Override
    public Result sign() {
        //1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2. 获取日期
        LocalDateTime now = LocalDateTime.now();

        //3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        //4. 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();

        //5. 写入Redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);

        return Result.ok();
    }

    @Override
    public Result signCount() {
        //1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2. 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        //3. 拼接Redis的key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        //4. 获取dayOfMonth
        int dayOfMonth = now.getDayOfMonth();
        //5. 从Redis中获取本月截止今天的签到记录，返回的是一个十进制数字 BITFIELD sign:1:202301 GET u6 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));

        if(result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == null || num == 0){
            return Result.ok(0);
        }

        //6. 签到统计
        int count = 0;
        while (true){
            if((num & 1) == 1){
                count++;
            }else{
                break;
            }
            num >>>= 1;
        }

        return Result.ok(count);


    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
