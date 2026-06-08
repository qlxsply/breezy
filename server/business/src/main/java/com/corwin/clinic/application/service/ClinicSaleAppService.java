package com.corwin.clinic.application.service;

import com.corwin.clinic.application.command.CreateSaleCommand;
import com.corwin.clinic.application.command.SaleLineCommand;
import com.corwin.clinic.application.view.SaleDetailView;
import com.corwin.clinic.application.view.SaleLineView;
import com.corwin.clinic.application.view.SaleView;
import com.corwin.clinic.domain.model.*;
import com.corwin.clinic.domain.repo.ItemLedgerRepository;
import com.corwin.clinic.domain.repo.SaleLineRepository;
import com.corwin.clinic.domain.repo.SaleRepository;
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
public class ClinicSaleAppService {

    private final SaleRepository saleRepo;
    private final SaleLineRepository saleLineRepo;
    private final ItemLedgerRepository ledgerRepo;
    private final ClinicCatalogAppService catalogService;

    @Transactional
    public Long create(CreateSaleCommand cmd) {
        BizAssert.notNull(cmd, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.soldAt(), BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.totalAmount(), BaseError.MISSING_PARAMETER);
        BizAssert.notEmpty(cmd.lines(), BaseError.MISSING_PARAMETER);

        Sale sale = Sale.create(cmd.soldAt(), cmd.totalAmount(), trimToNull(cmd.customerName()),
                trimToNull(cmd.remark()));
        saleRepo.save(sale);

        List<SaleLine> lines = new ArrayList<>();
        for (SaleLineCommand lineCmd : cmd.lines()) {
            lines.add(buildLine(sale.getId(), lineCmd));
        }
        List<SaleLine> savedLines = saleLineRepo.saveAll(lines);
        List<ItemLedger> ledgers = savedLines.stream()
                .map(line -> ItemLedger.create(line.getSkuId(), LedgerBizType.SALE, sale.getId(), line.getId(),
                        cmd.soldAt(), line.getQty(), line.getUnit(), line.getLineTotalAmount(), null, line.getRemark()))
                .toList();
        ledgerRepo.saveAll(ledgers);
        return sale.getId();
    }

    public PageData<SaleView> page(String customerNameLike, LocalDate startAt, LocalDate endAt, PageSpec spec) {
        PageData<Sale> page = saleRepo.page(trimToNull(customerNameLike), startAt, endAt, spec);
        List<SaleView> views = page.elements().stream()
                .map(sale -> new SaleView(sale.getId(), sale.getSoldAt(), sale.getTotalAmount(), sale.getCustomerName(),
                        sale.getRemark())).toList();
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), views);
    }

    public SaleDetailView detail(Long id) {
        Sale sale = saleRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
        List<SaleLine> lines = saleLineRepo.findBySaleId(id);
        Map<Long, ItemSku> skuMap = catalogService.mapSkus(lines.stream().map(SaleLine::getSkuId).distinct().toList());
        List<SaleLineView> lineViews = lines.stream().map(line -> toLineView(line, skuMap)).toList();
        BigDecimal linesTotal = lines.stream().map(SaleLine::getLineTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = sale.getTotalAmount().subtract(linesTotal);
        return new SaleDetailView(sale.getId(), sale.getSoldAt(), sale.getTotalAmount(), linesTotal, diff,
                sale.getCustomerName(), sale.getRemark(), lineViews);
    }

    @Transactional
    public void delete(Long id) {
        Sale sale = saleRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
        ledgerRepo.deleteByBizTypeAndBizId(LedgerBizType.SALE, sale.getId());
        saleLineRepo.deleteBySaleId(sale.getId());
        saleRepo.delete(sale);
    }

    private SaleLine buildLine(Long saleId, SaleLineCommand cmd) {
        BizAssert.notNull(cmd, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.skuId(), BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.qty(), BaseError.MISSING_PARAMETER);
        BizAssert.state(cmd.qty().compareTo(BigDecimal.ZERO) > 0, BaseError.INVALID_PARAMETER);
        BizAssert.notBlank(cmd.unit(), BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.lineTotalAmount(), BaseError.MISSING_PARAMETER);

        var sku = catalogService.resolveSku(cmd.skuId(), cmd.category(), cmd.itemName(), cmd.manufacturer(),
                cmd.spec());
        BigDecimal unitPrice = calculateUnitPrice(cmd.lineTotalAmount(), cmd.qty());
        return SaleLine.create(saleId, sku.getId(), cmd.qty(), cmd.unit().trim(), cmd.lineTotalAmount(), unitPrice,
                trimToNull(cmd.remark()));
    }

    private SaleLineView toLineView(SaleLine line, Map<Long, ItemSku> skuMap) {
        var sku = skuMap.get(line.getSkuId());
        return new SaleLineView(line.getId(), line.getSkuId(), sku == null ? null : sku.getDisplayName(),
                sku == null ? null : sku.getManufacturer(), sku == null ? null : sku.getSpec(), line.getQty(),
                line.getUnit(), line.getLineTotalAmount(), line.getUnitPrice(), line.getRemark());
    }

    private BigDecimal calculateUnitPrice(BigDecimal lineTotalAmount, BigDecimal qty) {
        if (lineTotalAmount == null || qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return lineTotalAmount.divide(qty, 4, RoundingMode.HALF_UP);
    }

    private String trimToNull(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return text.trim();
    }
}
