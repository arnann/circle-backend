package com.arnan.circle.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.arnan.circle.common.ErrorCode;
import com.arnan.circle.exception.BusinessException;
import com.arnan.circle.model.domain.User;
import com.arnan.circle.service.UserService;
import com.arnan.circle.util.AlgorithmUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arnan.circle.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.arnan.circle.common.Constant.*;

/**
 * @author Arnan
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-05-11 20:09:04
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;


    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "arnan";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 账户不能包含特殊字符
        String invalidPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(invalidPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码不同");
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号小于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于8位");
        }
        // 账户不能包含特殊字符
        String invalidPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(invalidPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserState(originUser.getUserState());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签名查询用户
     *
     * @param tagNameList 标签名列表
     * @return 脱敏后的用户
     */
    @Override
    public List<User> getUsersByTagName(List<String> tagNameList) {
        if (tagNameList == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        return list(queryWrapper).stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public User getCurrentUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (attribute == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) attribute;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) attribute;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public List<User> matchTopN(Integer n, HttpServletRequest request) {
        if (n == null || n <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = getCurrentUser(request);
        // 如果当前用户没有标签，直接返回前topN条
        String currentUserTags = currentUser.getTags();
        List<String> currentUserTagList = JSON.parseObject(currentUserTags, new TypeReference<List<String>>() {
        });
        if (StringUtils.isBlank(currentUserTags) || StringUtils.isEmpty(currentUserTags) || CollectionUtils.isEmpty(currentUserTagList)) {
            List<User> list = list(new QueryWrapper<User>().last("limit " + n));
            List<User> safeUsers = list.stream().map(this::getSafetyUser).collect(Collectors.toList());
            return safeUsers;
        }
        // 查询所有用户
        List<User> allUser = list();
        // 使用排序树(分数 -> 用户列表)
        SortedMap<Integer, List<User>> scoreUserSortedMap = new TreeMap<>();
        for (User user : allUser) {
            // 排除自己
            if (user.getId().equals(currentUser.getId())) {
                continue;
            }
            // 排除空标签用户
            User safetyUser = getSafetyUser(user);
            String userTags = safetyUser.getTags();
            List<String> tagList = JSON.parseObject(userTags, new TypeReference<List<String>>() {
            });
            if (StringUtils.isBlank(userTags)
                    || StringUtils.isEmpty(userTags)
                    || CollectionUtils.isEmpty(tagList)) {
                continue;
            }
            // 求最小距离
            int minDistance = AlgorithmUtil.minDistance(currentUserTagList, tagList);
            // 将map中的所有用户查出(合并流)，判断是否已满，如果已满
            List<User> scoreUserList = scoreUserSortedMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            if (scoreUserList.size() >= n) {
                Integer highestScore = scoreUserSortedMap.lastKey();
                // 如果分数小于则替换最大分数用户
                if (minDistance < highestScore) {
                    scoreUserSortedMap.compute(highestScore, (score, userList) -> {
                        // 分数对应的用户只有一个时，删除分数map
                        if (userList.size() == 1) {
                            // 表示删除键和值
                            return null;
                        } else {
                            // 对应多个时，删除该分数中的一个用户
                            userList.remove(0);
                            return userList;
                        }
                    });
                    addScoreUser(scoreUserSortedMap, safetyUser, minDistance);
                    scoreUserSortedMap.values().forEach(System.out::println);
                }
            } else {
                // 未满
                addScoreUser(scoreUserSortedMap, safetyUser, minDistance);
            }
        }
        ;
        return scoreUserSortedMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * 排序树添加用户
     *
     * @param scoreUserSortedMap 排序树
     * @param safetyUser         用户
     * @param minDistance        最小距离
     */
    private static void addScoreUser(SortedMap<Integer, List<User>> scoreUserSortedMap, User safetyUser, int minDistance) {
        scoreUserSortedMap.computeIfPresent(minDistance, (k, v) -> {
            v.add(safetyUser);
            return v;
        });
        scoreUserSortedMap.computeIfAbsent(minDistance,
                (k) -> scoreUserSortedMap.put(minDistance, new ArrayList<>(Arrays.asList(safetyUser))));
    }

}





