package com.platform.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.user.dto.WorkerCreateDTO;
import com.platform.user.dto.WorkerQueryDTO;
import com.platform.user.dto.WorkerUpdateDTO;
import com.platform.user.entity.Worker;

public interface WorkerService {

    Worker create(WorkerCreateDTO dto);

    Worker update(Long id, WorkerUpdateDTO dto);

    Worker getById(Long id);

    IPage<Worker> page(WorkerQueryDTO query);

    void delete(Long id);

    void resetPassword(Long id);
}
