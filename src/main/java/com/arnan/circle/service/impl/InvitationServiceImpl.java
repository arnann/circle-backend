package com.arnan.circle.service.impl;

import com.arnan.circle.common.ErrorCode;
import com.arnan.circle.common.ResultUtils;
import com.arnan.circle.exception.BusinessException;
import com.arnan.circle.model.domain.Circle;
import com.arnan.circle.model.domain.User;
import com.arnan.circle.model.domain.UserCircle;
import com.arnan.circle.model.request.InviteRequest;
import com.arnan.circle.model.vo.CircleVO;
import com.arnan.circle.service.CircleService;
import com.arnan.circle.service.UserService;
import com.arnan.circle.service.UserCircleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arnan.circle.model.domain.Invitation;
import com.arnan.circle.service.InvitationService;
import com.arnan.circle.mapper.InvitationMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Arnan
 * @description 针对表【invitation(邀请表)】的数据库操作Service实现
 * @createDate 2023-06-15 16:08:03
 */
@Service
public class InvitationServiceImpl extends ServiceImpl<InvitationMapper, Invitation>
        implements InvitationService {

    @Resource
    private UserCircleService userCircleService;

    @Resource
    private CircleService circleService;

    @Resource
    private UserService userService;

    @Override
    public Invitation invite(InviteRequest inviteRequest) {
        if (inviteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = inviteRequest.getUserId();
        Long circleId = inviteRequest.getCircleId();
        if (userId == null || circleId == null || userId <= 0 || circleId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 已在圈子中不能邀请
        UserCircle userCircle = userCircleService.getOne(new QueryWrapper<UserCircle>().eq("userId", userId)
                .eq("circleId", circleId));
        if (userCircle != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已经圈子中");
        }
        // 查询圈子是否已满
        long count = userCircleService.count(new QueryWrapper<UserCircle>().eq("circleId", circleId));
        Circle circle = circleService.getOne(new QueryWrapper<Circle>().eq("id", circleId));
        if (circle.getMaxNum() == count) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该圈子已满");
        }
        Invitation invitation = new Invitation();
        BeanUtils.copyProperties(inviteRequest, invitation);
        if (!save(invitation)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邀请失败");
        }
        return invitation;
    }

    @Override
    public List<CircleVO> getMyInvitationList(HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        List<Invitation> invitationList = list(new QueryWrapper<Invitation>().eq("userId", currentUser.getId()));
        if (CollectionUtils.isEmpty(invitationList)) {
            return null;
        }
        // 关联查询所有圈子
        List<CircleVO> circleVOList = new ArrayList<>();
        invitationList.forEach(invitation -> {
            Long circleId = invitation.getCircleId();
            Circle circle = circleService.getOne(new QueryWrapper<Circle>().eq("id", circleId));
            User createUser = userService.getOne(new QueryWrapper<User>().eq("id", circle.getUserId()));
            CircleVO circleVO = circleService.getCircleVO(currentUser, circle, createUser);
            circleVOList.add(circleVO);
        });
        return circleVOList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean agree(long circleId, HttpServletRequest request) {
        if (circleId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        //1. 用户最多加入 5 个圈子
        User currentUser = userService.getCurrentUser(request);
        Long userId = currentUser.getId();
        long hasJoinCount = userCircleService.count(new QueryWrapper<UserCircle>().eq("userId", userId));
        if (hasJoinCount >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多加入5个圈子");
        }
        //2. 只能加入未满的圈子
        Circle circle = circleService.getOne(new QueryWrapper<Circle>().eq("id", circleId));
        if (circle == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        long circleUserCount = userCircleService.count(new QueryWrapper<UserCircle>().eq("circleId", circleId));
        if (circleUserCount >= circle.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子已满员");
        }
        //3. 不能重复加入已加入的圈子
        long count = userCircleService.count(new QueryWrapper<UserCircle>()
                .eq("userId", userId).eq("circleId", circleId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入");
        }
        //4. 新增圈子 - 用户关联信息
        UserCircle ut = new UserCircle();
        ut.setUserId(userId);
        ut.setCircleId(circleId);
        ut.setJoinTime(new Date());
        boolean save = userCircleService.save(ut);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入失败");
        }
        // 删除邀请
        boolean removed = remove(new QueryWrapper<Invitation>().eq("circleId", circleId));
        if (!removed){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入失败");
        }
        return true;
    }
}




