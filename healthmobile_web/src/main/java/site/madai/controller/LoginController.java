package site.madai.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.JedisPool;
import site.madai.constant.MessageConstant;
import site.madai.constant.RedisMessageConstant;
import site.madai.entity.Result;
import site.madai.pojo.Member;
import site.madai.service.MemberService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Map;

/**
 * @Project: site.madai.controller
 * @Author: ShaoDi Wang
 * @Date: Created in 2019-07-01 14:58
 * @Description:
 * @Version: 1.0
 */
@RestController
@RequestMapping("login")
public class LoginController {
    @Reference
    private MemberService memberService;
    @Autowired
    private JedisPool jedisPool;

    //使用手机号和验证码登录
    @RequestMapping("check")
    public Result check(HttpServletResponse response, @RequestBody Map map) {
        String telephone = (String) map.get("telephone");
        String validateCode = (String) map.get("validateCode");
        //从Redis中获取缓存的验证码
        String codeInRedis = jedisPool.getResource().get(
                telephone + RedisMessageConstant.SENDTYPE_LOGIN);
        if (codeInRedis == null || !codeInRedis.equals(validateCode)) {
            //验证码输入错误
            return new Result(false, MessageConstant.VALIDATECODE_ERROR);
        } else {
            //验证码输入正确
            //判断当前用户是否为会员
            Member member = memberService.findByTelephone(telephone);
            if (member == null) {
                //当前用户不是会员，自动完成注册
                member = new Member();
                member.setPhoneNumber(telephone);
                member.setRegTime(new Date());
                memberService.add(member);
            }
            //登录成功
            //写入Cookie，跟踪用户
            Cookie cookie = new Cookie("login_member_telephone", telephone);
            //路径
            cookie.setPath("/");
            //有效期30天
            cookie.setMaxAge(60 * 60 * 24 * 30);
            response.addCookie(cookie);
            return new Result(true, MessageConstant.LOGIN_SUCCESS);
        }
    }
}
