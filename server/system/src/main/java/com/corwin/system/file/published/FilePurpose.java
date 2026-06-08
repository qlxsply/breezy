package com.corwin.system.file.published;

import lombok.Getter;

/**
 * @author Corwin 2026/4/16
 */
@Getter
public enum FilePurpose {
    AVATAR("头像"),
    ATTACHMENT("业务附件"),
    SNAPSHOT("数据快照"),
    CATALOG("目录图片"),
    DRIVE("个人云盘"),
    DDL("数据库DDL"),
    JSONFMT("JSON工具"),
    STATIC_ASSET("前端静态资源");

    private final String desc;

    FilePurpose(String desc) {
        this.desc = desc;
    }
}
