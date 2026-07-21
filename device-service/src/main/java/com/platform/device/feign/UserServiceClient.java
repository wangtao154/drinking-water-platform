package com.platform.device.feign;

import com.platform.common.result.R;
import com.platform.device.dto.feign.CustomerFeignDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * user-service 远程调用客户端
 */
@FeignClient(name = "user-service", path = "/api/v1")
public interface UserServiceClient {

    /**
     * 根据客户 ID 获取客户信息
     */
    @GetMapping("/customers/{id}")
    R<CustomerFeignDTO> getCustomerById(@PathVariable("id") Long id);

    /**
     * 根据客户名称模糊查询客户 ID 列表
     */
    @GetMapping("/customers/ids")
    R<List<Long>> findCustomerIdsByName(@RequestParam("name") String name);
}
