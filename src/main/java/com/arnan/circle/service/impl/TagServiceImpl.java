package com.arnan.circle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arnan.circle.model.domain.Tag;
import com.arnan.circle.service.TagService;
import com.arnan.circle.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author Arnan
* @description 针对表【tag(标签表)】的数据库操作Service实现
* @createDate 2023-05-31 17:04:33
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




