package com.platform.inventory.service;

import com.platform.common.result.PageResult;
import com.platform.inventory.dto.StockBatchCreateDTO;
import com.platform.inventory.dto.StockInboundDTO;
import com.platform.inventory.dto.StockPageQueryDTO;
import com.platform.inventory.dto.StockTransferDTO;
import com.platform.inventory.vo.DeviceStockVO;
import com.platform.inventory.vo.InventoryStatsVO;
import com.platform.inventory.vo.StockBatchVO;

/**
 * 库存服务接口
 */
public interface InventoryService {

    /**
     * 设备入库
     * 设置 stock_status=IN_STOCK, inbound_at=now, 插入库存日志
     */
    DeviceStockVO inbound(StockInboundDTO dto);

    /**
     * 设备调拨
     * 更新库存为 TRANSFERRING，再更新到目标仓库 IN_STOCK，插入库存日志
     */
    DeviceStockVO transfer(StockTransferDTO dto);

    /**
     * 分页查询设备库存
     */
    PageResult<DeviceStockVO> deviceStockPage(StockPageQueryDTO query);

    /**
     * 库存统计
     */
    InventoryStatsVO getStatistics();

    /**
     * 创建批次
     * 生成批次号 = "BT" + yyyyMMddHHmmss + 4位随机数
     */
    StockBatchVO createBatch(StockBatchCreateDTO dto);

    /**
     * 分页查询批次
     */
    PageResult<StockBatchVO> batchPage(StockPageQueryDTO query);
}
