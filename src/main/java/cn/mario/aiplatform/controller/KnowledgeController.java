package cn.mario.aiplatform.controller;


import cn.mario.aiplatform.exception.BizException;
import cn.mario.aiplatform.service.PdfKnowledgeService;
import cn.mario.aiplatform.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;


/**
 * @description: 文件上传，pdf解析
 * @author: mario
 * @date: 9/3/26
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Autowired
    private PdfKnowledgeService pdfKnowledgeService;

    @PostMapping("/pdf")
    public Result<String> upload(@RequestParam(value = "file") MultipartFile file) {
        // 校验文件
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new BizException("仅支持pdf文件");
        }

        pdfKnowledgeService.importPdf(file.getResource());

        return Result.success("PDF 导入成功");
    }

    @GetMapping(value = "/query")
    public Result<String> query(@RequestParam(value = "message") String message) {
        return Result.success(pdfKnowledgeService.query(message));
    }

    @GetMapping(value = "/query/stream", produces = "text/event-stream")
    public Flux<String> stream(@RequestParam(value = "message") String message) {
        return pdfKnowledgeService.stream(message);
    }

}
