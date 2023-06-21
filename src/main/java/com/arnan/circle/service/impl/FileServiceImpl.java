package com.arnan.circle.service.impl;

import com.arnan.circle.common.ErrorCode;
import com.arnan.circle.exception.BusinessException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arnan.circle.model.domain.FileInfo;
import com.arnan.circle.service.FileService;
import com.arnan.circle.mapper.FileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author Arnan
 * @description 针对表【file(文件表)】的数据库操作Service实现
 * @createDate 2023-06-14 22:29:19
 */
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileInfo>
        implements FileService {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.urlPrefix}")
    private String urlPrefix;

    @Override
    public String upload(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        String originalFilename = multipartFile.getOriginalFilename();
        long size = multipartFile.getSize() / 1024; // kb
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString();
        String pathname = uploadPath + File.separator + uuid + "-" + originalFilename;
        File file = new File(pathname);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败!");
        }
        String url = urlPrefix + uuid + "-" + originalFilename;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setType(suffix.substring(1));
        fileInfo.setSize(size);
        fileInfo.setUrl(url);
        boolean save = save(fileInfo);
        if (!save) {
            file.delete();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
        return url;
    }

    @Override
    public void getFile(String filename, HttpServletResponse response) throws IOException {
        Path path = Paths.get(uploadPath  + filename);
        response.setContentType("application/json;charset=utf-8");
//        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
        ServletOutputStream os = response.getOutputStream();
        byte[] bytes = Files.readAllBytes(path);
        os.write(bytes);
        os.flush();
        os.close();
    }
}




