package cn.mario.aiplatform.exception;

import cn.mario.aiplatform.controller.FileController;
import cn.mario.aiplatform.vo.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlobalExceptionHandlerTests {

    @Test
    void emptyFileBecomesBadRequestProblem() {
        BizException exception = assertThrows(BizException.class,
                () -> new FileController().uploadPDF(new MockMultipartFile("file", new byte[0])));
        Result<Void> result = new GlobalExceptionHandler().handleBizException(exception);

        assertEquals(0, result.getCode());
    }
}
