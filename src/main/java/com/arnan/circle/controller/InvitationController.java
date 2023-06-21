package com.arnan.circle.controller;

import com.arnan.circle.common.BaseResponse;
import com.arnan.circle.common.ErrorCode;
import com.arnan.circle.common.ResultUtils;
import com.arnan.circle.exception.BusinessException;
import com.arnan.circle.model.domain.Invitation;
import com.arnan.circle.model.domain.User;
import com.arnan.circle.model.request.InviteRequest;
import com.arnan.circle.model.vo.CircleVO;
import com.arnan.circle.service.InvitationService;
import com.arnan.circle.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("invitation")
public class InvitationController {
    @Resource
    private InvitationService invitationService;
    @Resource
    private UserService userService;

    @PostMapping
    public BaseResponse<Invitation> invite(@RequestBody InviteRequest inviteRequest) {
        if (inviteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Invitation invitation = invitationService.invite(inviteRequest);

        return ResultUtils.success(invitation);
    }

    @GetMapping("my")
    public BaseResponse<List<CircleVO>> getMyInvitationList(HttpServletRequest request){
        List<CircleVO> circleVOList = invitationService.getMyInvitationList(request);
        return ResultUtils.success(circleVOList);
    }

    @DeleteMapping("{circleId}")
    public BaseResponse<Boolean> ignore(@PathVariable("circleId") long circleId, HttpServletRequest request) {
        if (circleId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "圈子不存在");
        }
        User currentUser = userService.getCurrentUser(request);
        boolean removed = invitationService.remove(new QueryWrapper<Invitation>()
                .eq("circleId", circleId)
                .eq("userId", currentUser.getId()));
        if (!removed){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "忽略异常");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("{circleId}")
    public BaseResponse<Boolean> agree(@PathVariable("circleId") long circleId ,HttpServletRequest request){
        if (circleId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        invitationService.agree(circleId, request);
        return ResultUtils.success(true);
    }

}
