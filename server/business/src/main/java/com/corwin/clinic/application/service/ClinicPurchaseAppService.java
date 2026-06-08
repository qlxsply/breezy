package com.corwin.clinic.application.service;

import com.corwin.clinic.application.command.CreatePurchaseCommand;
import com.corwin.clinic.application.command.PurchaseLineCommand;
import com.corwin.clinic.application.view.PurchaseDetailView;
import com.corwin.clinic.application.view.PurchaseLineView;
import com.corwin.clinic.application.view.PurchaseView;
import com.corwin.clinic.domain.model.*;
import com.corwin.clinic.domain.repo.ItemLedgerRepository;
import com.corwin.clinic.domain.repo.PurchaseLineRepository;
import com.corwin.clinic.domain.repo.PurchaseRepository;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Corwin 2026/2/8
 */
@Service
@RequiredArgsConstructor
public class ClinicPurchaseAppService {

    private final PurchaseRepository purchaseRepo;
    private final PurchaseLineRepository purchaseLineRepo;
    private final ItemLedgerRepository ledgerRepo;
    private final ClinicCatalogAppService catalogService;

    @Transactional
    public Long create(CreatePurchaseCommand cmd) {
        BizAssert.notNull(cmd, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.purchasedAt(), BaseError.MISSING_PARAMETER);
        BizAssert.notEmpty(cmd.lines(), BaseError.MISSING_PARAMETER);

        Supplier supplier = catalogService.resolveSupplier(cmd.supplierId(), cmd.supplierName());
        Purchase purchase = Purchase.create(supplier.getId(), cmd.purchasedAt(), cmd.totalAmount(), cmd.remark());
        purchaseRepo.save(purchase);

        List<PurchaseLine> lines = new ArrayList<>();
        for (PurchaseLineCommand lineCmd : cmd.lines()) {
            PurchaseLine line = buildLine(purchase.getId(), lineCmd);
            lines.add(line);
        }
        List<PurchaseLine> savedLines = purchaseLineRepo.saveAll(lines);
        List<ItemLedger> ledgers = savedLines.stream()
                .map(line -> ItemLedger.create(line.getSkuId(), LedgerBizType.PURCHASE, purchase.getId(), line.getId(),
                        cmd.purchasedAt(), line.getQty(), line.getUnit(), line.getLineTotalAmount(), supplier.getId(),
                        line.getRemark())).toList();
        ledgerRepo.saveAll(ledgers);
        return purchase.getId();
    }

    public PageData<PurchaseView> page(Long supplierId, LocalDate startAt, LocalDate endAt, PageSpec spec) {
        PageData<Purchase> page = purchaseRepo.page(supplierId, startAt, endAt, spec);
        Map<Long, Supplier> supplierMap = catalogService.mapSuppliers(
                page.elements().stream().map(Purchase::getSupplierId).distinct().toList());
        List<PurchaseView> views = page.elements().stream()
                .map(p -> new PurchaseView(p.getId(), p.getSupplierId(), nameOfSupplier(supplierMap, p.getSupplierId()),
                        p.getPurchasedAt(), p.getTotalAmount(), p.getRemark())).toList();
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), views);
    }

    public PurchaseDetailView detail(Long id) {
        Purchase purchase = purchaseRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + id));
        Supplier supplier = catalogService.resolveSupplier(purchase.getSupplierId(), null);
        List<PurchaseLine> lines = purchaseLineRepo.findByPurchaseId(id);
        Map<Long, ItemSku> skuMap = catalogService.mapSkus(
                lines.stream().map(PurchaseLine::getSkuId).distinct().toList());
        List<PurchaseLineView> lineViews = lines.stream().map(line -> toLineView(line, skuMap)).toList();
        return new PurchaseDetailView(purchase.getId(), purchase.getSupplierId(), supplier.getName(),
                purchase.getPurchasedAt(), purchase.getTotalAmount(), purchase.getRemark(), lineViews);
    }

    @Transactional
    public void delete(Long id) {
        Purchase purchase = purchaseRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + id));
        ledgerRepo.deleteByBizTypeAndBizId(LedgerBizType.PURCHASE, purchase.getId());
        purchaseLineRepo.deleteByPurchaseId(purchase.getId());
        purchaseRepo.delete(purchase);
    }

    private PurchaseLine buildLine(Long purchaseId, PurchaseLineCommand cmd) {
        BizAssert.notNull(cmd, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.qty(), BaseError.MISSING_PARAMETER);
        BizAssert.state(cmd.qty().compareTo(BigDecimal.ZERO) > 0, BaseError.INVALID_PARAMETER);
        BizAssert.notBlank(cmd.unit(), BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.lineTotalAmount(), BaseError.MISSING_PARAMETER);

        var sku = catalogService.resolveSku(cmd.skuId(), cmd.category(), cmd.itemName(), cmd.manufacturer(),
                cmd.spec());
        BigDecimal unitPrice = calculateUnitPrice(cmd.lineTotalAmount(), cmd.qty());
        return PurchaseLine.create(purchaseId, sku.getId(), cmd.qty(), cmd.unit().trim(), cmd.lineTotalAmount(),
                unitPrice, trimToNull(cmd.remark()));
    }

    private PurchaseLineView toLineView(PurchaseLine line, Map<Long, ItemSku> skuMap) {
        var sku = skuMap.get(line.getSkuId());
        return new PurchaseLineView(line.getId(), line.getSkuId(), sku == null ? null : sku.getDisplayName(),
                sku == null ? null : sku.getManufacturer(), sku == null ? null : sku.getSpec(), line.getQty(),
                line.getUnit(), line.getLineTotalAmount(), line.getUnitPrice(), line.getRemark());
    }

    private BigDecimal calculateUnitPrice(BigDecimal lineTotalAmount, BigDecimal qty) {
        if (lineTotalAmount == null || qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return lineTotalAmount.divide(qty, 4, RoundingMode.HALF_UP);
    }

    private String nameOfSupplier(Map<Long, Supplier> map, Long supplierId) {
        Supplier supplier = map.get(supplierId);
        return supplier == null ? null : supplier.getName();
    }

    private String trimToNull(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return text.trim();
    }
}
