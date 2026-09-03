package cn.mario.aiplatform.controller;


import cn.mario.aiplatform.exception.BizException;
import cn.mario.aiplatform.vo.Result;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @description: 文件上传，pdf解析
 * @author: mario
 * @date: 9/3/26
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @PostMapping("/uploadPDF")
    public Result<String> uploadPDF(@RequestParam("file") MultipartFile file) throws IOException {
        // 校验文件
        if (file.isEmpty()) {
            throw new BizException("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.toLowerCase().endsWith(".pdf")) {
            throw new BizException("仅支持pdf文件");
        }

        // 解析 PDF，直接从输入流解析，不落磁盘
        return Result.success(parsePdf(file.getInputStream()));
    }

    private String parsePdf(InputStream inputStream) {
        try(PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            throw new BizException("文件解析失败");
        }
    }


}
