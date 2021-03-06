package com.yd.web.resolver;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.PriorityOrdered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import com.alibaba.fastjson.JSON;
import com.yd.core.res.BaseResponse;
import com.yd.core.utils.BusinessException;

/**
 * 基于HandlerExceptionResolver的
 * 全局异常处理器
 *
 * @author xulg
 */
@Component
public class CustomGlobalExceptionResolver implements HandlerExceptionResolver, PriorityOrdered, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(CustomGlobalExceptionResolver.class);

    @Value("${spring.profiles.active}")
    private String profile;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
        logger.error("全局异常处理器===>捕获异常：", e);
        if (handler == null) {
            // 说明没有找到执行的handler方法
            this.writeFailedJsonResult(e, response);
            return new ModelAndView();
        }
        if (handler instanceof ResourceHttpRequestHandler) {
            ModelAndView mv = new ModelAndView();
            mv.setViewName("error");
            return mv;
        }
        if (!HandlerMethod.class.isAssignableFrom(handler.getClass())) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "handler can not cast to {0}", HandlerMethod.class.getName()));
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // Ajax请求
        if (this.isAjaxRequest(request, response, handlerMethod)) {
            // 返回json格式的错误信息
            this.writeFailedJsonResult(e, response);
            return new ModelAndView();
        } else {
            // 页面路由
            ModelAndView mv = new ModelAndView("500");
            mv.addObject("errMsg", this.fetchErrorInfoByProfile(e).getMessage());
            return mv;
        }
    }

    @Override
    public int getOrder() {
        // 设置HandlerExceptionResolver的优先级为最高
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

    /**
     * is ajax request
     *
     * @param request       the request
     * @param response      the response
     * @param handlerMethod the handler method
     * @return is ajax request
     */
    private boolean isAjaxRequest(HttpServletRequest request,
                                  HttpServletResponse response,
                                  HandlerMethod handlerMethod) {
        String accept = request.getHeader("accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        String contentType = response.getContentType();
        Method method = handlerMethod.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        return StringUtils.containsIgnoreCase(accept, "application/json")
                || StringUtils.containsIgnoreCase(contentType, "application/json")
                || StringUtils.containsIgnoreCase(xRequestedWith, "XMLHttpRequest")
                || method.isAnnotationPresent(ResponseBody.class)
                || clazz.isAnnotationPresent(ResponseBody.class)
                || clazz.isAnnotationPresent(RestController.class);
    }

    /**
     * write request failed result
     *
     * @param e        the exception
     * @param response the response
     */
    private void writeFailedJsonResult(Exception e, HttpServletResponse response) {
        CodeAndMessage codeAndMessage = this.fetchErrorInfoByProfile(e);
        BaseResponse<Object> result = new BaseResponse<>();
        result.setCode(codeAndMessage.code);
        result.setMessage(codeAndMessage.getMessage());
        result.setResult(null);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache, must-revalidate");
        try {
            response.getWriter().write(JSON.toJSONString(result));
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new IllegalStateException(e1);
        }
    }

    private CodeAndMessage fetchErrorInfoByProfile(Throwable t) {
        // 开发环境和预发环境直接返回错误信息
        if ("dev".equalsIgnoreCase(profile) || "uat".equalsIgnoreCase(profile)) {
            String code = t instanceof BusinessException ? ((BusinessException) t).getCode() : "err";
            return new CodeAndMessage(code, t.getMessage());
        } else {
            return this.fetchErrorCodeAndMessageByException(t);
        }
    }

    private CodeAndMessage fetchErrorCodeAndMessageByException(Throwable t) {
        CodeAndMessage codeAndMessage = new CodeAndMessage();
        // 参数校验异常的处理
        if (t instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validException = (MethodArgumentNotValidException) t;
            FieldError fieldError = validException.getBindingResult().getFieldError();
            codeAndMessage.setCode("900000");
            if (fieldError != null) {
                codeAndMessage.setMessage(fieldError.getDefaultMessage());
            } else {
                codeAndMessage.setMessage("参数错误");
            }
            return codeAndMessage;
        }
        // 参数校验异常的处理
        if (t instanceof ConstraintViolationException) {
            Set<ConstraintViolation<?>> violations = ((ConstraintViolationException) t).getConstraintViolations();
            String message = new ArrayList<>(violations).get(0).getMessage();
            codeAndMessage.setCode("900001");
            codeAndMessage.setMessage(message);
            return codeAndMessage;
        }
        // 参数校验异常的处理
        if (t instanceof BindException) {
            List<ObjectError> allErrors = ((BindException) t).getAllErrors();
            String message = allErrors.get(0).getDefaultMessage();
            codeAndMessage.setCode("900002");
            codeAndMessage.setMessage(message);
            return codeAndMessage;
        }
        if (t instanceof BusinessException) {
            codeAndMessage.setCode(((BusinessException) t).getCode());
            codeAndMessage.setMessage(((BusinessException) t).getMessage());
            return codeAndMessage;
        }
        // 参数缺失
        if (t instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException exception = (MissingServletRequestParameterException) t;
            codeAndMessage.setCode("900003");
            codeAndMessage.setMessage("参数" + exception.getParameterName() + "缺失或类型错误");
            return codeAndMessage;
        }
        // 参数类型异常
        if (t instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException mismatchException = (MethodArgumentTypeMismatchException) t;
            String message = mismatchException.getMessage();
            codeAndMessage.setCode("900004");
            codeAndMessage.setMessage("参数类型解析异常");
            return codeAndMessage;
        }
        // 文件上传异常
        if (t instanceof MultipartException) {
            codeAndMessage.setCode("900005");
            String message;
            if (t instanceof MaxUploadSizeExceededException) {
                long maxUploadSize = ((MaxUploadSizeExceededException) t)
                        .getMaxUploadSize();
                if (maxUploadSize < 0) {
                    message = "上传文件超过上限";
                } else {
                    message = "上传文件最大不能超过" + (maxUploadSize / 1024) + "KB";
                }
            } else {
                message = "文件上传异常";
            }
            codeAndMessage.setMessage(message);
            return codeAndMessage;
        }
        // 请求方法不支持
        if (t instanceof HttpRequestMethodNotSupportedException) {
            codeAndMessage.setCode("900012");
            codeAndMessage.setMessage("不支持的请求方式"
                    + ((HttpRequestMethodNotSupportedException) t).getMethod());
            return codeAndMessage;
        }
        // 请求体缺失
        if (t instanceof HttpMessageNotReadableException) {
            codeAndMessage.setCode("900011");
            codeAndMessage.setMessage("请求体解析异常");
            return codeAndMessage;
        }
        // ContentType错误
        if (t instanceof HttpMediaTypeNotSupportedException) {
            HttpMediaTypeNotSupportedException exception = (HttpMediaTypeNotSupportedException) t;
            MediaType unsupportedContentType = exception.getContentType();
            codeAndMessage.setCode("900013");
            codeAndMessage.setMessage("不支持的ContentType: " + unsupportedContentType.getType()
                    + "/" + unsupportedContentType.getSubtype());
            return codeAndMessage;
        }
        // 业务异常
        if (t instanceof BusinessException) {
            BusinessException businessException = (BusinessException) t;
            String message = businessException.getMessage();
            codeAndMessage.setCode(businessException.getCode());
            codeAndMessage.setMessage(message);
            return codeAndMessage;
        }
        // dubbo服务调用异常
        if (t instanceof RpcException) {
            codeAndMessage.setCode("900007");
            codeAndMessage.setMessage("服务调用失败");
            return codeAndMessage;
        }
        if (t instanceof RuntimeException) {
            codeAndMessage.setCode("900010");
            codeAndMessage.setMessage("系统执行失败");
            return codeAndMessage;
        }
        // Exception
        if (t instanceof Exception) {
            codeAndMessage.setCode("900008");
            codeAndMessage.setMessage("系统异常");
            return codeAndMessage;
        }
        // Error
        if (t instanceof Error) {
            codeAndMessage.setCode("900009");
            codeAndMessage.setMessage("系统错误");
            return codeAndMessage;
        }
        // Throwable
        codeAndMessage.setCode("999999");
        codeAndMessage.setMessage("系统异常错误");
        return codeAndMessage;
    }

    @Override
    public void afterPropertiesSet() {
        logger.info(this.getClass().getName() + "全局异常拦截器初始化完成..."
                + "当前的profile: " + this.profile);
    }

    @SuppressWarnings("WeakerAccess")
    private static class CodeAndMessage {
        private String code;
        private String message;

        private CodeAndMessage() {
        }

        @SuppressWarnings("unused")
        private CodeAndMessage(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
