package com.corwin.bootstrap.application.service;

import com.corwin.system.dict.domain.model.DictStructureType;
import com.corwin.system.dict.domain.model.DictValueType;
import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public record DictTypeSeed(
    String id,
    String code,
    String name,
    String description,
    String enumClass,
    DictValueType valueType,
    DictStructureType structureType,
    boolean enabled,
    List<DictItemSeed> items) {}
