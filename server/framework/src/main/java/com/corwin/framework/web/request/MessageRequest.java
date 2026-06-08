package com.corwin.framework.web.request;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Corwin 2026/1/8
 */
@Getter
@Setter
public class MessageRequest extends BaseRequest {
    /**
     * 重试次数
     */
    private Integer retryCount;
}
