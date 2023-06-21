package com.arnan.circle.controller;

import com.arnan.circle.common.BaseResponse;
import com.arnan.circle.common.ErrorCode;
import com.arnan.circle.common.ResultUtils;
import com.arnan.circle.exception.BusinessException;
import com.arnan.circle.model.domain.Circle;
import com.arnan.circle.model.request.*;
import com.arnan.circle.model.vo.CircleVO;
import com.arnan.circle.service.CircleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("circle")
public class CircleController {

    @Resource
    private CircleService circleService;


    @PostMapping("create")
    public BaseResponse<Long> create(@RequestBody CircleCreateRequest CircleCreateRequest, HttpServletRequest request) {
        if (CircleCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long circleId = circleService.createCircle(CircleCreateRequest, request);
        return ResultUtils.success(circleId);
    }

    @DeleteMapping("/del/{id}")
    public BaseResponse delById(@PathVariable("id") long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!circleService.removeById(id)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(null);
    }

    @PutMapping
    public BaseResponse updateById(@RequestBody CircleUpdateRequest circleUpdateRequest, HttpServletRequest request) {
        if (circleUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (circleUpdateRequest.getId() == null || circleUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!circleService.updateById(circleUpdateRequest, request)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(null);
    }

    @GetMapping("{id}")
    public BaseResponse<Circle> getById(@PathVariable("id") long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Circle circle = circleService.getById(id);
        if (circle == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(circle);
    }

    @GetMapping("list")
    public BaseResponse<List<CircleVO>> getAll(CircleSearchRequest circleSearchRequest,
                                             HttpServletRequest request) {
        List<CircleVO> circleVOList = circleService.getAll(circleSearchRequest, request);
        return ResultUtils.success(circleVOList);
    }

    @GetMapping("/list/page")
    public BaseResponse<IPage<CircleVO>> getListByPage(CirclePageQuery circlePageQuery, HttpServletRequest request) {
        Page<CircleVO> page = new Page<>(circlePageQuery.getPageNum(), circlePageQuery.getPageSize());
        CircleSearchRequest circleSearchRequest = new CircleSearchRequest();
        BeanUtils.copyProperties(circlePageQuery,circleSearchRequest);
        page.setRecords(circleService.getAll(circleSearchRequest, request));
        return ResultUtils.success(page);
    }

    @GetMapping("/my/join")
    public BaseResponse<List<CircleVO>> getMyJoinList(String name, HttpServletRequest request) {
        List<CircleVO> circleVOList = (circleService.getMyJoinList(name, request));
        return ResultUtils.success(circleVOList);
    }

    @GetMapping("/my/create")
    public BaseResponse<List<CircleVO>> getMyCreateList(String name, HttpServletRequest request) {
        List<CircleVO> circleVOList = (circleService.getMyCreateList(name, request));
        return ResultUtils.success(circleVOList);
    }

    @PostMapping("/join")
    public BaseResponse joinCircle(@RequestBody CircleJoinRequest circleJoinRequest, HttpServletRequest request){
        if (circleJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        circleService.joinCircle(circleJoinRequest, request);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/quit/{id}")
    public BaseResponse quit(@PathVariable("id") long id, HttpServletRequest request){
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(circleService.quit(id, request));
    }

    @DeleteMapping("/disband/{id}")
    public BaseResponse disband(@PathVariable("id") long id, HttpServletRequest request){
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(circleService.disband(id, request));
    }
}
