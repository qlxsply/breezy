package com.corwin.schemaforge.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.schemaforge.interfaces.web.res.SchemaSnapshotRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据库结构快照 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface SchemaSnapshotMybatisMapper {

    PageData<SchemaSnapshotRes> pageQuery(@Param("managedDatabaseId") Long managedDatabaseId,
            @Param("nameLike") String nameLike, @Param("spec") PageSpec spec);

    List<SchemaSnapshotRes> listAll();
}
