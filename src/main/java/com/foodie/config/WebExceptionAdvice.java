package com.foodie.config;

import com.foodie.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author Emma_Lyy
 * @create 2022-11-30 15:58
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e){
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }



}
