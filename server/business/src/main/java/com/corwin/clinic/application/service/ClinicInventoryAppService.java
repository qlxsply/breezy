package com.corwin.clinic.application.service;

import com.corwin.clinic.application.view.ItemInventoryView;
import com.corwin.clinic.domain.model.ItemSku;
import com.corwin.clinic.domain.repo.ItemLedgerRepository;
import com.corwin.clinic.domain.repo.SkuInventory;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Corwin 2026/2/9
 */
@Service
@RequiredArgsConstructor
public class ClinicInventoryAppService {

    private final ItemLedgerRepository ledgerRepo;
    private final ClinicCatalogAppService catalogService;

    public PageData<ItemInventoryView> page(Long skuId, PageSpec spec) {
        PageData<SkuInventory> page = ledgerRepo.pageInventory(skuId, spec);
        List<Long> skuIds = page.elements().stream().map(SkuInventory::skuId).toList();
        Map<Long, ItemSku> skuMap = catalogService.mapSkus(skuIds);

        List<ItemInventoryView> views = page.elements().stream().map(inv -> {
            ItemSku sku = skuMap.get(inv.skuId());
            return new ItemInventoryView(inv.skuId(), sku == null ? null : sku.getDisplayName(),
                    sku == null ? null : sku.getManufacturer(), sku == null ? null : sku.getSpec(), inv.inventoryQty());
        }).toList();

        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), views);
    }
}
