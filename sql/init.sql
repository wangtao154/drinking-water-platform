-- ==============================================================================
-- 直饮水平台 - 数据库初始化脚本
-- 数据库：drinking_water
-- MySQL：8.0+
-- 字符集：utf8mb4 / utf8mb4_unicode_ci
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS drinking_water
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE drinking_water;

-- ============================================================
-- 一、设备管理模块
-- ============================================================

-- 1.1 设备型号表
CREATE TABLE IF NOT EXISTS device_model (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    model_code      VARCHAR(64)   NOT NULL                 COMMENT '型号编码（唯一）',
    model_name      VARCHAR(128)  NOT NULL                 COMMENT '型号名称',
    category        VARCHAR(64)                            COMMENT '设备分类',
    description     VARCHAR(512)                           COMMENT '型号描述',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    filter_config   JSON                                  COMMENT '滤芯配置：{"level_1":"filter_model_id","level_2":"",...}',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted         TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '软删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_code (model_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备型号表';

-- 1.2 设备实例表
CREATE TABLE IF NOT EXISTS device (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID（平台业务ID，雪花算法，设备二维码含此值）',
    sn              VARCHAR(32)   NOT NULL                 COMMENT '网关序列号（MQTT ClientID，如 j082438，设备与控制板通信标识）',
    model_id        BIGINT        NOT NULL                 COMMENT '设备型号ID，关联 device_model.id',
    iccid           VARCHAR(32)                            COMMENT '物联网卡ICCID（选填）',
    imei            VARCHAR(32)                            COMMENT '设备IMEI（选填）',
    production_date DATE                                  COMMENT '生产日期',
    production_batch VARCHAR(64)                           COMMENT '生产批次',
    qr_code_url     VARCHAR(256)                           COMMENT '设备二维码图片URL（MinIO存储）',
    online_status   TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '在线状态：0=离线 1=在线',
    lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'REGISTERED' COMMENT '生命周期状态：REGISTERED/ALLOCATED/PENDING_INSTALL/ACTIVATED_ONLINE/ACTIVATED_OFFLINE/RETURNED/SCRAPPED',
    dealer_id       BIGINT                                COMMENT '当前所属经销商ID',
    customer_id     BIGINT                                COMMENT '当前绑定客户ID',
    activated_at    DATETIME(3)                            COMMENT '激活时间（客户绑定成功时）',
    returned_at     DATETIME(3)                            COMMENT '退机时间',
    scrapped_at     DATETIME(3)                            COMMENT '报废时间',
    province        VARCHAR(32)                            COMMENT '省',
    city            VARCHAR(32)                            COMMENT '市',
    district        VARCHAR(32)                            COMMENT '区',
    address         VARCHAR(256)                           COMMENT '详细地址',
    charge_mode     VARCHAR(32)                            COMMENT '计费模式：FLOW_BASED/MONTHLY_RENT/PACKAGE_RECHARGE/SHARED',
    remain_duration INT          NOT NULL DEFAULT 0        COMMENT '剩余时长（秒）',
    remain_flow     BIGINT       NOT NULL DEFAULT 0        COMMENT '剩余流量（升，×1000 精确到 mL）',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    active_flag     TINYINT(1) GENERATED ALWAYS AS (IF(deleted = 0, 1, NULL)) STORED COMMENT '未删除唯一约束标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_id_active (device_id, active_flag),
    UNIQUE KEY uk_sn_active (sn, active_flag),
    KEY idx_model_id (model_id),
    KEY idx_lifecycle_status (lifecycle_status),
    KEY idx_customer_id (customer_id),
    KEY idx_dealer_id (dealer_id),
    KEY idx_online_status (online_status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备实例表';

-- 1.3 设备状态变更日志
CREATE TABLE IF NOT EXISTS device_status_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID',
    from_status     VARCHAR(32)                            COMMENT '变更前状态',
    to_status       VARCHAR(32)   NOT NULL                 COMMENT '变更后状态',
    operator_id     BIGINT                                COMMENT '操作人ID',
    operator_name   VARCHAR(64)                            COMMENT '操作人姓名',
    remark          VARCHAR(512)                           COMMENT '备注',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态变更日志';

-- 1.4 设备绑定记录
CREATE TABLE IF NOT EXISTS device_binding (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID',
    customer_id     BIGINT        NOT NULL                 COMMENT '客户ID',
    bind_type       VARCHAR(16)   NOT NULL                 COMMENT '绑定方式：QR_SCAN/INPUT_ID',
    bind_at         DATETIME(3)   NOT NULL                 COMMENT '绑定时间',
    unbind_at       DATETIME(3)                            COMMENT '解绑时间',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/UNBOUND',
    activated_package_id BIGINT                            COMMENT '激活时选择的套餐ID',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_active (device_id, status) COMMENT '每个设备最多一条ACTIVE绑定',
    KEY idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备绑定记录';

-- ============================================================
-- 二、滤芯管理模块
-- ============================================================

-- 2.1 滤芯型号表
CREATE TABLE IF NOT EXISTS filter_model (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    model_code      VARCHAR(64)   NOT NULL                 COMMENT '滤芯型号编码（唯一）',
    model_name      VARCHAR(128)  NOT NULL                 COMMENT '滤芯型号名称',
    category        VARCHAR(32)   NOT NULL                 COMMENT '滤芯分类：PP_COTTON/GRANULAR_AC/COMPRESSED_AC/RO_MEMBRANE/POST_AC/OTHER',
    filter_level    TINYINT       NOT NULL                 COMMENT '滤芯级别：1-5（对应设备中的安装位置）',
    standard_life_duration INT    NOT NULL                 COMMENT '标准寿命-时间（月）',
    standard_life_flow     BIGINT NOT NULL                 COMMENT '标准寿命-流量（升，×1000）',
    price           BIGINT       NOT NULL DEFAULT 0        COMMENT '参考售价（分）',
    photo_url       VARCHAR(256)                           COMMENT '滤芯照片URL（MinIO）',
    description     VARCHAR(512)                           COMMENT '描述',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_code (model_code),
    KEY idx_category (category),
    KEY idx_filter_level (filter_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='滤芯型号表';

-- 2.2 滤芯实例表
CREATE TABLE IF NOT EXISTS filter_instance (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    filter_id       VARCHAR(32)   NOT NULL                 COMMENT '滤芯ID（全局唯一出厂序列号，滤芯二维码含此值）',
    filter_model_id BIGINT        NOT NULL                 COMMENT '滤芯型号ID，关联 filter_model.id',
    production_date DATE                                  COMMENT '生产日期',
    production_batch VARCHAR(64)                           COMMENT '生产批次',
    qr_code_url     VARCHAR(256)                           COMMENT '滤芯二维码图片URL（MinIO）',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'IN_STOCK' COMMENT '生命周期状态：IN_STOCK/PENDING_INSTALL/IN_USE/SCRAPPED',
    current_device_id VARCHAR(32)                          COMMENT '当前安装的设备ID（仅在IN_USE时不空）',
    installed_at    DATETIME(3)                            COMMENT '安装时间（激活到设备上）',
    installed_by    BIGINT                                COMMENT '安装人员ID',
    used_duration   INT          NOT NULL DEFAULT 0        COMMENT '已用时长（秒）',
    used_flow       BIGINT       NOT NULL DEFAULT 0        COMMENT '已用流量（升，×1000）',
    scrapped_at     DATETIME(3)                            COMMENT '报废时间',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_filter_id (filter_id),
    KEY idx_filter_model_id (filter_model_id),
    KEY idx_lifecycle_status (lifecycle_status),
    KEY idx_current_device_id (current_device_id),
    KEY idx_production_batch (production_batch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='滤芯实例表';

-- 2.3 滤芯状态变更日志
CREATE TABLE IF NOT EXISTS filter_status_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    filter_id       VARCHAR(32)   NOT NULL                 COMMENT '滤芯ID',
    from_status     VARCHAR(32)                            COMMENT '变更前状态',
    to_status       VARCHAR(32)   NOT NULL                 COMMENT '变更后状态',
    operator_id     BIGINT                                COMMENT '操作人ID',
    operator_name   VARCHAR(64)                            COMMENT '操作人姓名',
    device_id       VARCHAR(32)                            COMMENT '关联设备ID（安装/拆卸时记录）',
    remark          VARCHAR(512)                           COMMENT '备注',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_filter_id (filter_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='滤芯状态变更日志';

-- 2.4 滤芯更换记录
CREATE TABLE IF NOT EXISTS filter_replace_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID',
    old_filter_id   VARCHAR(32)   NOT NULL                 COMMENT '旧滤芯ID',
    new_filter_id   VARCHAR(32)   NOT NULL                 COMMENT '新滤芯ID',
    work_order_id   BIGINT        NOT NULL                 COMMENT '关联工单ID',
    replaced_by     BIGINT        NOT NULL                 COMMENT '更换人员ID',
    replaced_at     DATETIME(3)   NOT NULL                 COMMENT '更换时间',
    old_used_duration INT         NOT NULL                 COMMENT '旧滤芯已用时长（秒）',
    old_used_flow   BIGINT       NOT NULL                 COMMENT '旧滤芯已用流量（升，×1000）',
    photo_urls      JSON                                  COMMENT '更换照片URL列表（MinIO）',
    remark          VARCHAR(512)                           COMMENT '备注',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_old_filter_id (old_filter_id),
    KEY idx_new_filter_id (new_filter_id),
    KEY idx_work_order_id (work_order_id),
    KEY idx_replaced_at (replaced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='滤芯更换记录';

-- ============================================================
-- 三、客户管理模块
-- ============================================================

-- 3.1 客户表
CREATE TABLE IF NOT EXISTS customer (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_type   VARCHAR(16)   NOT NULL                 COMMENT '客户主体类型：PERSONAL/FAMILY/COMPANY/SCHOOL/OTHER_ORG',
    name            VARCHAR(64)                            COMMENT '姓名（个人/家庭客户）',
    phone           VARCHAR(20)                            COMMENT '电话',
    wechat          VARCHAR(64)                            COMMENT '微信号/昵称',
    org_name        VARCHAR(128)                           COMMENT '单位名称（公司/学校/单位客户）',
    credit_code    VARCHAR(32)                            COMMENT '统一社会信用代码',
    contact_name    VARCHAR(64)                            COMMENT '联系人姓名',
    contact_phone   VARCHAR(20)                            COMMENT '联系人电话',
    contact_wechat  VARCHAR(64)                            COMMENT '联系人微信',
    bank_account    VARCHAR(64)                            COMMENT '对公账户信息',
    balance         BIGINT       NOT NULL DEFAULT 0        COMMENT '余额（分）',
    remain_duration INT          NOT NULL DEFAULT 0        COMMENT '剩余时长（秒）',
    remain_flow     BIGINT       NOT NULL DEFAULT 0        COMMENT '剩余流量（升，×1000）',
    province        VARCHAR(32)                            COMMENT '省',
    city            VARCHAR(32)                            COMMENT '市',
    district        VARCHAR(32)                            COMMENT '区',
    address         VARCHAR(256)                           COMMENT '单位地址/家庭地址',
    open_id         VARCHAR(128)                           COMMENT '微信 OpenID',
    union_id        VARCHAR(128)                           COMMENT '微信 UnionID',
    dealer_id       BIGINT                                COMMENT '所属经销商ID',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：GUEST=游客(微信登录未审批), ACTIVE=正式, DISABLED=禁用, FROZEN=冻结, INACTIVE=未激活',
    registered_at   DATETIME(3)  NOT NULL                 COMMENT '注册时间',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_phone (phone),
    KEY idx_customer_type (customer_type),
    KEY idx_open_id (open_id),
    KEY idx_dealer_id (dealer_id),
    KEY idx_status (status),
    KEY idx_credit_code (credit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表（支持个人/家庭/公司/学校/其他单位）';

-- 3.2 用户反馈表
CREATE TABLE IF NOT EXISTS customer_feedback (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)                            COMMENT '设备ID',
    customer_id     BIGINT        NOT NULL                 COMMENT '客户ID',
    customer_name   VARCHAR(64)                            COMMENT '客户名称',
    contact_phone   VARCHAR(20)                            COMMENT '联系电话',
    content         VARCHAR(1024) NOT NULL                 COMMENT '反馈内容',
    photo_urls      JSON                                  COMMENT '反馈照片URL列表（MinIO）',
    reply_content   VARCHAR(1024)                          COMMENT '回复内容',
    feedback_type   VARCHAR(32)                            COMMENT '反馈类型',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/RESOLVED',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_customer_id (customer_id),
    KEY idx_device_id (device_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- ============================================================
-- 四、经销商管理模块
-- ============================================================

-- 4.1 经销商表
CREATE TABLE IF NOT EXISTS dealer (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    dealer_code     VARCHAR(16)   NOT NULL                 COMMENT '经销商编号（5位数字登录账号）',
    dealer_name     VARCHAR(128)  NOT NULL                 COMMENT '经销商名称',
    dealer_level    VARCHAR(16)   NOT NULL                 COMMENT '经销商层级：L1_DEALER/L2_DEALER/L3_DEALER',
    parent_id       BIGINT                                COMMENT '上级经销商ID',
    phone           VARCHAR(20)   NOT NULL                 COMMENT '手机号',
    password        VARCHAR(128)  NOT NULL                 COMMENT '密码（BCrypt加密）',
    recharge_amount BIGINT       NOT NULL DEFAULT 0        COMMENT '充值金额（分）',
    credit_balance  BIGINT       NOT NULL DEFAULT 0        COMMENT '授权余额（分）',
    total_balance   BIGINT       NOT NULL DEFAULT 0        COMMENT '累计余额（分）',
    commission_rate DECIMAL(5,4) NOT NULL DEFAULT 0        COMMENT '分润比例（0.0000~1.0000）',
    region          VARCHAR(128)                           COMMENT '区域',
    bank_account    VARCHAR(64)                            COMMENT '银行账户',
    contact_name    VARCHAR(64)                            COMMENT '联系人姓名',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED/CANCELLED',
    registered_at   DATETIME(3)   NOT NULL                 COMMENT '登记时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dealer_code (dealer_code),
    KEY idx_phone (phone),
    KEY idx_parent_id (parent_id),
    KEY idx_dealer_level (dealer_level),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经销商表';

-- ============================================================
-- 五、运维人员模块
-- ============================================================

-- 5.1 运维人员表
CREATE TABLE IF NOT EXISTS worker (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name            VARCHAR(64)   NOT NULL                 COMMENT '姓名',
    phone           VARCHAR(20)   NOT NULL                 COMMENT '手机号（登录账号）',
    open_id         VARCHAR(128)                           COMMENT '微信OpenID',
    password        VARCHAR(128)  NOT NULL                 COMMENT '密码（BCrypt加密，初始123456）',
    province        VARCHAR(32)                            COMMENT '省',
    city            VARCHAR(32)                            COMMENT '市',
    district        VARCHAR(32)                            COMMENT '区',
    address         VARCHAR(256)                           COMMENT '详细地址',
    dealer_id       BIGINT        NOT NULL                 COMMENT '所属经销商ID',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED/CANCELLED',
    service_count   INT           NOT NULL DEFAULT 0        COMMENT '服务工单数量',
    rating          DECIMAL(3,2)  NOT NULL DEFAULT 5.00     COMMENT '平均评分',
    registered_at   DATETIME(3)   NOT NULL                 COMMENT '登记时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    active_flag     TINYINT(1) GENERATED ALWAYS AS (IF(deleted = 0, 1, NULL)) STORED COMMENT '未删除唯一约束标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone_active (phone, active_flag),
    KEY idx_worker_open_id (open_id),
    KEY idx_dealer_id (dealer_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维人员表';

-- 5.1.1 运维人员公众号绑定表
CREATE TABLE IF NOT EXISTS worker_wechat_binding (
    id                  BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    worker_id           BIGINT        NOT NULL                 COMMENT '运维人员ID',
    mini_open_id        VARCHAR(128)                           COMMENT '小程序OpenID',
    official_open_id    VARCHAR(128)  NOT NULL                 COMMENT '公众号OpenID',
    union_id            VARCHAR(128)                           COMMENT '微信开放平台UnionID',
    subscribe_status    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '关注状态：0未知/未关注，1已关注',
    bound_at            DATETIME(3)                            COMMENT '绑定时间',
    unbound_at          DATETIME(3)                            COMMENT '解绑时间',
    created_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          BIGINT                                 COMMENT '创建人ID',
    updated_by          BIGINT                                 COMMENT '更新人ID',
    deleted             TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除：0未删，1已删',
    active_flag         TINYINT(1) GENERATED ALWAYS AS (IF(deleted = 0, 1, NULL)) STORED COMMENT '未删除唯一约束标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_worker_id_active (worker_id, active_flag),
    UNIQUE KEY uk_official_open_id_active (official_open_id, active_flag),
    KEY idx_union_id (union_id),
    KEY idx_subscribe_status (subscribe_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维人员公众号绑定表';

-- 5.2 用户身份申请表
CREATE TABLE IF NOT EXISTS user_application (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    applicant_id    BIGINT        NOT NULL                 COMMENT '申请者 customer.id',
    apply_type      VARCHAR(16)   NOT NULL                 COMMENT '申请类型：CUSTOMER=正式客户, WORKER=运维人员',
    name            VARCHAR(64)   NOT NULL                 COMMENT '姓名',
    phone           VARCHAR(20)   NOT NULL                 COMMENT '手机号',
    id_card         VARCHAR(32)                            COMMENT '身份证号',
    province        VARCHAR(32)                            COMMENT '省',
    city            VARCHAR(32)                            COMMENT '市',
    district        VARCHAR(32)                            COMMENT '区',
    address         VARCHAR(256)                           COMMENT '详细地址',
    customer_type   VARCHAR(16)                            COMMENT '客户类型(仅客户申请)：PERSONAL/FAMILY/COMPANY/SCHOOL/OTHER_ORG',
    org_name        VARCHAR(128)                           COMMENT '单位名称(仅单位客户)',
    credit_code     VARCHAR(32)                             COMMENT '统一社会信用代码',
    dealer_id       BIGINT                                 COMMENT '所属经销商(仅运维申请)',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    reviewer_id     BIGINT                                 COMMENT '审批人ID',
    review_comment  VARCHAR(256)                           COMMENT '审批意见',
    reviewed_at     DATETIME(3)                            COMMENT '审批时间',
    created_by      BIGINT                                 COMMENT '创建者ID',
    updated_by      BIGINT                                 COMMENT '更新者ID',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_applicant (applicant_id),
    KEY idx_status (status),
    KEY idx_apply_type (apply_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户身份申请表';

-- ============================================================
-- 六、工单管理模块
-- ============================================================

-- 6.1 工单表
CREATE TABLE IF NOT EXISTS work_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    order_no        VARCHAR(32)   NOT NULL                 COMMENT '工单编号（唯一）',
    order_type      VARCHAR(32)   NOT NULL                 COMMENT '工单类型：INSTALL_APPOINTMENT/REPAIR/REMOVE/RELOCATE/FILTER_REPLACE',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID',
    customer_id     BIGINT        NOT NULL                 COMMENT '客户ID',
    customer_name   VARCHAR(64)                            COMMENT '客户名称',
    customer_phone  VARCHAR(20)                            COMMENT '客户电话',
    province        VARCHAR(32)                            COMMENT '省',
    city            VARCHAR(32)                            COMMENT '市',
    district        VARCHAR(32)                            COMMENT '区',
    address         VARCHAR(256)                           COMMENT '设备地址',
    description     VARCHAR(1024)                          COMMENT '工单描述',
    model_id        BIGINT                                COMMENT '设备型号ID',
    dealer_id       BIGINT        NOT NULL                 COMMENT '经销商ID',
    worker_id       BIGINT                                COMMENT '指派运维人员ID',
    worker_name     VARCHAR(64)                            COMMENT '运维人员姓名',
    order_status    VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '工单状态：PENDING/ASSIGNED/ACCEPTED/IN_PROGRESS/COMPLETED/CANCELLED',
    appoint_time    DATETIME(3)                            COMMENT '预约时间',
    accepted_at     DATETIME(3)                            COMMENT '接单时间',
    completed_at    DATETIME(3)                            COMMENT '完成时间',
    cancelled_at    DATETIME(3)                            COMMENT '取消时间',
    photo_urls      JSON                                  COMMENT '完成照片URL列表（MinIO）',
    remark          VARCHAR(512)                           COMMENT '完成备注',
    rating          TINYINT                                COMMENT '客户评分（1-5）',
    review_content  VARCHAR(512)                           COMMENT '客户评价内容',
    old_filter_id   VARCHAR(32)                            COMMENT '旧滤芯ID',
    new_filter_id   VARCHAR(32)                            COMMENT '新滤芯ID',
    trigger_type    VARCHAR(32)                            COMMENT '触发方式：AUTO（预警自动触发）/MANUAL（手动创建）',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    dispatch_notify_status  VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT '派单公众号通知状态：NONE/PENDING/PUSHED/SKIPPED/FAILED',
    dispatch_notify_message VARCHAR(256)                           COMMENT '派单公众号通知结果说明',
    dispatch_notified_at    DATETIME(3)                            COMMENT '派单公众号通知处理时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_device_id (device_id),
    KEY idx_customer_id (customer_id),
    KEY idx_dealer_id (dealer_id),
    KEY idx_worker_id (worker_id),
    KEY idx_order_status (order_status),
    KEY idx_order_type (order_type),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 6.2 工单流转日志
CREATE TABLE IF NOT EXISTS work_order_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    work_order_id   BIGINT        NOT NULL                 COMMENT '工单ID',
    from_status     VARCHAR(32)                            COMMENT '变更前状态',
    to_status       VARCHAR(32)   NOT NULL                 COMMENT '变更后状态',
    operator_id     BIGINT        NOT NULL                 COMMENT '操作人ID',
    operator_type   VARCHAR(16)   NOT NULL                 COMMENT '操作人类型：ADMIN/DEALER/WORKER/CUSTOMER',
    operator_name   VARCHAR(64)                            COMMENT '操作人姓名',
    remark          VARCHAR(512)                           COMMENT '备注',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_work_order_id (work_order_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转日志';

-- ============================================================
-- 七、订单管理模块
-- ============================================================

-- 7.1 订单表
CREATE TABLE IF NOT EXISTS order_info (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    order_no        VARCHAR(32)   NOT NULL                 COMMENT '订单编号（唯一）',
    customer_id     BIGINT        NOT NULL                 COMMENT '客户ID',
    customer_name   VARCHAR(64)                            COMMENT '客户姓名',
    customer_phone  VARCHAR(20)                            COMMENT '客户手机',
    customer_wechat VARCHAR(64)                            COMMENT '客户微信/昵称',
    product_name    VARCHAR(128)                           COMMENT '商品/套餐名称',
    package_id      BIGINT                                COMMENT '套餐ID',
    order_amount    BIGINT        NOT NULL                 COMMENT '订单金额（分）',
    face_value      BIGINT        NOT NULL                 COMMENT '面值（分）',
    pay_amount      BIGINT        NOT NULL                 COMMENT '实际支付金额（分）',
    pay_method      VARCHAR(32)                            COMMENT '支付方式：WECHAT_PAY/MANUAL_RECHARGE',
    order_status    VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING/PAID/CANCELLED/REFUNDING/REFUNDED/ERROR',
    commission_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '分账状态：PENDING/SETTLED',
    dealer_id       BIGINT                                COMMENT '经销商ID',
    dealer_name     VARCHAR(128)                           COMMENT '经销商名称',
    order_type      VARCHAR(32)   NOT NULL                 COMMENT '订单类型：RECHARGE/BUY_WATER',
    ordered_at      DATETIME(3)   NOT NULL                 COMMENT '下单时间',
    paid_at         DATETIME(3)                            COMMENT '支付时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer_id (customer_id),
    KEY idx_order_status (order_status),
    KEY idx_paid_at (paid_at),
    KEY idx_dealer_id (dealer_id),
    KEY idx_order_type (order_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
-- 八、支付与财务管理模块
-- ============================================================

-- 8.1 支付记录表
CREATE TABLE IF NOT EXISTS payment_record (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    order_no        VARCHAR(32)   NOT NULL                 COMMENT '订单编号',
    pay_method      VARCHAR(32)   NOT NULL                 COMMENT '支付方式',
    transaction_id  VARCHAR(64)                            COMMENT '第三方交易流水号',
    pay_amount      BIGINT        NOT NULL                 COMMENT '支付金额（分）',
    pay_status      VARCHAR(32)   NOT NULL                 COMMENT '支付状态：SUCCESS/FAIL/REFUND',
    paid_at         DATETIME(3)                            COMMENT '支付时间',
    raw_response    JSON                                  COMMENT '原始回调数据',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    KEY idx_order_no (order_no),
    KEY idx_paid_at (paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- 8.2 消费记录表（权责发生制基础）
CREATE TABLE IF NOT EXISTS consumption_record (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID',
    customer_id     BIGINT        NOT NULL                 COMMENT '客户ID',
    related_recharge_id BIGINT                             COMMENT '关联充值订单ID',
    consume_flow    BIGINT        NOT NULL                 COMMENT '本次消耗流量（升，×1000）',
    consume_amount  BIGINT        NOT NULL                 COMMENT '本次消耗金额（分）',
    revenue_amount  BIGINT        NOT NULL                 COMMENT '权责发生制营收（分，充赠按实际消耗比例计算）',
    consumed_at     DATETIME(3)   NOT NULL                 COMMENT '消费时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_customer_id (customer_id),
    KEY idx_consumed_at (consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费记录表（权责发生制基础）';

-- 8.3 分润记录表
CREATE TABLE IF NOT EXISTS commission_record (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    order_no        VARCHAR(32)   NOT NULL                 COMMENT '订单编号',
    dealer_id       BIGINT        NOT NULL                 COMMENT '经销商ID',
    dealer_level    VARCHAR(16)   NOT NULL                 COMMENT '经销商层级',
    order_amount    BIGINT        NOT NULL                 COMMENT '订单金额（分）',
    commission_rate DECIMAL(5,4)  NOT NULL                 COMMENT '分润比例',
    commission_amount BIGINT      NOT NULL                 COMMENT '分润金额（分）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SETTLED',
    settled_at      DATETIME(3)                            COMMENT '结算时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_order_no (order_no),
    KEY idx_dealer_id (dealer_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分润记录表';

-- 8.4 结算账单表
CREATE TABLE IF NOT EXISTS settlement_bill (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    bill_no         VARCHAR(32)   NOT NULL                 COMMENT '账单编号（唯一）',
    dealer_id       BIGINT        NOT NULL                 COMMENT '经销商ID',
    bill_start_date DATE          NOT NULL                 COMMENT '账单开始日期',
    bill_end_date   DATE          NOT NULL                 COMMENT '账单结束日期（15天周期）',
    total_amount    BIGINT        NOT NULL                 COMMENT '本期总金额（分）',
    commission_amount BIGINT      NOT NULL                 COMMENT '本期分润金额（分）',
    withdraw_amount BIGINT       NOT NULL DEFAULT 0        COMMENT '已提现金额（分）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SETTLED/WITHDRAWN',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no),
    KEY idx_dealer_id (dealer_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算账单表（15天周期）';

-- 8.5 发票表
CREATE TABLE IF NOT EXISTS invoice (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    invoice_no      VARCHAR(32)   NOT NULL                 COMMENT '发票编号（唯一）',
    customer_id     BIGINT        NOT NULL                 COMMENT '客户ID',
    amount          BIGINT        NOT NULL                 COMMENT '发票金额（分）',
    title           VARCHAR(256)  NOT NULL                 COMMENT '发票抬头',
    tax_no          VARCHAR(32)                            COMMENT '税号',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ISSUED/MAILED',
    issued_at       DATETIME(3)                            COMMENT '开票时间',
    mailed_at       DATETIME(3)                            COMMENT '邮寄时间',
    mailing_address VARCHAR(256)                           COMMENT '邮寄地址',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_no (invoice_no),
    KEY idx_customer_id (customer_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票表';

-- 8.6 退款记录表
CREATE TABLE IF NOT EXISTS refund_record (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    order_no        VARCHAR(32)   NOT NULL                 COMMENT '原订单编号',
    refund_no       VARCHAR(32)   NOT NULL                 COMMENT '退款编号（唯一）',
    refund_amount   BIGINT       NOT NULL                 COMMENT '退款金额（分）',
    refund_reason   VARCHAR(512)                           COMMENT '退款原因',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/COMPLETED',
    operator_id     BIGINT                                COMMENT '操作人ID',
    operator_name   VARCHAR(64)                            COMMENT '操作人姓名',
    processed_at    DATETIME(3)                            COMMENT '处理时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_order_no (order_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- ============================================================
-- 九、套餐管理模块
-- ============================================================

-- 9.1 套餐表
CREATE TABLE IF NOT EXISTS package (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    package_code    VARCHAR(32)   NOT NULL                 COMMENT '套餐编号（唯一）',
    package_name    VARCHAR(128)  NOT NULL                 COMMENT '套餐名称',
    package_type    VARCHAR(32)   NOT NULL                 COMMENT '套餐类型：QR_SCAN/SHARED/RENTAL/WALLET/INSTALL',
    price           BIGINT        NOT NULL                 COMMENT '套餐价格（分）',
    face_value      BIGINT                                 COMMENT '面值（分，项目钱包类型使用）',
    flow_quota      BIGINT                                 COMMENT '套餐流量配额（升，×1000）',
    duration_days   INT                                    COMMENT '套餐时长（天）',
    charge_mode     VARCHAR(32)                            COMMENT '计费模式',
    model_id        BIGINT                                 COMMENT '适用设备型号ID（null 表示通用）',
    photo_url       VARCHAR(256)                           COMMENT '套餐图片URL',
    commission_amount BIGINT                               COMMENT '固定分佣金额（分）',
    commission_rate  DECIMAL(5,4)                          COMMENT '分佣比例',
    cold_water_rate  BIGINT                                COMMENT '冷水费率（分/升×1000，共享套餐使用）',
    hot_water_rate   BIGINT                                COMMENT '热水费率（分/升×1000，共享套餐使用）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_package_code (package_code),
    KEY idx_package_type (package_type),
    KEY idx_model_id (model_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐表';

-- ============================================================
-- 十、库存管理模块
-- ============================================================

-- 10.1 设备库存表
CREATE TABLE IF NOT EXISTS device_stock (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID',
    batch_no        VARCHAR(64)                            COMMENT '进货批次号',
    warehouse_type  VARCHAR(32)   NOT NULL                 COMMENT '仓库类型：FACTORY/DEALER',
    warehouse_id    BIGINT                                 COMMENT '仓库ID（经销商ID或厂家仓库ID=0）',
    stock_status    VARCHAR(16)   NOT NULL                 COMMENT '库存状态：IN_STOCK/OUT_STOCK/TRANSFERRING',
    inbound_at      DATETIME(3)                            COMMENT '入库时间',
    outbound_at     DATETIME(3)                            COMMENT '出库时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_batch_no (batch_no),
    KEY idx_warehouse (warehouse_type, warehouse_id),
    KEY idx_stock_status (stock_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备库存表';

-- 10.2 设备库存流水
CREATE TABLE IF NOT EXISTS device_stock_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '设备ID',
    batch_no        VARCHAR(64)                            COMMENT '批次号',
    from_warehouse  VARCHAR(32)                            COMMENT '来源仓库',
    to_warehouse    VARCHAR(32)                            COMMENT '目标仓库',
    operation       VARCHAR(32)   NOT NULL                 COMMENT '操作类型：INBOUND/OUTBOUND/TRANSFER/DISTRIBUTE',
    quantity        INT           NOT NULL DEFAULT 1        COMMENT '数量',
    operator_id     BIGINT                                 COMMENT '操作人ID',
    operator_name   VARCHAR(64)                            COMMENT '操作人姓名',
    remark          VARCHAR(512)                           COMMENT '备注',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_batch_no (batch_no),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备库存流水记录';

-- 10.3 滤芯库存表
CREATE TABLE IF NOT EXISTS filter_stock (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    filter_id       VARCHAR(32)   NOT NULL                 COMMENT '滤芯ID',
    batch_no        VARCHAR(64)                            COMMENT '进货批次号',
    warehouse_type  VARCHAR(32)   NOT NULL                 COMMENT '仓库类型：FACTORY/DEALER',
    warehouse_id    BIGINT                                 COMMENT '仓库ID',
    stock_status    VARCHAR(16)   NOT NULL                 COMMENT '库存状态：IN_STOCK/OUT_STOCK/TRANSFERRING',
    inbound_at      DATETIME(3)                            COMMENT '入库时间',
    outbound_at     DATETIME(3)                            COMMENT '出库时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_filter_id (filter_id),
    KEY idx_batch_no (batch_no),
    KEY idx_warehouse (warehouse_type, warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='滤芯库存表';

-- 10.4 进货批次表
CREATE TABLE IF NOT EXISTS stock_batch (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batch_no        VARCHAR(64)   NOT NULL                 COMMENT '批次号（唯一）',
    batch_type      VARCHAR(16)   NOT NULL                 COMMENT '批次类型：DEVICE/FILTER',
    product_type    VARCHAR(64)                            COMMENT '产品类型（型号编码或滤芯型号编码）',
    quantity        INT           NOT NULL                 COMMENT '数量',
    import_method   VARCHAR(16)   NOT NULL                 COMMENT '录入方式：MANUAL/EXCEL_IMPORT/CSV_IMPORT',
    import_file_url VARCHAR(256)                           COMMENT '导入文件URL（MinIO）',
    manufacturer    VARCHAR(128)                           COMMENT '生产厂家',
    produced_at     DATE                                   COMMENT '生产日期',
    remark          VARCHAR(512)                           COMMENT '备注',
    operator_id     BIGINT                                 COMMENT '操作人ID',
    operator_name   VARCHAR(64)                            COMMENT '操作人姓名',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_batch_type (batch_type),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货批次表';

-- ============================================================
-- 十一、IoT 数据模块
-- ============================================================

-- 11.1 指令下发日志
CREATE TABLE IF NOT EXISTS command_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    message_id      VARCHAR(36)   NOT NULL                 COMMENT 'MQTT messageID（UUID）',
    sn              VARCHAR(32)   NOT NULL                 COMMENT '网关序列号',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '平台设备ID',
    point_id        VARCHAR(8)    NOT NULL                 COMMENT '目标测点ID（如 Q3）',
    value           VARCHAR(64)   NOT NULL                 COMMENT '下发值（String）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENT/EXECUTED/TIMEOUT/FAILED',
    operator_id     BIGINT                                COMMENT '操作人ID',
    ack_received_at DATETIME(3)                            COMMENT 'ACK接收时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_sn (sn),
    KEY idx_device_id (device_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQTT指令下发日志';

-- 11.2 设备上下线日志
CREATE TABLE IF NOT EXISTS device_online_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    sn              VARCHAR(32)   NOT NULL                 COMMENT '网关序列号',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '平台设备ID',
    event_type      VARCHAR(8)    NOT NULL                 COMMENT '事件类型：ONLINE/OFFLINE',
    occurred_at     DATETIME(3)   NOT NULL                 COMMENT '发生时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sn (sn),
    KEY idx_device_id (device_id),
    KEY idx_event_type (event_type),
    KEY idx_occurred_at (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备上下线日志（LWT触发）';

-- 11.3 设备告警表
CREATE TABLE IF NOT EXISTS device_alert (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    device_id       VARCHAR(32)   NOT NULL                 COMMENT '平台设备ID',
    sn              VARCHAR(32)   NOT NULL                 COMMENT '网关序列号',
    alert_type      VARCHAR(32)   NOT NULL                 COMMENT '告警类型：DEVICE_FAULT/WATER_LEAK/WATER_SHORTAGE/LOW_VOLTAGE/WATER_QUALITY/FILTER_EXPIRE/FLOW_EXPIRE/RENT_EXPIRE',
    alert_level     VARCHAR(16)   NOT NULL                 COMMENT '告警级别：WARNING/ALARM',
    alert_message   VARCHAR(512)  NOT NULL                 COMMENT '告警消息',
    push_status     VARCHAR(16)   NOT NULL DEFAULT 'UNPUSHED' COMMENT '推送状态：UNPUSHED/PUSHED',
    handled_status  VARCHAR(16)   NOT NULL DEFAULT 'UNHANDLED' COMMENT '处理状态：UNHANDLED/HANDLED',
    auto_work_order_id BIGINT                              COMMENT '自动生成的工单ID',
    related_filter_id VARCHAR(32)                           COMMENT '关联滤芯ID（滤芯到期告警时）',
    triggered_at    DATETIME(3)   NOT NULL                 COMMENT '触发时间',
    pushed_at       DATETIME(3)                            COMMENT '推送时间',
    handled_at      DATETIME(3)                            COMMENT '处理时间',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by      BIGINT                                 COMMENT '创建人ID',
    updated_by      BIGINT                                 COMMENT '更新人ID',
    deleted         TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除：0未删，1已删',
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_sn (sn),
    KEY idx_alert_type (alert_type),
    KEY idx_alert_level (alert_level),
    KEY idx_push_status (push_status),
    KEY idx_triggered_at (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备告警表';

-- ============================================================
-- 十二、系统管理模块
-- ============================================================

-- 12.1 系统角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    role_code       VARCHAR(32)   NOT NULL                 COMMENT '角色编码',
    role_name       VARCHAR(64)   NOT NULL                 COMMENT '角色名称',
    role_desc       VARCHAR(256)                           COMMENT '角色描述',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 12.2 后台账户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_no     VARCHAR(16)   NOT NULL                 COMMENT '工号（唯一）',
    username        VARCHAR(64)   NOT NULL                 COMMENT '登录账号',
    password        VARCHAR(128)  NOT NULL                 COMMENT '密码（BCrypt加密）',
    role_id         BIGINT        NOT NULL                 COMMENT '角色ID',
    name            VARCHAR(64)                            COMMENT '姓名',
    phone           VARCHAR(20)                            COMMENT '电话',
    email           VARCHAR(128)                           COMMENT '邮箱',
    wechat          VARCHAR(64)                            COMMENT '微信',
    department      VARCHAR(128)                           COMMENT '部门',
    last_login_ip   VARCHAR(45)                            COMMENT '最近登录IP',
    last_login_at   DATETIME(3)                            COMMENT '最近登录时间',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_no (employee_no),
    UNIQUE KEY uk_username (username),
    KEY idx_role_id (role_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台账户表';

-- 12.3 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    trace_id        VARCHAR(64)   NOT NULL                 COMMENT '链路追踪ID',
    operator_id     BIGINT                                 COMMENT '操作人ID',
    operator_type   VARCHAR(16)                            COMMENT '操作人类型',
    operator_name   VARCHAR(64)                            COMMENT '操作人姓名',
    operation       VARCHAR(64)   NOT NULL                 COMMENT '操作类型：DEVICE_BIND/DEVICE_UNBIND/ORDER_PAY/REMOTE_CONTROL/DATA_EXPORT/FILTER_INSTALL/FILTER_REMOVE/...',
    target_type     VARCHAR(32)                            COMMENT '操作目标类型',
    target_id       VARCHAR(64)                            COMMENT '操作目标ID',
    request_ip      VARCHAR(45)                            COMMENT '请求IP',
    request_url     VARCHAR(256)                           COMMENT '请求URL',
    request_method  VARCHAR(16)                            COMMENT '请求方法',
    request_params  TEXT                                   COMMENT '请求参数（脱敏后）',
    response_code   INT                                    COMMENT '响应状态码',
    cost_time       INT                                    COMMENT '耗时（ms）',
    result          VARCHAR(16)                            COMMENT '结果：SUCCESS/FAIL',
    error_msg       VARCHAR(1024)                          COMMENT '错误信息',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_operator_id (operator_id),
    KEY idx_operation (operation),
    KEY idx_created_at (created_at),
    KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表（操作日志全审计）';

-- 12.4 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    config_key      VARCHAR(64)   NOT NULL                 COMMENT '配置键（唯一）',
    config_value    VARCHAR(1024) NOT NULL                 COMMENT '配置值',
    config_desc     VARCHAR(256)                           COMMENT '配置描述',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 12.5 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    permission_code VARCHAR(64)   NOT NULL                 COMMENT '权限编码（唯一）',
    permission_name VARCHAR(64)   NOT NULL                 COMMENT '权限名称',
    permission_type VARCHAR(16)   NOT NULL                 COMMENT '权限类型：MENU/BUTTON/API',
    parent_id       BIGINT                                 COMMENT '父权限ID（树形结构）',
    path            VARCHAR(256)                           COMMENT '前端路由路径 / API路径',
    icon            VARCHAR(64)                            COMMENT '菜单图标',
    sort_order      INT           NOT NULL DEFAULT 0        COMMENT '排序',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_parent_id (parent_id),
    KEY idx_permission_type (permission_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 12.6 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    role_id         BIGINT        NOT NULL                 COMMENT '角色ID',
    permission_id   BIGINT        NOT NULL                 COMMENT '权限ID',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

-- ============================================================
-- 十三、告警阈值配置表
-- ============================================================

CREATE TABLE IF NOT EXISTS alert_threshold (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    threshold_key   VARCHAR(64)   NOT NULL                 COMMENT '阈值键',
    threshold_value VARCHAR(128)  NOT NULL                 COMMENT '阈值值',
    related_point   VARCHAR(8)                             COMMENT '关联测点ID',
    description     VARCHAR(256)                           COMMENT '描述',
    enabled         TINYINT(1)    NOT NULL DEFAULT 1       COMMENT '是否启用：0=禁用 1=启用',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_threshold_key (threshold_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警阈值配置表';

-- ============================================================
-- ============================================================
-- 种子数据
-- ============================================================
-- ============================================================

-- ---- 系统角色 ----
INSERT INTO sys_role (role_code, role_name, role_desc) VALUES
('SUPER_ADMIN', '超级管理员', '拥有全部权限'),
('ADMIN', '平台管理员', '平台后台运营人员'),
('DEALER', '经销商', '经销商层级账号'),
('WORKER', '运维人员', '安装维修师傅'),
('CUSTOMER', '客户', '终端消费者');

-- ---- 后台超级管理员账户（密码：admin123，BCrypt 加密）----
INSERT INTO sys_user (employee_no, username, password, role_id, name, phone, email, status) VALUES
('000001', 'admin', '$2a$10$N.ZOn9G6/YLFixAkp0rTnKqLG8AK8Q5bUgv2SEddUJnVtD9YqK8KO', 1, '超级管理员', '13800000000', 'admin@drinking-water.com', 'ENABLED');

-- ---- 系统配置初始化 ----
INSERT INTO sys_config (config_key, config_value, config_desc) VALUES
('SYSTEM_NAME', '直饮水平台', '系统名称'),
('CUSTOMER_SERVICE_PHONE', '400-000-0000', '客服电话'),
('DEFAULT_PASSWORD', '123456', '新建账号初始密码'),
('SETTLEMENT_CYCLE_DAYS', '15', '结算周期（天）'),
('FILTER_WARNING_DAYS', '15', '滤芯预警天数（寿命<此值开始预警）'),
('FILTER_ALARM_DAYS', '10', '滤芯报警天数（寿命<此值升级为报警）'),
('DEVICE_HEARTBEAT_INTERVAL', '60', '设备心跳间隔（秒）'),
('DEVICE_OFFLINE_THRESHOLD', '180', '设备离线判定阈值（秒）'),
('MQTT_BROKER_HOST', 'emqx', 'MQTT Broker 服务器地址（容器内服务名）'),
('MQTT_BROKER_PORT', '1883', 'MQTT Broker TCP 端口'),
('MQTT_BROKER_USERNAME', 'iotscc', 'MQTT Broker 认证用户名'),
('MQTT_BROKER_PASSWORD', 'iotscc', 'MQTT Broker 认证密码'),
('MQTT_KEEPALIVE', '120', 'MQTT KeepAlive（秒）'),
('ALERT_TDS_THRESHOLD', '50', '纯水TDS告警阈值（PPM）'),
('ALERT_PRESSURE_LOW_BAR', '2.0', '增压泵后压力告警阈值（bar）');

-- ---- 告警阈值初始化 ----
INSERT INTO alert_threshold (threshold_key, threshold_value, related_point, description) VALUES
('ALERT_TDS_THRESHOLD', '50', 'P2', '纯水TDS超过此值告警'),
('ALERT_PRESSURE_LOW_BAR', '2.0', 'P36', '压力低于此值告警'),
('ALERT_DEVICE_FAULT', '1', 'P28', '设备故障报警'),
('ALERT_WATER_LEAK', '1', 'P33', '漏水告警'),
('ALERT_WATER_SHORTAGE', '1', 'P32', '缺水告警'),
('ALERT_LOW_VOLTAGE', '1', 'P34', '电压低预警'),
('FILTER_WARNING_PERCENT', '15', NULL, '滤芯剩余寿命百分比预警阈值'),
('FILTER_ALARM_PERCENT', '10', NULL, '滤芯剩余寿命百分比报警阈值');

-- ---- 权限初始化（核心权限）----
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, path, sort_order, status) VALUES
('DASHBOARD', '仪表盘', 'MENU', 0, '/dashboard', 1, 'ENABLED'),
('DEVICE', '设备管理', 'MENU', 0, '/devices', 2, 'ENABLED'),
('DEVICE_VIEW', '设备查看', 'API', 2, '/api/v1/devices', 1, 'ENABLED'),
('DEVICE_EDIT', '设备编辑', 'API', 2, '/api/v1/devices/*', 2, 'ENABLED'),
('FILTER', '滤芯管理', 'MENU', 0, '/filters', 3, 'ENABLED'),
('CUSTOMER', '客户管理', 'MENU', 0, '/customers', 4, 'ENABLED'),
('DEALER', '经销商管理', 'MENU', 0, '/dealers', 5, 'ENABLED'),
('WORKER', '运维人员', 'MENU', 0, '/workers', 6, 'ENABLED'),
('WORK_ORDER', '工单管理', 'MENU', 0, '/work-orders', 7, 'ENABLED'),
('ORDER', '订单管理', 'MENU', 0, '/orders', 8, 'ENABLED'),
('PACKAGE', '套餐管理', 'MENU', 0, '/packages', 9, 'ENABLED'),
('INVENTORY', '库存管理', 'MENU', 0, '/inventory', 10, 'ENABLED'),
('FINANCE', '财务管理', 'MENU', 0, '/finance', 11, 'ENABLED'),
('REPORT', '报表中心', 'MENU', 0, '/reports', 12, 'ENABLED'),
('SYSTEM', '系统管理', 'MENU', 0, '/system', 13, 'ENABLED'),
('AI_ASSISTANT_VIEW', 'AI 助手使用', 'API', 0, '/api/v1/ai/assistant/**', 99, 'ENABLED'),
('SYSTEM_USER', '账户管理', 'API', 15, '/api/v1/system/users', 1, 'ENABLED'),
('SYSTEM_ROLE', '角色管理', 'API', 15, '/api/v1/system/roles', 2, 'ENABLED'),
('SYSTEM_CONFIG', '系统配置', 'API', 15, '/api/v1/system/configs', 3, 'ENABLED'),
('SYSTEM_AUDIT', '审计日志', 'API', 15, '/api/v1/system/audit-logs', 4, 'ENABLED');

-- ---- 超级管理员角色绑定全部权限 ----
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE status = 'ENABLED';

-- ---- 经销商角色绑定查看权限 ----
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission 
WHERE permission_code IN ('DASHBOARD','DEVICE','DEVICE_VIEW','FILTER','CUSTOMER','WORKER','WORK_ORDER','ORDER','PACKAGE','INVENTORY','FINANCE','REPORT');

-- ============================================================
-- 脚本结束
-- ============================================================
