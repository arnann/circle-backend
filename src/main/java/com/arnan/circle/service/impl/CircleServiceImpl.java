package com.arnan.circle.service.impl;

import com.arnan.circle.common.ErrorCode;
import com.arnan.circle.enums.CircleStatusEnum;
import com.arnan.circle.exception.BusinessException;
import com.arnan.circle.model.domain.User;
import com.arnan.circle.model.domain.UserCircle;
import com.arnan.circle.model.request.CircleCreateRequest;
import com.arnan.circle.model.request.CircleJoinRequest;
import com.arnan.circle.model.request.CircleSearchRequest;
import com.arnan.circle.model.request.CircleUpdateRequest;
import com.arnan.circle.model.vo.CircleVO;
import com.arnan.circle.service.UserService;
import com.arnan.circle.service.UserCircleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arnan.circle.model.domain.Circle;
import com.arnan.circle.service.CircleService;
import com.arnan.circle.mapper.CircleMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author Arnan
 * @description 针对表【circle(圈子)】的数据库操作Service实现
 * @createDate 2023-06-06 17:41:05
 */
@Service
public class CircleServiceImpl extends ServiceImpl<CircleMapper, Circle>
        implements CircleService {
    @Resource
    private UserService userService;

    @Resource
    private UserCircleService userCircleService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long createCircle(CircleCreateRequest CircleCreateRequest, HttpServletRequest request) {
        //1. 请求参数是否为空？
        if (CircleCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 是否登录，未登录不允许创建
        User currentUser = userService.getCurrentUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //3. 校验信息
        //   1. 圈子人数 > 1 且 <= 20
        if (CircleCreateRequest.getMaxNum() <= 1 || CircleCreateRequest.getMaxNum() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子人数应大于1且小于20");
        }
        //   2. 圈子标题 <= 20
        if (CircleCreateRequest.getName().isEmpty() || CircleCreateRequest.getName().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子名称字数应小于20");
        }
        //   3. 描述 <= 512
        if (!CircleCreateRequest.getDescription().isEmpty() && CircleCreateRequest.getDescription().length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子描述字数应小于512");
        }
        //   4. status 是否公开（int）不传默认为 0（公开）
        Integer status = Optional.ofNullable(CircleCreateRequest.getStatus()).orElse(0);
        CircleStatusEnum circleStatusEnum = CircleStatusEnum.getEnumByValue(status);
        if (circleStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子状态不满足要求");
        }
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        if (circleStatusEnum.equals(CircleStatusEnum.SECRET) && CircleCreateRequest.getPassword().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码应小于32");
        }
        //   6. 超时时间 > 当前时间
        Date expireTime = CircleCreateRequest.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        //   7. 校验用户最多创建 5 个圈子
        long circleCount = count(new QueryWrapper<Circle>().eq("userId", currentUser.getId()));
        if (circleCount >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建5个圈子");
        }
        //4. 插入圈子信息到圈子表
        Circle circle = new Circle();
        circle.setUserId(currentUser.getId());
        BeanUtils.copyProperties(CircleCreateRequest, circle);
        circle.setId(null);
        if (!save(circle) || circle.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建圈子失败");
        }
        //5. 插入用户  => 圈子关系到关系表
        UserCircle userCircle = new UserCircle();
        userCircle.setCircleId(circle.getId());
        userCircle.setUserId(currentUser.getId());
        if (!userCircleService.save(userCircle)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建圈子失败");
        }
        return circle.getId();
    }

    @Override
    public List<CircleVO> getAll(CircleSearchRequest circleSearchRequest, HttpServletRequest request) {
        QueryWrapper<Circle> queryWrapper = new QueryWrapper();
        // 从请求参数中取出圈子名称等查询条件，如果存在则作为查询条件
        Long id = circleSearchRequest.getId();
        if (id != null && id > 0) {
            queryWrapper.eq("id", id);
        }
        String name = circleSearchRequest.getName();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }
        // 可以通过某个关键词同时对名称和描述查询
        String searchText = circleSearchRequest.getSearchText();
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
        }
        String description = circleSearchRequest.getDescription();
        if (StringUtils.isNotBlank(description)) {
            queryWrapper.like("description", description);
        }
        // 只有管理员才能查看私有的房间
        Integer status = circleSearchRequest.getStatus();
        CircleStatusEnum statusEnum = CircleStatusEnum.getEnumByValue(status);
        if (statusEnum != null && statusEnum.equals(CircleStatusEnum.PRIVATE)) {
            if (!userService.isAdmin(request)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        }
        if (statusEnum != null && (statusEnum.equals(CircleStatusEnum.PUBLIC)
                || statusEnum.equals(CircleStatusEnum.SECRET))) {
            queryWrapper.eq("status", statusEnum.getValue());
        }
        Integer maxNum = circleSearchRequest.getMaxNum();
        if (maxNum != null && maxNum > 1) {
            queryWrapper.eq("maxNum", maxNum);
        }
        // 查询
        List<Circle> circleList = list(queryWrapper);
        if (circleList == null) {
            return new ArrayList<>();
        }
        // 处理
        List<CircleVO> circleVOList = new ArrayList<>();
        User currentUser = userService.getCurrentUser(request);
        circleList.forEach(circle -> {
            // 不展示已过期的圈子（根据过期时间筛选）
            Date expireTime = circle.getExpireTime();
            if (expireTime != null && new Date().after(expireTime)) {
                return;
            }
            // 查询创建者
            Long userId = circle.getUserId();
            User createUser = userService.getById(userId);
            CircleVO circleVO = getCircleVO(currentUser, circle, createUser);
            circleVOList.add(circleVO);
        });
        return circleVOList;
    }

    @Override
    public boolean updateById(CircleUpdateRequest circleUpdateRequest, HttpServletRequest request) {
        //1. 校验参数
        if (circleUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long circleId = circleUpdateRequest.getId();
        if (circleId == null || circleId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = circleUpdateRequest.getName();
        if (name != null && name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子标题字数 > 20");
        }
        String description = circleUpdateRequest.getDescription();
        if (description != null && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子标题字数应 < 512 ");
        }
        Integer maxNum = circleUpdateRequest.getMaxNum();
        if (maxNum != null && (maxNum > 20 || maxNum <= 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子人数字数应 <= 20 且 > 1");
        }
        String password = circleUpdateRequest.getPassword();
        if (password != null && password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度应小于32");
        }
        //2. 公开和私有不能有密码，加密要有密码
        Integer status = circleUpdateRequest.getStatus();
        CircleStatusEnum statusEnum = CircleStatusEnum.getEnumByValue(status);
        if ((CircleStatusEnum.PUBLIC.equals(statusEnum) || CircleStatusEnum.PRIVATE.equals(statusEnum))
                && circleUpdateRequest.getPassword() != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子设置公开或者私有时不能设置密码");
        }
        if (CircleStatusEnum.SECRET.equals(statusEnum) && circleUpdateRequest.getPassword() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子设置加密时必须设置密码");
        }
        //3. 查询圈子是否存在
        QueryWrapper<Circle> queryWrapper = new QueryWrapper<>();
        Circle circle = getOne(queryWrapper.eq("id", circleId));
        if (circle == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        //4. 只有管理员或者圈子的创建者可以修改
        User currentUser = userService.getCurrentUser(request);
        if (!userService.isAdmin(request) && !circle.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        BeanUtils.copyProperties(circleUpdateRequest, circle);
        if (!updateById(circle)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return true;
    }

    @Override
    public boolean joinCircle(CircleJoinRequest circleJoinRequest, HttpServletRequest request) {
        if (circleJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //1. 用户最多加入 5 个圈子
        User currentUser = userService.getCurrentUser(request);
        Long userId = currentUser.getId();
        long hasJoinCount = userCircleService.count(new QueryWrapper<UserCircle>().eq("userId", userId));
        if (hasJoinCount >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多加入5个圈子");
        }
        //2. 圈子必须存在，只能加入未满、未过期的圈子
        Long circleId = circleJoinRequest.getCircleId();
        if (circleId == null || circleId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        Circle circle = getOne(new QueryWrapper<Circle>().eq("id", circleId));
        if (circle == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        Date expireTime = circle.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子已过期");
        }
        long circleUserCount = userCircleService.count(new QueryWrapper<UserCircle>().eq("circleId", circleId));
        if (circleUserCount >= circle.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子已满员");
        }
        //3. 不能加入自己的圈子，不能重复加入已加入的圈子（幂等性）
        long count = userCircleService.count(new QueryWrapper<UserCircle>()
                .eq("userId", userId).eq("circleId", circleId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入");
        }
        //4. 禁止加入私有的圈子
        Integer status = circle.getStatus();
        CircleStatusEnum statusEnum = CircleStatusEnum.getEnumByValue(status);
        if (CircleStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有圈子");
        }
        //5. 如果加入的圈子是加密的，必须密码匹配才可以
        if (CircleStatusEnum.SECRET.equals(statusEnum)) {
            String password = circleJoinRequest.getPassword();
            if (password == null || !password.equals(circle.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        //6. 新增圈子 - 用户关联信息
        UserCircle ut = new UserCircle();
        ut.setUserId(userId);
        ut.setCircleId(circleId);
        ut.setJoinTime(new Date());
        boolean save = userCircleService.save(ut);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean quit(long id, HttpServletRequest request) {
        //1. 校验请求参数
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 校验圈子是否存在
        Circle circle = getOne(new QueryWrapper<Circle>().eq("id", id));
        if (circle == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        //3. 校验我是否已加入圈子
        User currentUser = userService.getCurrentUser(request);
        long count = userCircleService.count(new QueryWrapper<UserCircle>()
                .eq("userId", currentUser.getId())
                .eq("circleId", id));
        if (count <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入圈子不能退出圈子");
        }
        //4. 如果圈子只剩一人，圈子解散
        long circleUserCount = userCircleService.count(new QueryWrapper<UserCircle>().eq("circleId", id));
        if (circleUserCount == 1) {
            boolean circleRemove = removeById(id);
            boolean userCircleRemove = userCircleService.remove(new QueryWrapper<UserCircle>().eq("circleId", id));
            if (!circleRemove || !userCircleRemove) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
            }
            return true;
        }
        //5. 还有其他人
        //(1). 如果是队长退出圈子，权限转移给第二早加入的用户 —— 先来后到
        if (circle.getUserId().equals(currentUser.getId())) {
            List<UserCircle> userCircleList = userCircleService.list(new QueryWrapper<UserCircle>()
                    .eq("circleId", id).last("order by id asc limit 2"));
            Circle updateCircle = new Circle();
            updateCircle.setId(id);
            updateCircle.setUserId(userCircleList.get(1).getUserId());
            boolean updatedCircle = updateById(updateCircle);
            boolean userCircleRemove = userCircleService.remove(new QueryWrapper<UserCircle>()
                    .eq("userId", currentUser.getId())
                    .eq("circleId", id));
            if (!updatedCircle || !userCircleRemove) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
            }
            return true;
        } else {
            //(2). 非队长，自己退出圈子
            boolean userCircleRemove = userCircleService.remove(new QueryWrapper<UserCircle>()
                    .eq("userId", currentUser.getId())
                    .eq("circleId", id));
            if (!userCircleRemove) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
            }
        }
        return true;
    }

    @Override
    public boolean disband(long id, HttpServletRequest request) {
        //1. 校验请求参数
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 校验圈子是否存在
        Circle circle = getOne(new QueryWrapper<Circle>().eq("id", id));
        if (circle == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        //3. 校验是不是圈子的队长
        User currentUser = userService.getCurrentUser(request);
        if (!circle.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "非队长不能解散圈子");
        }
        //4. 移除所有加入圈子的关联信息
        boolean removeUserCircle = userCircleService.remove(new QueryWrapper<UserCircle>()
                .eq("circleId", id));
        //5. 删除圈子
        boolean removeCircle = removeById(id);
        if (!removeUserCircle || !removeCircle) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解散失败");
        }
        return true;
    }

    @Override
    public List<CircleVO> getMyJoinList(String name, HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        Long userId = currentUser.getId();
        List<UserCircle> userCircleList = userCircleService.list(new QueryWrapper<UserCircle>().eq("userId", userId));
        if (CollectionUtils.isEmpty(userCircleList)) {
            return null;
        }
        List<CircleVO> circleVOList = new ArrayList<>();
        userCircleList.forEach(userCircle -> {
            Circle circle = getById(userCircle.getCircleId());
            if (circle != null && name != null && !circle.getName().contains(name)) {
                return;
            }
            // 根据圈子查出创建者
            User createUser = userService.getById(circle.getUserId());
            CircleVO circleVO = getCircleVO(currentUser, circle, createUser);
            circleVOList.add(circleVO);
        });
        return circleVOList;
    }

    @Override
    public List<CircleVO> getMyCreateList(String name, HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        Long userId = currentUser.getId();
        List<CircleVO> circleVOList = new ArrayList<>();
        List<Circle> circleList = list(new QueryWrapper<Circle>().eq("userId", userId));
        circleList.forEach(circle -> {
            if (circle != null && name != null && !circle.getName().contains(name)) {
                return;
            }
            CircleVO circleVO = getCircleVO(currentUser, circle, currentUser);
            circleVOList.add(circleVO);
        });
        return circleVOList;
    }

    @Override
    public CircleVO getCircleVO(User currentUser, Circle circle, User createUser) {
        CircleVO circleVO = new CircleVO();
        BeanUtils.copyProperties(circle, circleVO);
        circleVO.setCreateUser(userService.getSafetyUser(createUser));
        // 关联查询圈子成员
        List<Long> userIdList = userCircleService
                .list(new QueryWrapper<UserCircle>().eq("circleId", circle.getId()))
                .stream().map(UserCircle::getUserId)
                .collect(Collectors.toList());
        List<User> memberList = userService.listByIds(userIdList);
        circleVO.setMemberList(memberList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList()));
        if (userIdList.contains(currentUser.getId())) {
            circleVO.setHasJoin(true);
        }
        return circleVO;
    }
}




