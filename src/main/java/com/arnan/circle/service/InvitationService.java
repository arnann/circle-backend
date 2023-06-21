package com.arnan.circle.service;

import com.arnan.circle.model.domain.Invitation;
import com.arnan.circle.model.request.InviteRequest;
import com.arnan.circle.model.vo.CircleVO;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Arnan
* @description 针对表【invitation(邀请表)】的数据库操作Service
* @createDate 2023-06-15 16:08:03
*/
public interface InvitationService extends IService<Invitation> {

    Invitation invite(InviteRequest inviteRequest);

    List<CircleVO> getMyInvitationList(HttpServletRequest request);

    boolean agree(long circleId, HttpServletRequest request);
}
