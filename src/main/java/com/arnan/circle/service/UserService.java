package com.arnan.circle.service;

import com.arnan.circle.model.domain.Tag;
import com.arnan.circle.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Arnan
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2023-05-11 20:09:04
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签名模糊查询用户
     *
     * @param tagNameList 标签名列表
     * @return 脱敏后的用户信息
     */
    List<User> getUsersByTagName(List<String> tagNameList);

    User getCurrentUser(HttpServletRequest request);

    boolean isAdmin(HttpServletRequest request);

    List<User> matchTopN(Integer n, HttpServletRequest request);
}
