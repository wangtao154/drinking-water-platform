package com.platform.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.auth.UserContext;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.common.exception.BusinessException;
import com.platform.inventory.dto.StockBatchCreateDTO;
import com.platform.inventory.dto.StockInboundDTO;
import com.platform.inventory.dto.StockPageQueryDTO;
import com.platform.inventory.dto.StockTransferDTO;
import com.platform.inventory.entity.DeviceStock;
import com.platform.inventory.entity.DeviceStockLog;
import com.platform.inventory.entity.FilterStock;
import com.platform.inventory.entity.StockBatch;
import com.platform.inventory.mapper.DeviceStockLogMapper;
import com.platform.inventory.mapper.DeviceStockMapper;
import com.platform.inventory.mapper.FilterStockMapper;
import com.platform.inventory.mapper.StockBatchMapper;
import com.platform.inventory.service.InventoryService;
import com.platform.inventory.vo.DeviceStockVO;
import com.platform.inventory.vo.InventoryStatsVO;
import com.platform.inventory.vo.StockBatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 库存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final DeviceStockMapper deviceStockMapper;
    private final DeviceStockLogMapper deviceStockLogMapper;
    private final FilterStockMapper filterStockMapper;
    private final StockBatchMapper stockBatchMapper;

    @Override
    @Transactional
    public DeviceStockVO inbound(StockInboundDTO dto) {
        LocalDateTime now = LocalDateTime.now();

        // 创建设备库存记录
        DeviceStock deviceStock = new DeviceStock();
        deviceStock.setDeviceId(dto.getDeviceId());
        deviceStock.setBatchNo(dto.getBatchNo());
        deviceStock.setWarehouseType(dto.getWarehouseType());
        deviceStock.setWarehouseId(dto.getWarehouseId());
        deviceStock.setStockStatus("IN_STOCK");
        deviceStock.setInboundAt(now);

        deviceStockMapper.insert(deviceStock);
        log.info("[InventoryService] 设备入库: deviceId={}, batchNo={}, warehouseType={}, warehouseId={}",
                dto.getDeviceId(), dto.getBatchNo(), dto.getWarehouseType(), dto.getWarehouseId());

        // 插入库存日志
        saveStockLog(dto.getDeviceId(), dto.getBatchNo(),
                null, dto.getWarehouseType() + ":" + dto.getWarehouseId(),
                "INBOUND", dto.getQuantity(), "设备入库");

        return DeviceStockVO.fromEntity(deviceStock);
    }

    @Override
    @Transactional
    public DeviceStockVO transfer(StockTransferDTO dto) {
        // 1. 查找源仓库中的设备库存
        DeviceStock deviceStock = deviceStockMapper.selectOne(
                new LambdaQueryWrapper<DeviceStock>()
                        .eq(DeviceStock::getDeviceId, dto.getDeviceId())
                        .eq(DeviceStock::getWarehouseType, dto.getFromWarehouseType())
                        .eq(DeviceStock::getWarehouseId, dto.getFromWarehouseId())
                        .eq(DeviceStock::getStockStatus, "IN_STOCK")
                        .last("LIMIT 1")
        );

        if (deviceStock == null) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT,
                    "设备在源仓库中不存在或不在库中");
        }

        LocalDateTime now = LocalDateTime.now();
        String fromWarehouse = dto.getFromWarehouseType() + ":" + dto.getFromWarehouseId();
        String toWarehouse = dto.getToWarehouseType() + ":" + dto.getToWarehouseId();

        // 2. 更新为 TRANSFERRING 状态
        deviceStockMapper.update(null,
                new LambdaUpdateWrapper<DeviceStock>()
                        .eq(DeviceStock::getId, deviceStock.getId())
                        .set(DeviceStock::getStockStatus, "TRANSFERRING")
        );

        // 3. 更新到目标仓库并设为 IN_STOCK
        deviceStockMapper.update(null,
                new LambdaUpdateWrapper<DeviceStock>()
                        .eq(DeviceStock::getId, deviceStock.getId())
                        .set(DeviceStock::getWarehouseType, dto.getToWarehouseType())
                        .set(DeviceStock::getWarehouseId, dto.getToWarehouseId())
                        .set(DeviceStock::getStockStatus, "IN_STOCK")
        );

        // 刷新实体
        deviceStock.setWarehouseType(dto.getToWarehouseType());
        deviceStock.setWarehouseId(dto.getToWarehouseId());
        deviceStock.setStockStatus("IN_STOCK");
        deviceStock.setUpdatedAt(now);

        log.info("[InventoryService] 设备调拨: deviceId={}, from={}, to={}",
                dto.getDeviceId(), fromWarehouse, toWarehouse);

        // 插入库存日志
        saveStockLog(dto.getDeviceId(), deviceStock.getBatchNo(),
                fromWarehouse, toWarehouse, "TRANSFER", 1, "设备调拨");

        return DeviceStockVO.fromEntity(deviceStock);
    }

    @Override
    public PageResult<DeviceStockVO> deviceStockPage(StockPageQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<DeviceStock> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getDeviceId())) {
            wrapper.eq(DeviceStock::getDeviceId, query.getDeviceId());
        }
        if (StringUtils.hasText(query.getWarehouseType())) {
            wrapper.eq(DeviceStock::getWarehouseType, query.getWarehouseType());
        }
        if (StringUtils.hasText(query.getStockStatus())) {
            wrapper.eq(DeviceStock::getStockStatus, query.getStockStatus());
        }
        if (StringUtils.hasText(query.getBatchNo())) {
            wrapper.eq(DeviceStock::getBatchNo, query.getBatchNo());
        }
        wrapper.orderByDesc(DeviceStock::getCreatedAt);

        IPage<DeviceStock> page = new Page<>(query.getPageNum(), query.getPageSize());
        deviceStockMapper.selectPage(page, wrapper);

        return PageResult.of(page.convert(DeviceStockVO::fromEntity));
    }

    @Override
    public InventoryStatsVO getStatistics() {
        InventoryStatsVO stats = new InventoryStatsVO();

        // 设备库存统计
        stats.setTotalDeviceStock(deviceStockMapper.selectCount(null));
        stats.setInStockDevices(deviceStockMapper.selectCount(
                new LambdaQueryWrapper<DeviceStock>()
                        .eq(DeviceStock::getStockStatus, "IN_STOCK")
        ));
        stats.setOutStockDevices(deviceStockMapper.selectCount(
                new LambdaQueryWrapper<DeviceStock>()
                        .eq(DeviceStock::getStockStatus, "OUT_STOCK")
        ));
        stats.setTransferringDevices(deviceStockMapper.selectCount(
                new LambdaQueryWrapper<DeviceStock>()
                        .eq(DeviceStock::getStockStatus, "TRANSFERRING")
        ));

        // 滤芯库存统计
        stats.setTotalFilterStock(filterStockMapper.selectCount(null));
        stats.setInStockFilters(filterStockMapper.selectCount(
                new LambdaQueryWrapper<FilterStock>()
                        .eq(FilterStock::getStockStatus, "IN_STOCK")
        ));

        return stats;
    }

    @Override
    @Transactional
    public StockBatchVO createBatch(StockBatchCreateDTO dto) {
        StockBatch batch = new StockBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setBatchType(dto.getBatchType());
        batch.setProductType(dto.getProductType());
        batch.setQuantity(dto.getQuantity());
        batch.setImportMethod(dto.getImportMethod() != null ? dto.getImportMethod() : "MANUAL");
        batch.setImportFileUrl(dto.getImportFileUrl());
        batch.setManufacturer(dto.getManufacturer());
        batch.setProducedAt(dto.getProducedAt());
        batch.setRemark(dto.getRemark());
        batch.setCreatedAt(LocalDateTime.now());

        // 设置操作人信息
        Long operatorId = UserContext.getUserId();
        batch.setOperatorId(operatorId != null ? operatorId : 0L);
        batch.setOperatorName(UserContext.get() != null ? UserContext.get().getUserName() : "系统");

        stockBatchMapper.insert(batch);
        log.info("[InventoryService] 创建批次: batchNo={}, type={}, quantity={}",
                batch.getBatchNo(), batch.getBatchType(), batch.getQuantity());

        return StockBatchVO.fromEntity(batch);
    }

    @Override
    public PageResult<StockBatchVO> batchPage(StockPageQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<StockBatch> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getBatchNo())) {
            wrapper.eq(StockBatch::getBatchNo, query.getBatchNo());
        }
        wrapper.orderByDesc(StockBatch::getCreatedAt);

        IPage<StockBatch> page = new Page<>(query.getPageNum(), query.getPageSize());
        stockBatchMapper.selectPage(page, wrapper);

        return PageResult.of(page.convert(StockBatchVO::fromEntity));
    }

    // ==================== 私有方法 ====================

    /**
     * 生成批次号：BT + yyyyMMddHHmmss + 4位随机数
     */
    private String generateBatchNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(10000);
        return "BT" + timestamp + String.format("%04d", random);
    }

    /**
     * 保存库存流转日志
     */
    private void saveStockLog(String deviceId, String batchNo, String fromWarehouse,
                              String toWarehouse, String operation, Integer quantity, String remark) {
        DeviceStockLog stockLog = new DeviceStockLog();
        stockLog.setDeviceId(deviceId);
        stockLog.setBatchNo(batchNo);
        stockLog.setFromWarehouse(fromWarehouse);
        stockLog.setToWarehouse(toWarehouse);
        stockLog.setOperation(operation);
        stockLog.setQuantity(quantity);
        stockLog.setRemark(remark);
        stockLog.setCreatedAt(LocalDateTime.now());

        // 设置操作人信息
        Long operatorId = UserContext.getUserId();
        stockLog.setOperatorId(operatorId != null ? operatorId : 0L);
        stockLog.setOperatorName(UserContext.get() != null ? UserContext.get().getUserName() : "系统");

        deviceStockLogMapper.insert(stockLog);
    }
}
