package com.arnan.circle.controller;

import com.arnan.circle.common.BaseResponse;
import com.arnan.circle.common.ResultUtils;
import com.arnan.circle.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("file")
public class FileController {
    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    public BaseResponse<String> upload(@RequestParam("file") MultipartFile multipartFile) {
        String fileUrl = fileService.upload(multipartFile);
        return ResultUtils.success(fileUrl);
    }

    @GetMapping("/{filename}")
    public BaseResponse getFile(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {
        fileService.getFile(filename, response);
        return ResultUtils.success(null);
    }
}
