package com.foodie.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.foodie.utils.RegexPatterns;
import com.foodie.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.foodie.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * @author Emma_Lyy
 * @create 2022-12-01 0:37
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 正则校验手机号格式
        if(RegexUtils.isPhoneInvalid(phone)){
            //2. 如果不符合，返回错误信息
            return Result.fail("请输入正确的手机号");
        }
        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4. 保存验证码到session
        session.setAttribute("code",code);
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
        Object codeInSession = session.getAttribute("code");

        //3. 不一致，报错
        if(codeInSession == null || !codeInSession.equals(code)){
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

        //7. 将user存到session里
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();

    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
