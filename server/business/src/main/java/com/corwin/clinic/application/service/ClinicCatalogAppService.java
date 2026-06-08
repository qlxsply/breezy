package com.corwin.clinic.application.service;

import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.clinic.domain.model.ItemSku;
import com.corwin.clinic.domain.model.ItemSpu;
import com.corwin.clinic.domain.model.Supplier;
import com.corwin.clinic.domain.repo.ItemSkuRepository;
import com.corwin.clinic.domain.repo.ItemSpuRepository;
import com.corwin.clinic.domain.repo.SupplierRepository;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/2/8
 */
@Service
@RequiredArgsConstructor
public class ClinicCatalogAppService {

    private final ItemSpuRepository itemSpuRepo;
    private final ItemSkuRepository itemSkuRepo;
    private final SupplierRepository supplierRepo;

    public PageData<ItemSku> pageSkus(ItemCategory category, String nameLike, Boolean enabled, PageSpec spec) {
        return itemSkuRepo.page(category, trimToNull(nameLike), enabled, spec);
    }

    public PageData<Supplier> pageSuppliers(String nameLike, Boolean enabled, PageSpec spec) {
        return supplierRepo.page(trimToNull(nameLike), enabled, spec);
    }

    public Supplier resolveSupplier(Long supplierId, String supplierName) {
        if (supplierId != null) {
            return supplierRepo.findById(supplierId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        }
        BizAssert.notBlank(supplierName, BaseError.MISSING_PARAMETER);
        String normalized = normalizeSupplierName(supplierName);
        BizAssert.notBlank(normalized, BaseError.MISSING_PARAMETER);
        Supplier existing = supplierRepo.findByNameNorm(normalized).orElse(null);
        if (existing != null) {
            return existing;
        }
        Supplier created = Supplier.create(supplierName.trim(), normalized, null);
        return supplierRepo.save(created);
    }

    public ItemSku resolveSku(Long skuId, ItemCategory category, String itemName, String manufacturer, String spec) {
        if (skuId != null) {
            return itemSkuRepo.findById(skuId)
                    .orElseThrow(() -> new IllegalArgumentException("Item SKU not found: " + skuId));
        }
        BizAssert.notNull(category, BaseError.MISSING_PARAMETER);
        BizAssert.notBlank(itemName, BaseError.MISSING_PARAMETER);
        String displayName = itemName.trim();

        ItemSpu spu = itemSpuRepo.findByCategoryAndName(category, displayName)
                .orElseGet(() -> itemSpuRepo.save(ItemSpu.create(category, displayName, null)));

        return itemSkuRepo.findBySpuIdAndDisplayName(spu.getId(), displayName).orElseGet(() -> itemSkuRepo.save(
                ItemSku.create(spu.getId(), displayName, trimToNull(manufacturer), trimToNull(spec))));
    }

    public Map<Long, Supplier> mapSuppliers(List<Long> ids) {
        List<Long> distinct = distinctIds(ids);
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return supplierRepo.findByIdIn(distinct).stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
    }

    public Map<Long, ItemSpu> mapSpus(List<Long> ids) {
        List<Long> distinct = distinctIds(ids);
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return itemSpuRepo.findByIdIn(distinct).stream().collect(Collectors.toMap(ItemSpu::getId, Function.identity()));
    }

    public Map<Long, ItemSku> mapSkus(List<Long> ids) {
        List<Long> distinct = distinctIds(ids);
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return itemSkuRepo.findByIdIn(distinct).stream().collect(Collectors.toMap(ItemSku::getId, Function.identity()));
    }

    private String normalizeSupplierName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String collapsed = trimmed.replaceAll("\\s+", " ");
        return collapsed.toLowerCase();
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(id -> id != null).distinct().toList();
    }

    private String trimToNull(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return text.trim();
    }
}
