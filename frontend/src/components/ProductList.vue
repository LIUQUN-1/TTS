<template>
  <div class="product-list-container">
    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="18" :md="18" :lg="18" :xl="18">
          <el-space wrap>
            <el-button type="primary" :icon="Plus" @click="showAddDialog">
              批量新增商品
            </el-button>
            <el-button type="success" :icon="Refresh" @click="triggerCheck" :loading="checkLoading">
              刷新状态
            </el-button>
          </el-space>
        </el-col>
        <el-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6" class="statistic-col">
          <el-statistic title="监控商品总数" :value="totalCount" />
        </el-col>
      </el-row>
    </el-card>

    <!-- 筛选条件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryParams" @submit.prevent="handleSearch" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="queryParams.isValid" placeholder="全部" clearable class="filter-select">
            <el-option label="有效" :value="1" />
            <el-option label="失效" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="确认状态">
          <el-select v-model="queryParams.confirmStatus" placeholder="全部" clearable class="filter-select">
            <el-option label="待处理" :value="0" />
            <el-option label="已确认" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="销售地区">
          <el-input v-model="queryParams.saleRegion" placeholder="如: IDR" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="商品ID">
          <el-input v-model="queryParams.keyword" placeholder="搜索商品ID" clearable class="filter-input-large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 商品列表表格 -->
    <el-card class="table-card" shadow="never">
      <el-table 
        :data="tableData" 
        v-loading="loading"
        stripe
        class="product-table"
      >
        <el-table-column prop="productId" label="商品ID" min-width="180" fixed />
        <el-table-column prop="title" label="商品标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="shopName" label="店铺名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="saleRegion" label="销售地区" min-width="100" align="center" />
        <el-table-column prop="isValid" label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isValid === 1 ? 'success' : 'danger'">
              {{ row.isValid === 1 ? '有效' : '失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confirmStatus" label="确认状态" min-width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.isValid === 1">-</span>
            <el-tag v-else :type="row.confirmStatus === 1 ? 'info' : 'warning'">
              {{ row.confirmStatus === 1 ? '已确认' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="佣金信息" min-width="220">
          <template #default="{ row }">
            <span v-if="row.commissionRate">
              佣金: {{ row.commissionRate }} / 金额: {{ row.commissionAmount }} 币种: {{ row.commissionCurrency }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastCheckTime" label="最后校验时间" min-width="170" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.isValid === 0 && row.confirmStatus === 0"
              type="warning" 
              size="small" 
              link
              @click="handleConfirm(row)"
            >
              确认失效
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增商品对话框 -->
    <el-dialog 
      v-model="addDialogVisible" 
      title="批量新增监控商品" 
      class="add-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="商品ID列表">
          <el-input
            v-model="addForm.productIdsText"
            type="textarea"
            :rows="8"
            placeholder="请输入商品ID，每行一个或用逗号分隔"
          />
          <div class="form-tip">支持多种格式：每行一个ID，或用逗号、分号、空格分隔</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="addLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { productApi, taskApi } from '../api'

// 查询参数
const queryParams = reactive({
  page: 1,
  size: 20,
  isValid: undefined,
  saleRegion: '',
  keyword: '',
  confirmStatus: undefined
})

// 表格数据
const tableData = ref([])
const total = ref(0)
const totalCount = ref(0)
const loading = ref(false)

// 校验任务
const checkLoading = ref(false)

// 新增对话框
const addDialogVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({
  productIdsText: ''
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await productApi.getList(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载商品总数
const loadCount = async () => {
  try {
    const res = await productApi.getCount()
    totalCount.value = res.data
  } catch (error) {
    console.error('加载总数失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  queryParams.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryParams.page = 1
  queryParams.size = 20
  queryParams.isValid = undefined
  queryParams.saleRegion = ''
  queryParams.keyword = ''
  queryParams.confirmStatus = undefined
  loadData()
}

// 分页变化
const handlePageChange = () => {
  loadData()
}

const handleSizeChange = () => {
  queryParams.page = 1
  loadData()
}

// 显示新增对话框
const showAddDialog = () => {
  addForm.productIdsText = ''
  addDialogVisible.value = true
}

// 批量新增商品
const handleAdd = async () => {
  const text = addForm.productIdsText.trim()
  if (!text) {
    ElMessage.warning('请输入商品ID')
    return
  }

  // 解析商品ID列表（支持多种分隔符）
  const productIds = text
    .split(/[\n,，;；\s]+/)
    .map(id => id.trim())
    .filter(id => id.length > 0)

  if (productIds.length === 0) {
    ElMessage.warning('未识别到有效的商品ID')
    return
  }

  addLoading.value = true
  try {
    const res = await productApi.addProducts(productIds)
    ElMessage.success(res.message || `成功新增 ${res.data} 个商品`)
    addDialogVisible.value = false
    loadData()
    loadCount()
  } catch (error) {
    console.error('新增失败:', error)
  } finally {
    addLoading.value = false
  }
}

// 确认失效
const handleConfirm = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认将商品"${row.title}"标记为已确认失效状态？`,
      '确认操作',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await productApi.confirmInvalid(row.productId)
    ElMessage.success('确认成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认失败:', error)
    }
  }
}

// 删除商品
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除商品"${row.title}"吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await productApi.deleteProduct(row.productId)
    ElMessage.success('删除成功')
    loadData()
    loadCount()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 手动触发校验
const triggerCheck = async () => {
  checkLoading.value = true
  try {
    const res = await taskApi.triggerCheck()
    ElMessage.success(res.data?.message || '校验任务已提交')
  } catch (error) {
    console.error('触发校验失败:', error)
  } finally {
    checkLoading.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadData()
  loadCount()
})
</script>

<style scoped>
.product-list-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar-card,
.filter-card,
.table-card {
  margin-bottom: 16px;
}

.toolbar-card,
.filter-card {
  flex-shrink: 0;
}

.table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-table {
  flex: 1;
  width: 100%;
}

.statistic-col {
  text-align: right;
}

@media (max-width: 768px) {
  .statistic-col {
    text-align: left;
    margin-top: 10px;
  }
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-select {
  width: 120px;
  min-width: 100px;
}

.filter-input {
  width: 120px;
  min-width: 100px;
}

.filter-input-large {
  width: 200px;
  min-width: 150px;
}

@media (max-width: 768px) {
  .filter-select,
  .filter-input {
    width: 100%;
    min-width: unset;
  }
  
  .filter-input-large {
    width: 100%;
    min-width: unset;
  }
  
  .filter-form .el-form-item {
    width: 100%;
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .pagination-container {
    justify-content: center;
  }
}

.add-dialog {
  --el-dialog-width: 90vw;
  max-width: 600px;
}

@media (max-width: 768px) {
  .add-dialog {
    --el-dialog-width: 95vw;
    max-width: 95vw;
  }
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
