package com.platform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.user.dto.ApplicationCreateDTO;
import com.platform.user.dto.ApplicationQueryDTO;
import com.platform.user.dto.ApplicationReviewDTO;
import com.platform.user.entity.Application;
import com.platform.user.entity.Customer;
import com.platform.user.entity.Dealer;
import com.platform.user.entity.Worker;
import com.platform.user.mapper.ApplicationMapper;
import com.platform.user.mapper.CustomerMapper;
import com.platform.user.mapper.DealerMapper;
import com.platform.user.mapper.WorkerMapper;
import com.platform.user.service.ApplicationService;
import com.platform.user.vo.ApplicationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户身份申请服务实现
 *
 * 业务流程：
 * 1. 游客微信登录 → 自动注册 Customer(status=GUEST)
 * 2. 游客提交申请 → 创建 user_application 记录(status=PENDING)
 * 3. PC端管理员审批：
 *    - 通过+客户类型：更新 customer.status=ACTIVE，回填申请信息
 *    - 通过+运维类型：创建 worker 记录(关联 openId)，customer 保持 GUEST
 *    - 驳回：仅更新申请状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final CustomerMapper customerMapper;
    private final WorkerMapper workerMapper;
    private final DealerMapper dealerMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Application create(Long applicantId, ApplicationCreateDTO dto) {
        // 1. 校验申请者是否存在
        Customer customer = customerMapper.selectById(applicantId);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 2. 校验是否已是正式客户或运维
        if ("ACTIVE".equals(customer.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "您已是正式客户，无需重复申请");
        }
        Worker existingWorker = workerMapper.selectByOpenId(customer.getOpenId());
        if (existingWorker != null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "您已是运维人员，无需重复申请");
        }

        // 3. 校验是否有待审核的同类型申请
        Long pendingCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getApplicantId, applicantId)
                        .eq(Application::getApplyType, dto.getApplyType())
                        .eq(Application::getStatus, "PENDING")
        );
        if (pendingCount > 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "您已有一个待审核的同类型申请，请等待审批结果");
        }

        // 4. 创建申请记录
        Application app = new Application();
        app.setApplicantId(applicantId);
        app.setApplyType(dto.getApplyType());
        app.setName(dto.getName());
        app.setPhone(dto.getPhone());
        app.setIdCard(dto.getIdCard());
        app.setProvince(dto.getProvince());
        app.setCity(dto.getCity());
        app.setDistrict(dto.getDistrict());
        app.setAddress(dto.getAddress());
        app.setCustomerType(dto.getCustomerType());
        app.setOrgName(dto.getOrgName());
        app.setCreditCode(dto.getCreditCode());
        app.setDealerId(dto.getDealerId());
        app.setStatus("PENDING");
        applicationMapper.insert(app);

        log.info("[Application] 新申请: id={}, applicantId={}, type={}", app.getId(), applicantId, dto.getApplyType());
        return app;
    }

    @Override
    public List<ApplicationVO> myApplications(Long applicantId) {
        List<Application> list = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getApplicantId, applicantId)
                        .orderByDesc(Application::getCreatedAt)
        );
        return list.stream().map(app -> {
            ApplicationVO vo = ApplicationVO.from(app);
            if (app.getDealerId() != null) {
                Dealer dealer = dealerMapper.selectById(app.getDealerId());
                if (dealer != null) {
                    vo.setDealerName(dealer.getDealerName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<ApplicationVO> page(ApplicationQueryDTO query) {
        query.normalize();
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getApplyType())) {
            wrapper.eq(Application::getApplyType, query.getApplyType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Application::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Application::getName, query.getKeyword())
                    .or().like(Application::getPhone, query.getKeyword()));
        }
        wrapper.orderByDesc(Application::getCreatedAt);

        IPage<Application> page = new Page<>(query.getPage(), query.getSize());
        IPage<Application> result = applicationMapper.selectPage(page, wrapper);

        // 转换为 VO
        IPage<ApplicationVO> voPage = result.convert(app -> {
            ApplicationVO vo = ApplicationVO.from(app);
            // 关联查询经销商名称
            if (app.getDealerId() != null) {
                Dealer dealer = dealerMapper.selectById(app.getDealerId());
                if (dealer != null) {
                    vo.setDealerName(dealer.getDealerName());
                }
            }
            return vo;
        });

        return voPage;
    }

    @Override
    public Application getById(Long id) {
        Application app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请记录不存在");
        }
        return app;
    }

    @Override
    @Transactional
    public Application review(Long id, ApplicationReviewDTO dto, Long reviewerId) {
        // 1. 查询申请记录
        Application app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请记录不存在");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该申请已审批，请勿重复操作");
        }

        // 2. 更新申请状态
        app.setStatus(dto.getApproved() ? "APPROVED" : "REJECTED");
        app.setReviewerId(reviewerId);
        app.setReviewComment(dto.getReviewComment());
        app.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(app);

        // 3. 如果通过，执行身份转换
        if (dto.getApproved()) {
            doApprove(app);
        }

        log.info("[Application] 审批完成: id={}, approved={}, reviewerId={}", id, dto.getApproved(), reviewerId);
        return app;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Application app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请记录不存在");
        }
        applicationMapper.deleteById(id);
        log.info("[Application] 删除申请: id={}", id);
    }

    /**
     * 审批通过后的身份转换
     */
    private void doApprove(Application app) {
        Customer customer = customerMapper.selectById(app.getApplicantId());
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请者不存在");
        }

        if ("CUSTOMER".equals(app.getApplyType())) {
            // --- 转为正式客户 ---
            customer.setName(app.getName());
            customer.setPhone(app.getPhone());
            customer.setCustomerType(app.getCustomerType() != null ? app.getCustomerType() : "INDIVIDUAL");
            customer.setOrgName(app.getOrgName());
            customer.setCreditCode(app.getCreditCode());
            customer.setProvince(app.getProvince());
            customer.setCity(app.getCity());
            customer.setDistrict(app.getDistrict());
            customer.setAddress(app.getAddress());
            customer.setDealerId(app.getDealerId());
            customer.setStatus("ACTIVE");
            // 设置初始密码（手机号后6位），标记未修改
            String initialPassword = app.getPhone() != null && app.getPhone().length() >= 6
                    ? app.getPhone().substring(app.getPhone().length() - 6) : "123456";
            customer.setPassword(passwordEncoder.encode(initialPassword));
            customer.setPasswordChanged(false);
            customerMapper.updateById(customer);

            log.info("[Application] 游客转正式客户: customerId={}, name={}", customer.getId(), app.getName());

        } else if ("WORKER".equals(app.getApplyType())) {
            // --- 转为运维人员 ---
            // 检查是否已存在同手机号运维人员
            Worker existingWorker = workerMapper.selectByPhone(app.getPhone());
            if (existingWorker != null) {
                throw new BusinessException(ResultCode.DATA_DUPLICATE, "手机号已被其他运维人员使用");
            }

            Worker worker = new Worker();
            worker.setName(app.getName());
            worker.setPhone(app.getPhone());
            worker.setOpenId(customer.getOpenId()); // 关联微信 openId
            worker.setDealerId(app.getDealerId());
            worker.setProvince(app.getProvince());
            worker.setCity(app.getCity());
            worker.setDistrict(app.getDistrict());
            worker.setAddress(app.getAddress());
            // 初始密码 = 手机号后6位
            String initialPassword = app.getPhone() != null && app.getPhone().length() >= 6
                    ? app.getPhone().substring(app.getPhone().length() - 6) : "123456";
            worker.setPassword(passwordEncoder.encode(initialPassword));
            worker.setPasswordChanged(false);
            worker.setStatus("ACTIVE");
            worker.setRegisteredAt(LocalDateTime.now());
            worker.setServiceCount(0);
            worker.setRating(new BigDecimal("5.00"));
            workerMapper.insert(worker);
            customerMapper.deleteById(customer.getId());

            log.info("[Application] 游客转运维人员: workerId={}, name={}, openId={}",
                    worker.getId(), app.getName(), customer.getOpenId());
        }
    }
}
