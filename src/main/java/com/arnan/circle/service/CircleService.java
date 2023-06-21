package com.arnan.circle.service;

import com.arnan.circle.model.domain.Circle;
import com.arnan.circle.model.domain.User;
import com.arnan.circle.model.request.CircleCreateRequest;
import com.arnan.circle.model.request.CircleJoinRequest;
import com.arnan.circle.model.request.CircleSearchRequest;
import com.arnan.circle.model.request.CircleUpdateRequest;
import com.arnan.circle.model.vo.CircleVO;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Arnan
* @description 针对表【circle(圈子)】的数据库操作Service
* @createDate 2023-06-06 17:41:05
*/
public interface CircleService extends IService<Circle> {

    long createCircle(CircleCreateRequest CircleCreateRequest, HttpServletRequest request);

    List<CircleVO> getAll(CircleSearchRequest circleSearchRequest, HttpServletRequest request);

    boolean updateById(CircleUpdateRequest circleUpdateRequest, HttpServletRequest request);

    boolean joinCircle(CircleJoinRequest circleJoinRequest, HttpServletRequest request);

    boolean quit(long id, HttpServletRequest request);

    boolean disband(long id, HttpServletRequest request);

    List<CircleVO> getMyJoinList(String name, HttpServletRequest request);

    List<CircleVO> getMyCreateList(String name, HttpServletRequest request);

    CircleVO getCircleVO(User currentUser, Circle circle, User createUser);
}
