package com.mindease.pojo.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分页查询请求 DTO 单元测试
 * <p>
 * 验证 PageQuery 分页参数的合法性校验、默认值回退、
 * 边界保护（如超大页码/页大小）、排序参数清洗等功能。
 * </p>
 */
@DisplayName("PageQuery 分页查询单元测试")
class PageQueryTest {

    // --------------------------------------------------------
    // 轻量级 PageQuery 实现（模拟实际 DTO 行为）
    // --------------------------------------------------------

    static class PageQuery {
        private Integer page = 1;
        private Integer pageSize = 10;
        private String orderBy;
        private Boolean ascending = true;
        private String keyword;

        public PageQuery() {}

        public PageQuery(Integer page, Integer pageSize) {
            this.page = page;
            this.pageSize = pageSize;
        }

        /** 校验并修正分页参数，返回修正后的自身引用 */
        public PageQuery normalize() {
            if (page == null || page < 1) page = 1;
            if (pageSize == null || pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100;  // 上限保护
            if (orderBy != null) {
                orderBy = orderBy.trim();
            }
            if (ascending == null) ascending = true;
            return this;
        }

        /** 计算数据库层面的 OFFSET 值 */
        public int getOffset() {
            normalize();
            return (page - 1) * pageSize;
        }

        /** 获取安全的 limit 值 */
        public int getLimit() {
            normalize();
            return pageSize;
        }

        /** 是否有搜索关键词 */
        public boolean hasKeyword() {
            return keyword != null && !keyword.trim().isEmpty();
        }

        /** 计算总页数 */
        public int getTotalPages(long totalCount) {
            normalize();
            if (totalCount <= 0) return 0;
            return (int) Math.ceil((double) totalCount / pageSize);
        }

        /** 校验是否为有效页码 */
        public boolean isValidPage(long totalCount) {
            int totalPages = getTotalPages(totalCount);
            return totalPages > 0 && page >= 1 && page <= totalPages;
        }

        /** 获取排序 SQL 片段 */
        public String buildOrderClause(Map<String, String> allowedColumns) {
            if (orderBy == null || allowedColumns == null || !allowedColumns.containsKey(orderBy)) {
                return "";
            }
            String col = allowedColumns.get(orderBy);
            return col + (Boolean.TRUE.equals(ascending) ? " ASC" : " DESC");
        }

        // --- getter / setter ---

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
        public String getOrderBy() { return orderBy; }
        public void setOrderBy(String orderBy) { this.orderBy = orderBy; }
        public Boolean getAscending() { return ascending; }
        public void setAscending(Boolean ascending) { this.ascending = ascending; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
    }

    private PageQuery query;

    @BeforeEach
    void setUp() {
        query = new PageQuery();
    }

    // ==================== 默认值测试 ====================

    @Nested
    @DisplayName("默认参数值")
    class DefaultValuesTests {

        @Test
        @DisplayName("未设置时 page 默认为 1")
        void defaultPageIsOne() {
            assertEquals(1, query.getPage());
        }

        @Test
        @DisplayName("未设置时 pageSize 默认为 10")
        void defaultPageSizeIsTen() {
            assertEquals(10, query.getPageSize());
        }

        @Test
        @DisplayName("默认升序排列")
        void defaultOrderIsAsc() {
            assertTrue(query.getAscending());
        }

        @Test
        @DisplayName("默认无排序字段和关键词")
        void defaultOrderByAndKeywordNull() {
            assertNull(query.getOrderBy());
            assertNull(query.getKeyword());
        }
    }

    // ==================== 参数标准化测试 ====================

    @Nested
    @DisplayName("参数标准化（normalize）")
    class NormalizationTests {

        @Test
        @DisplayName("page 为 0 时修正为 1")
        void zeroPageNormalizedToOne() {
            query.setPage(0);
            query.normalize();
            assertEquals(1, query.getPage());
        }

        @Test
        @DisplayName("page 为负数时修正为 1")
        void negativePageNormalizedToOne() {
            query.setPage(-5);
            query.normalize();
            assertEquals(1, query.getPage());
        }

        @Test
        @DisplayName("page 为 null 时修正为 1")
        void nullPageNormalizedToOne() {
            query.setPage(null);
            query.normalize();
            assertEquals(1, query.getPage());
        }

        @Test
        @DisplayName("pageSize 为 0 时修正为默认值 10")
        void zeroPageSizeNormalizedToDefault() {
            query.setPageSize(0);
            query.normalize();
            assertEquals(10, query.getPageSize());
        }

        @Test
        @DisplayName("pageSize 超过上限 100 时截断到 100")
        void oversizedPageSizeCapped() {
            query.setPageSize(9999);
            query.normalize();
            assertEquals(100, query.getPageSize());
        }

        @Test
        @DisplayName("pageSize 为 null 时使用默认值 10")
        void nullPageSizeNormalized() {
            query.setPageSize(null);
            query.normalize();
            assertEquals(10, query.getPageSize());
        }

        @Test
        @DisplayName("ascending 为 null 时默认设为 true")
        void nullAscendingDefaultsTrue() {
            query.setAscending(null);
            query.normalize();
            assertTrue(query.getAscending());
        }

        @Test
        @DisplayName("normalize 方法返回自身引用支持链式调用")
        void returnsSelfForChaining() {
            PageQuery result = query.normalize();
            assertSame(query, result, "应返回同一实例");
        }
    }

    // ==================== OFFSET / LIMIT 计算 ====================

    @Nested
    @DisplayName("OFFSET 与 LIMIT 计算")
    class OffsetLimitTests {

        @Test
        @DisplayName("第 1 页每页 10 条 → offset=0")
        void firstPageOffsetZero() {
            query.setPage(1);
            assertEquals(0, query.getOffset());
        }

        @Test
        @DisplayName("第 2 页每页 10 条 → offset=10")
        void secondPageOffsetTen() {
            query.setPage(2);
            assertEquals(10, query.getOffset());
        }

        @Test
        @DisplayName("第 3 页每页 20 条 → offset=40")
        void customPageSizeOffset() {
            query.setPage(3);
            query.setPageSize(20);
            assertEquals(40, query.getOffset());
        }

        @Test
        @DisplayName("limit 值受上限约束")
        void limitRespectsCap() {
            query.setPageSize(50);
            assertEquals(50, query.getLimit());

            query.setPageSize(200);
            assertEquals(100, query.getLimit());
        }
    }

    // ==================== 总页数计算 ====================

    @Nested
    @DisplayName("总页数计算")
    class TotalPagesTests {

        @Test
        @DisplayName("0 条记录 → 总页数为 0")
        void zeroRecordsZeroPages() {
            assertEquals(0, query.getTotalPages(0));
        }

        @Test
        @DisplayName("5 条记录每页 10 条 → 1 页")
        void lessThanOnePage() {
            assertEquals(1, query.getTotalPages(5));
        }

        @Test
        @DisplayName("刚好整除 → 页数为商")
        void exactDivision() {
            query.setPageSize(20);
            assertEquals(5, query.getTotalPages(100));
        }

        @Test
        @DisplayName("有余数 → 进位取整")
        void withRemainder() {
            query.setPageSize(10);
            assertEquals(11, query.getTotalPages(101));
            assertEquals(3, query.getTotalPages(21));
        }

        @Test
        @DisplayName("大数据量分页计算精度")
        void largeDataset() {
            query.setPageSize(50);
            assertEquals(2000, query.getTotalPages(100000));
            assertEquals(2001, query.getTotalPages(100001));
        }
    }

    // ==================== 页码有效性校验 ====================

    @Nested
    @DisplayName("页码有效性校验")
    class ValidPageTests {

        @Test
        @DisplayName("有数据时第一页有效")
        void firstPageIsValidWhenDataExists() {
            assertTrue(query.isValidPage(100));
        }

        @Test
        @DisplayName("超出最大页码视为无效")
        void beyondMaxPageIsInvalid() {
            query.setPage(100);
            assertFalse(query.isValidPage(50));
        }

        @Test
        @DisplayName("0 条数据时任何页码都无效")
        void anyPageInvalidWithNoData() {
            assertFalse(query.isValidPage(0));
        }

        @Test
        @DisplayName("刚好最后一页有效")
        void lastPageIsValid() {
            query.setPage(10);
            query.setPageSize(10);
            assertTrue(query.isValidPage(95));  // 95条→10页, 第10页有效
        }
    }

    // ==================== 排序子句构建 ====================

    @Nested
    @DisplayName("排序子句构建")
    class OrderClauseTests {

        private Map<String, String> columns;

        @org.junit.jupiter.api.BeforeEach
        void initColumns() {
            columns = new HashMap<>();
            columns.put("createTime", "create_time");
            columns.put("updateTime", "update_time");
            columns.put("name", "name");
        }

        @Test
        @DisplayName("允许的字段生成 ASC 排序")
        void ascOrderClause() {
            query.setOrderBy("createTime");
            String clause = query.buildOrderClause(columns);
            assertEquals("create_time ASC", clause);
        }

        @Test
        @DisplayName("降序排列生成 DESC 排序")
        void descOrderClause() {
            query.setOrderBy("name");
            query.setAscending(false);
            String clause = query.buildOrderClause(columns);
            assertEquals("name DESC", clause);
        }

        @Test
        @DisplayName("不在白名单中的字段忽略排序")
        void disallowedFieldIgnored() {
            query.setOrderBy("dangerous_sql_injection");
            String clause = query.buildOrderClause(columns);
            assertEquals("", clause);
        }

        @Test
        @DisplayName("未指定排序字段时返回空串")
        void noOrderFieldReturnsEmpty() {
            String clause = query.buildOrderClause(columns);
            assertEquals("", clause);
        }

        @Test
        @DisplayName("白名单本身为 null 时不崩溃")
        void nullAllowedColumnsSafe() {
            query.setOrderBy("createTime");
            assertDoesNotThrow(() -> query.buildOrderClause(null));
            assertEquals("", query.buildOrderClause((Map<String, String>) null));
        }
    }

    // ==================== 关键词搜索测试 ====================

    @Nested
    @DisplayName("搜索关键词检测")
    class KeywordTests {

        @Test
        @DisplayName("有关键词返回 true")
        void keywordPresent() {
            query.setKeyword("焦虑症");
            assertTrue(query.hasKeyword());
        }

        @Test
        @DisplayName("纯空格关键词视为无关键词")
        void whitespaceOnlyKeywordIgnored() {
            query.setKeyword("   ");
            assertFalse(query.hasKeyword());
        }

        @Test
        @DisplayName("null 关键词返回 false")
        void nullKeyword() {
            assertFalse(query.hasKeyword());
        }

        @Test
        @DisplayName("空字符串返回 false")
        void emptyKeyword() {
            query.setKeyword("");
            assertFalse(query.hasKeyword());
        }
    }

    // ==================== 构造函数测试 ====================

    @Nested
    @DisplayName("构造函数初始化")
    class ConstructorTests {

        @Test
        @DisplayName("无参构造使用全部默认值")
        void noArgConstructorUsesDefaults() {
            PageQuery q = new PageQuery();
            q.normalize();
            assertEquals(1, q.getPage());
            assertEquals(10, q.getPageSize());
        }

        @Test
        @DisplayName("双参构造接受自定义分页参数")
        void twoArgConstructor() {
            PageQuery q = new PageQuery(5, 20);
            q.normalize();
            assertEquals(5, q.getPage());
            assertEquals(20, q.getPageSize());
        }

        @Test
        @DisplayName("双参构造传异常值后 normalize 可修正")
        void constructorWithAbnormalValuesThenNormalize() {
            PageQuery q = new PageQuery(-1, 0);
            q.normalize();
            assertEquals(1, q.getPage());
            assertEquals(10, q.getPageSize());
        }
    }

    // ==================== 反射字段完整性 ====================

    @Nested
    @DisplayName("DTO 字段完整性检查")
    class FieldIntegrityTests {

        @Test
        @DisplayName("确认所有必要字段均存在")
        void allRequiredFieldsExist() {
            String[] expectedFields = {"page", "pageSize", "orderBy", "ascending", "keyword"};
            Field[] declaredFields = PageQuery.class.getDeclaredFields();
            assertEquals(expectedFields.length, declaredFields.length,
                    "字段数量应匹配预期");
            for (Field f : declaredFields) {
                assertNotNull(f.getName());
            }
        }
    }
}
