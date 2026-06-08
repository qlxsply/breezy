package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.system.dict.domain.model.DictSourceType;
import com.corwin.system.user.domain.model.DefaultUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Corwin 2026/4/28
 */
@Service
@RequiredArgsConstructor
public class BootstrapDictionarySyncService {

    private static final String OPERATOR = DefaultUser.SYSTEM.account();

    private final DataSource dataSource;
    private final BootstrapDefinitionResources definitionResources;
    private final BootstrapSqlTemplateService sqlTemplateService;
    private final DictionaryDefinitionLoader dictionaryDefinitionLoader;

    public BootstrapTaskReport run(boolean dryRun) {
        long startedAt = System.currentTimeMillis();
        List<DictTypeSeed> definitions = dictionaryDefinitionLoader.loadDefinitions(
                definitionResources.dictionariesXml(), definitionResources.dictionariesXsd());
        List<DictItemRow> itemRows = flattenItems(definitions);

        if (!dryRun) {
            BootstrapJdbcTransactionSupport.executeWithoutResult(dataSource, connection -> {
                executeDelete(connection, sqlTemplateService.load("dict_delete_items.sql"));
                executeDelete(connection, sqlTemplateService.load("dict_delete_types.sql"));
                batchInsertTypes(connection, definitions);
                batchInsertItems(connection, itemRows);
            });
        }

        String message = "dictTypes=" + definitions.size() + "; dictItems=" + itemRows.size() + "; strategy=delete-all-and-rebuild";
        return new BootstrapTaskReport(BootstrapTaskKey.DICTIONARY_SYNC, dryRun, true,
                System.currentTimeMillis() - startedAt, message);
    }

    private List<DictItemRow> flattenItems(List<DictTypeSeed> definitions) {
        List<DictItemRow> result = new ArrayList<>();
        for (DictTypeSeed definition : definitions) {
            for (DictItemSeed item : definition.items()) {
                result.add(new DictItemRow(definition.id(), item));
            }
        }
        return result;
    }

    private void executeDelete(Connection connection, String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private void batchInsertTypes(Connection connection, List<DictTypeSeed> definitions) throws SQLException {
        String sql = sqlTemplateService.load("dict_insert_type.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (DictTypeSeed definition : definitions) {
                bindTypeParameters(ps, definition);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchInsertItems(Connection connection, List<DictItemRow> rows) throws SQLException {
        String sql = sqlTemplateService.load("dict_insert_item.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (DictItemRow row : rows) {
                bindItemParameters(ps, row);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void bindTypeParameters(PreparedStatement ps, DictTypeSeed definition) throws SQLException {
        ps.setString(1, definition.id());
        ps.setString(2, definition.code());
        ps.setString(3, definition.name());
        ps.setString(4, definition.description());
        ps.setString(5, definition.enumClass());
        ps.setString(6, definition.valueType().name());
        ps.setString(7, definition.structureType().name());
        ps.setString(8, DictSourceType.BUILTIN.name());
        ps.setBoolean(9, definition.enabled());
        ps.setString(10, OPERATOR);
        ps.setString(11, OPERATOR);
    }

    private void bindItemParameters(PreparedStatement ps, DictItemRow row) throws SQLException {
        DictItemSeed item = row.item();
        ps.setString(1, item.id());
        ps.setString(2, row.dictTypeId());
        ps.setString(3, item.parentItemId());
        ps.setString(4, item.itemCode());
        ps.setString(5, item.itemLabel());
        ps.setString(6, item.itemValue());
        ps.setInt(7, item.sortNo());
        ps.setBoolean(8, item.enabled());
        ps.setBoolean(9, item.defaultItem());
        ps.setString(10, item.tagColor());
        ps.setString(11, item.tagType());
        ps.setString(12, item.extraJson());
        ps.setString(13, item.description());
        ps.setString(14, OPERATOR);
        ps.setString(15, OPERATOR);
    }

    private record DictItemRow(
            String dictTypeId,
            DictItemSeed item
    ) {
    }
}
