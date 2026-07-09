<template>
  <div class="statistics-container">
    <div class="statistics-header">
      <h2>专家行为统计报表</h2>
      <div class="filter-bar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="margin-right: 16px;"
        />
        <el-button type="primary" @click="fetchAllExports">查询</el-button>
      </div>
    </div>
    <el-row :gutter="24">
      <el-col :span="12">
        <el-card>
          <h3>综合统计（所有会话）</h3>
          <el-table :data="simpleExport.expertInterventionExport" style="margin-bottom: 16px;">
            <el-table-column prop="type" label="行为类型" />
            <el-table-column prop="count" label="次数" />
          </el-table>
          <el-table :data="simpleExport.expertDecisionTreeExport">
            <el-table-column prop="type" label="决策树行为类型" />
            <el-table-column prop="count" label="次数" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h3>明细统计（按会话）</h3>
          <el-table :data="detailExport.expertInterventionExport">
            <el-table-column prop="chatId" label="会话ID" width="100" />
            <el-table-column label="行为明细">
              <template #default="scope">
                <el-table :data="scope.row.export" size="small" border>
                  <el-table-column prop="type" label="类型" width="180" />
                  <el-table-column prop="count" label="次数" width="80" />
                </el-table>
              </template>
            </el-table-column>
          </el-table>
          <el-table :data="detailExport.expertDecisionTreeExport" style="margin-top: 16px;">
            <el-table-column prop="type" label="决策树行为类型" />
            <el-table-column prop="count" label="次数" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getExpertExportSimple, getExpertExportDetail } from '@/api/chat'
import { ElMessage } from 'element-plus'

const dateRange = ref([])
const simpleExport = ref({ expertInterventionExport: [], expertDecisionTreeExport: [] })
const detailExport = ref({ expertInterventionExport: [], expertDecisionTreeExport: [] })

const fetchAllExports = async () => {
  try {
    const params = {
      start_time: dateRange.value?.[0] || '',
      end_time: dateRange.value?.[1] || ''
    }
    const [simpleRes, detailRes] = await Promise.all([
      getExpertExportSimple(params),
      getExpertExportDetail(params)
    ])
    if (simpleRes.code === 200) simpleExport.value = simpleRes.data
    if (detailRes.code === 200) detailExport.value = detailRes.data
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  }
}

// 页面加载时自动拉取全部
fetchAllExports()
</script>

<style scoped>
.statistics-container {
  padding: 32px;
  background: #f5f7fa;
  min-height: 100vh;
}
.statistics-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.filter-bar {
  display: flex;
  align-items: center;
}
</style> 