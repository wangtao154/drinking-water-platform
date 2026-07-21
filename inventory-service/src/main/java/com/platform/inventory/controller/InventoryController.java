package com.platform.inventory.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.inventory.dto.StockBatchCreateDTO;
import com.platform.inventory.dto.StockInboundDTO;
import com.platform.inventory.dto.StockPageQueryDTO;
import com.platform.inventory.dto.StockTransferDTO;
import com.platform.inventory.service.InventoryService;
import com.platform.inventory.vo.DeviceStockVO;
import com.platform.inventory.vo.InventoryStatsVO;
import com.platform.inventory.vo.StockBatchVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存管理 Controller
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 设备入库
     */
    @PostMapping("/devices/inbound")
    public R<DeviceStockVO> inbound(@Valid @RequestBody StockInboundDTO dto) {
        return R.ok(inventoryService.inbound(dto));
    }

    /**
     * 设备调拨
     */
    @PostMapping("/devices/transfer")
    public R<DeviceStockVO> transfer(@Valid @RequestBody StockTransferDTO dto) {
        return R.ok(inventoryService.transfer(dto));
    }

    /**
     * 分页查询设备库存
     */
    @GetMapping("/devices")
    public R<PageResult<DeviceStockVO>> deviceStockPage(StockPageQueryDTO query) {
        return R.ok(inventoryService.deviceStockPage(query));
    }

    /**
     * 库存统计
     */
    @GetMapping("/statistics")
    public R<InventoryStatsVO> getStatistics() {
        return R.ok(inventoryService.getStatistics());
    }

    /**
     * 创建批次
     */
    @PostMapping("/batches")
    public R<StockBatchVO> createBatch(@Valid @RequestBody StockBatchCreateDTO dto) {
        return R.ok(inventoryService.createBatch(dto));
    }

    /**
     * 分页查询批次
     */
    @GetMapping("/batches")
    public R<PageResult<StockBatchVO>> batchPage(StockPageQueryDTO query) {
        return R.ok(inventoryService.batchPage(query));
    }
}
