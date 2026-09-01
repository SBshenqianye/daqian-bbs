/**
 * 分页逻辑 Mixin
 * 提供通用的分页状态管理和方法，可被任何需要分页的组件复用
 *
 * 使用示例：
 * <script>
 * import paginationMixin from '@/mixins/pagination'
 *
 * export default {
 *   mixins: [paginationMixin],
 *   methods: {
 *     fetchData() {
 *       return this.postRequest('/api/list', this.paginationParams)
 *     }
 *   }
 * }
 * </script>
 */
export default {
  data() {
    return {
      pagination: {
        total: 0,
        page: 1,
        size: 10,
        pages: 1,
      },
    }
  },

  computed: {
    /**
     * 分页请求参数（已合并分页信息）
     * 使用方式：{ ...this.paginationParams, ...其他业务参数 }
     */
    paginationParams() {
      return {
        page: this.pagination.page,
        size: this.pagination.size,
      }
    },

    /**
     * 页码数组（用于渲染分页按钮）
     * 支持省略号：1 2 3 ... 10
     */
    pageNumbers() {
      const pages = this.pagination.pages
      const current = this.pagination.page

      if (pages <= 7) {
        return Array.from({ length: pages }, (_, i) => i + 1)
      }

      const nums = []
      let start = Math.max(1, current - 2)
      let end = Math.min(pages, current + 2)

      if (start > 2) {
        nums.push(1, '...')
      }

      for (let i = start; i <= end; i++) {
        nums.push(i)
      }

      if (end < pages - 1) {
        nums.push('...', pages)
      } else if (end === pages - 1) {
        nums.push(pages)
      }

      return nums
    },
  },

  methods: {
    /**
     * 切换页码
     * @param {number} page - 目标页码
     */
    changePage(page) {
      if (page < 1 || page > this.pagination.pages) return
      this.pagination.page = page
      this.onPageChange()
    },

    /**
     * 重置到第一页并刷新
     */
    resetPage() {
      this.pagination.page = 1
      this.onPageChange()
    },

    /**
     * 从接口响应中解析分页数据
     * @param {Object} resp - 接口响应（假设数据在 resp.obj 中）
     * @param {string} listKey - 列表字段名，默认 'list'
     * @returns {Array} 列表数据
     */
    parsePaginationResponse(resp, listKey = 'list') {
      if (resp && resp.obj) {
        this.pagination.total = resp.obj.total || 0
        this.pagination.pages = resp.obj.pages || 1
        return Array.isArray(resp.obj[listKey]) ? resp.obj[listKey] : []
      }
      this.pagination.total = 0
      this.pagination.pages = 1
      return []
    },

    /**
     * 页码变化时的回调（子类重写此方法）
     * 默认什么都不做，子类可以重写来触发数据加载
     */
    onPageChange() {
      // 子类重写此方法，例如：this.fetchList()
    },
  },
}
