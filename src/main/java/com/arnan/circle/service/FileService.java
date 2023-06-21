package com.arnan.circle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.arnan.circle.model.domain.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
* @author Arnan
* @description 针对表【file(文件表)】的数据库操作Service
* @createDate 2023-06-14 22:29:19
*/
public interface FileService extends IService<FileInfo> {
    String upload(MultipartFile multipartFile);

    void getFile(String filename, HttpServletResponse response) throws IOException;

}
