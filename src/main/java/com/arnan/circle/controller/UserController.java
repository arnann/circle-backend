package com.arnan.circle.controller;

import com.arnan.circle.common.BaseResponse;
import com.arnan.circle.common.ErrorCode;
import com.arnan.circle.common.ResultUtils;
import com.arnan.circle.exception.BusinessException;
import com.arnan.circle.model.domain.User;
import com.arnan.circle.model.request.UserLoginRequest;
import com.arnan.circle.model.request.UserRegisterRequest;
import com.arnan.circle.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.arnan.circle.common.Constant.*;

@Slf4j
@RestController
@RequestMapping("user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }


    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @DeleteMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User safetyUser = userService.getSafetyUser(userService.getById(currentUser.getId()));
        return ResultUtils.success(safetyUser);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    @GetMapping("/recommend")
    public BaseResponse<IPage<User>> searchUsers(@RequestParam("pageNum") Integer pageNum,
                                                 @RequestParam("pageSize") Integer pageSize,
                                                 HttpServletRequest request) {
        if (pageNum == null || pageSize == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        User currentUser = userService.getCurrentUser(request);
        String key = String.format(USER_RECOMMEND_KEY, currentUser.getId());
        IPage<User> userPage = (IPage<User>) valueOperations.get(key);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        IPage<User> page = userService.page(new Page<>(pageNum, pageSize), null)
                .convert(user -> userService.getSafetyUser(user));
        try {
            valueOperations.set(key, page, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("recommend set key error", e);
        }
        return ResultUtils.success(page);
    }

    @GetMapping("/match/{n}")
    public BaseResponse<List<User>> matchTopN(@PathVariable("n") Integer n,
                                           HttpServletRequest request){
        if (n == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        List<User> userList = userService.matchTopN(n, request);
        return ResultUtils.success(userList);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @GetMapping("/get/tags")
    public BaseResponse<List<User>> getByTagNameList(@RequestParam List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        List<User> users = userService.getUsersByTagName(tagNameList);
        return ResultUtils.success(users);
    }

    @PutMapping
    public BaseResponse updateById(@RequestBody User user, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(user)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        User currentUser = userService.getCurrentUser(request);
        String key = String.format(USER_RECOMMEND_KEY, currentUser.getId());
        IPage<User> userPage = (IPage<User>) valueOperations.get(key);
        if (userPage != null) {
            redisTemplate.delete(key);
        }
        if (!userService.updateById(user)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);

        }
        return ResultUtils.success(null);
    }

    private boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

}
