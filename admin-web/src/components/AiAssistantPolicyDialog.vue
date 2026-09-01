<template>
  <el-dialog v-model="visible" :title="AI_ASSISTANT_POLICY_TITLE" width="min(720px, calc(100vw - 32px))" append-to-body destroy-on-close>
    <el-alert
      title="运营方：贵州省聚控云科技有限公司。请在登录前仔细阅读并确认本用户协议与隐私政策。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-tabs class="policy-tabs" stretch>
      <el-tab-pane label="用户协议">
        <section class="policy-section">
          <h4>一、适用范围与服务内容</h4>
          <p>本协议适用于直饮水平台管理后台及其提供的设备管理、客户与工单管理、订单与报表、监控预警、系统配置和后台 AI 辅助功能。您应以受授权的后台账户使用平台，并遵守适用的法律法规、组织管理制度和平台业务规则。</p>
          <h4>二、账户与权限</h4>
          <p>请妥善保管账户、密码和登录凭证，不得出借、转让或与无授权人员共用。平台按角色分配菜单、页面和数据权限，并记录必要的登录与操作日志。因账户保管不当或超出授权范围使用平台造成的风险，应由相关责任方承担。</p>
          <h4>三、业务操作规则</h4>
          <p>设备控制、派单、退款、价格配置、权限调整及其他可能影响设备、资金或业务数据的操作，应由具备相应权限的人员在平台页面核对后人工执行。平台展示的数据、状态和建议应结合现场设备、业务单据和人工复核结果使用。</p>
          <h4>四、后台 AI 辅助功能</h4>
          <p>后台 AI 助手仅用于解释系统操作、汇总当前账户已获授权的数据及提供辅助分析建议。AI 不会自动控制设备、创建或修改订单、派单、退款或变更系统配置。AI 生成内容会在界面中标注“本内容由 AI 生成”，仅供参考，不替代专业判断或人工审批。</p>
          <h4>五、服务调整与协议更新</h4>
          <p>平台可能因维护、升级、安全处置或业务调整临时变更部分功能。协议或隐私政策发生实质更新时，平台将更新版本并要求重新确认；继续使用相关功能即表示您同意在授权范围内遵守更新后的规则。</p>
        </section>
      </el-tab-pane>
      <el-tab-pane label="隐私政策">
        <section class="policy-section">
          <h4>一、信息处理主体与目的</h4>
          <p>贵州省聚控云科技有限公司作为直饮水平台运营方，为提供账户登录、权限控制、设备运维、工单与订单处理、报表统计、安全审计、问题处理和客户支持等服务，在必要范围内处理相关信息。</p>
          <h4>二、处理的信息范围</h4>
          <p>平台可能处理后台账户标识、角色权限、姓名、联系方式（如由管理员录入）、登录状态与最近登录时间、系统操作记录，以及在履职过程中产生的设备、客户、工单、订单和报表数据。后台 AI 审计仅记录调用人、时间、模型、数据工具、结果状态及提问和回答的脱敏摘要，不保存完整提问和完整回答。</p>
          <h4>三、AI 模型数据处理</h4>
          <p>使用后台 AI 助手时，平台仅会向第三方生成模型服务发送经统一脱敏的问题、经权限过滤的静态知识片段及字段白名单内的授权汇总数据。不会发送密码、支付密钥、应用密钥、证书、访问令牌、完整手机号、完整身份证号码、完整住址、OpenID、UnionID、银行卡号、完整微信交易号或完整原始审计日志。</p>
          <h4>四、留存、安全与共享</h4>
          <p>平台采取权限隔离、访问控制、审计记录和必要的安全措施保护数据。除为提供 AI 辅助服务而按前述范围委托模型服务处理外，平台不会出售个人信息。后台 AI 调用审计、协议同意记录和投诉处理记录在账户删除或注销后仍至少保留六个月，期满后按平台数据清理规则处理。</p>
          <h4>五、您的权利与反馈渠道</h4>
          <p>您可在授权范围内查询、更正相关账户信息，并可通过系统提供的账户注销路径提交注销申请。对 AI 回答、数据使用或隐私处理存在异议时，可通过 AI 助手中的“投诉建议”入口或公司已公布的管理员联系渠道反馈。为履行法定义务、处理争议或保障平台安全，部分必要记录可能在法定或约定留存期内继续保存。</p>
          <h4>六、版本信息</h4>
          <p>当前版本：{{ AI_ASSISTANT_POLICY_VERSION }}；生效日期：2026 年 8 月 26 日。</p>
        </section>
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button type="primary" @click="visible = false">我已知悉</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { AI_ASSISTANT_POLICY_TITLE, AI_ASSISTANT_POLICY_VERSION } from '@/constants/aiAssistantPolicy'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
</script>

<style scoped lang="scss">
.policy-tabs { margin-top: 18px; }
.policy-section { color: #425466; font-size: 14px; line-height: 1.8; }
.policy-section h4 { margin: 4px 0 6px; color: #1f2d3d; font-size: 14px; }
.policy-section p { margin: 0 0 14px; }

:deep(.el-dialog__body) {
  max-height: min(62vh, 640px);
  overflow-y: auto;
}
</style>
