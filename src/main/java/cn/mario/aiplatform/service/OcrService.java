package cn.mario.aiplatform.service;

import java.nio.file.Path;

public interface OcrService {

    public String recognize(Path image);

}
