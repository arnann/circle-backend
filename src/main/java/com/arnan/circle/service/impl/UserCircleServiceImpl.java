package com.arnan.circle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arnan.circle.model.domain.UserCircle;
import com.arnan.circle.service.UserCircleService;
import com.arnan.circle.mapper.UserCircleMapper;
import org.springframework.stereotype.Service;

/**
* @author Arnan
* @description 针对表【user_circle(用户圈子关系表)】的数据库操作Service实现
* @createDate 2023-06-06 17:41:13
*/
@Service
public class UserCircleServiceImpl extends ServiceImpl<UserCircleMapper, UserCircle>
    implements UserCircleService{

}




