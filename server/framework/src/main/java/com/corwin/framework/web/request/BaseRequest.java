package com.corwin.framework.web.request;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Corwin 2026/1/8
 */
@Getter
@Setter
public class BaseRequest {
    /**
     * 版本号
     */
    private String apiVersion;
    /**
     * 客户端时间戳
     */
    private Long timestamp;
    /**
     * 调用方系统标识（外部系统对接常用）
     */
    private String clientId;
    /**
     * 幂等键
     */
    private String idemKey;
}
