package com.corwin.clinic.application.service;

import com.corwin.clinic.application.view.ItemLedgerView;
import com.corwin.clinic.domain.model.ItemLedger;
import com.corwin.clinic.domain.model.ItemSku;
import com.corwin.clinic.domain.model.LedgerBizType;
import com.corwin.clinic.domain.model.Supplier;
import com.corwin.clinic.domain.repo.ItemLedgerRepository;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Corwin 2026/2/8
 */
@Service
@RequiredArgsConstructor
public class ClinicLedgerAppService {

    private final ItemLedgerRepository ledgerRepo;
    private final ClinicCatalogAppService catalogService;

    public PageData<ItemLedgerView> page(Long skuId, LedgerBizType bizType, Long supplierId, LocalDate startAt,
            LocalDate endAt, PageSpec spec) {
        PageData<ItemLedger> page = ledgerRepo.page(skuId, bizType, supplierId, startAt, endAt, spec);
        List<Long> skuIds = page.elements().stream().map(ItemLedger::getSkuId).distinct().toList();
        List<Long> supplierIds = page.elements().stream().map(ItemLedger::getSupplierId).filter(Objects::nonNull)
                .distinct().toList();
        Map<Long, ItemSku> skuMap = catalogService.mapSkus(skuIds);
        Map<Long, Supplier> supplierMap = catalogService.mapSuppliers(supplierIds);
        List<ItemLedgerView> views = page.elements().stream().map(ledger -> toView(ledger, skuMap, supplierMap))
                .toList();
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), views);
    }

    private ItemLedgerView toView(ItemLedger ledger, Map<Long, ItemSku> skuMap, Map<Long, Supplier> supplierMap) {
        ItemSku sku = skuMap.get(ledger.getSkuId());
        Long supplierId = ledger.getSupplierId();
        Supplier supplier = supplierId == null ? null : supplierMap.get(supplierId);
        return new ItemLedgerView(ledger.getId(), ledger.getSkuId(), sku == null ? null : sku.getDisplayName(),
                ledger.getBizType(), ledger.getBizId(), ledger.getBizLineId(), ledger.getOccurredAt(), ledger.getQty(),
                ledger.getUnit(), ledger.getAmountTotal(), supplierId, supplier == null ? null : supplier.getName(),
                ledger.getRemark());
    }
}
