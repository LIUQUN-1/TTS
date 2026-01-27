import request from '../utils/request'

const BASE_URL = '/TTS/monitor/products'

/**
 * 商品监控相关API
 */
export const productApi = {
  /**
   * 获取商品监控列表（分页）
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码
   * @param {number} params.size - 每页条数
   * @param {number} params.isValid - 状态筛选（1-有效，0-失效）
   * @param {string} params.saleRegion - 销售国家筛选
   * @param {string} params.keyword - 搜索关键词
   * @param {number} params.confirmStatus - 确认状态（0-待处理，1-已确认）
   */
  getList(params) {
    return request({
      url: BASE_URL,
      method: 'get',
      params
    })
  },

  /**
   * 批量新增监控商品
   * @param {Array<string>} productIds - 商品ID列表
   */
  addProducts(productIds) {
    return request({
      url: BASE_URL + '/add',
      method: 'post',
      data: {productIds}
    })
  },

  /**
   * 删除监控商品
   * @param {string} productId - 商品ID
   */
  deleteProduct(productId) {
    return request({
      url: `${BASE_URL}/${productId}`,
      method: 'delete'
    })
  },

  /**
   * 确认商品失效
   * @param {string} productId - 商品ID
   */
  confirmInvalid(productId) {
    return request({
      url: `${BASE_URL}/${productId}/confirm`,
      method: 'post'
    })
  },

  /**
   * 统计商品总数
   */
  getCount() {
    return request({
      url: `${BASE_URL}/count`,
      method: 'get'
    })
  }
}

/**
 * 任务控制相关API
 */
export const taskApi = {
  /**
   * 手动触发商品校验任务
   */
  triggerCheck() {
    return request({
      url: '/TTS/monitor/task/trigger',
      method: 'post'
    })
  }
}
