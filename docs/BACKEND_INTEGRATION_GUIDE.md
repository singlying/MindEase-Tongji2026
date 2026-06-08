# 后端对接指南

> 📘 当后端 API 完成后，前端如何对接？

---

## ✅ 理想情况（零代码修改）

如果后端**严格按照 API 文档**实现，只需两步：

### 步骤 1：修改环境变量

**文件**：`.env.development`

```bash
# 修改前
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCK=true

# 修改后
VITE_API_BASE_URL=http://your-backend-url/api  # 改为后端实际地址
VITE_USE_MOCK=false  # 关闭Mock
```

### 步骤 2：重启开发服务器

```bash
Ctrl+C  # 停止
npm run dev  # 重启
```

**就这么简单！** 🎉

---

## ⚠️ 可能需要调整的情况

### 情况 1：响应数据结构不一致

**问题**：后端返回的数据格式与 API 文档不符

**示例**：

```javascript
// API文档约定
{
  "code": 200,
  "message": "success",
  "data": {
    "logId": 5001,
    "aiAnalysis": "..."
  }
}

// 后端实际返回
{
  "status": 200,  // ← 字段名不同
  "msg": "OK",    // ← 字段名不同
  "result": {     // ← 字段名不同
    "id": 5001,   // ← 字段名不同
    "analysis": "..."
  }
}
```

**解决位置**：`src/api/request.ts` - 响应拦截器

```typescript
// 可能需要修改
service.interceptors.response.use(
  (response) => {
    // 统一处理响应格式差异
    const res = response.data;

    // 如果后端字段不同，在这里转换
    if (res.status !== undefined) {
      return {
        code: res.status,
        message: res.msg,
        data: res.result,
      };
    }

    return res;
  }
  // ...
);
```

---

### 情况 2：CORS 跨域问题

**问题**：浏览器控制台报错

```
Access to XMLHttpRequest at 'http://backend:8080/api/mood/log'
from origin 'http://localhost:5173' has been blocked by CORS policy
```

**解决方案 A**：后端配置 CORS（推荐）

```java
// Spring Boot 示例
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        // ...
    }
}
```

**解决方案 B**：前端配置代理

**文件**：`vite.config.ts`

```typescript
export default defineConfig({
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
```

然后环境变量改为：

```bash
VITE_API_BASE_URL=/api  # 不带域名
```

---

### 情况 3：需要 Token 认证

**问题**：后端要求请求头带 Token

**解决位置**：`src/api/request.ts` - 请求拦截器

```typescript
// 可能需要添加
service.interceptors.request.use(
  (config) => {
    // 从本地存储获取token
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }
  // ...
);
```

---

### 情况 4：日期格式不一致

**问题**：前端发送 `2024-11-27 14:00:00`，后端要求 ISO 格式

**解决位置**：提交日记的地方

```typescript
// src/views/mood/MoodDiaryView.vue

// 修改前
formData.value = {
  logDate: new Date().toISOString().slice(0, 19).replace("T", " "),
};

// 修改后（根据后端要求）
formData.value = {
  logDate: new Date().toISOString(), // ISO 8601格式
};
```

---

### 情况 5：错误处理逻辑

**问题**：后端错误码与约定不符

**解决位置**：`src/api/request.ts` - 响应拦截器

```typescript
service.interceptors.response.use(
  (response) => {
    const res = response.data;

    // 根据后端实际错误码调整
    if (res.code !== 200) {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message));
    }

    return res;
  }
  // ...
);
```

---

## 📋 对接检查清单

### 环境配置检查

- [ ] `.env.development` 中 `VITE_USE_MOCK=false`
- [ ] `VITE_API_BASE_URL` 指向正确的后端地址
- [ ] 已重启开发服务器

### 功能测试

- [ ] 提交日记成功，能看到新日记
- [ ] 删除日记成功，日记消失
- [ ] 查看详情正常显示
- [ ] AI 分析内容正确
- [ ] 趋势图数据准确

### 网络检查

- [ ] 打开浏览器开发者工具 → Network
- [ ] 提交日记，检查请求是否发送到后端
- [ ] 检查响应状态码（应为 200）
- [ ] 检查响应数据结构是否符合预期
- [ ] 无 CORS 错误

### 数据持久性

- [ ] 提交日记后刷新页面，日记仍在
- [ ] 删除日记后刷新页面，日记仍不在
- [ ] 换浏览器/设备能看到数据

---

## 🔍 调试技巧

### 1. 查看网络请求

```
浏览器 F12 → Network → 筛选 XHR/Fetch
提交日记 → 查看请求详情
```

关注：

- Request URL：是否正确
- Request Method：是否为 POST
- Request Payload：数据是否完整
- Response：返回数据格式

### 2. 查看控制台日志

```javascript
// 在 API 调用前添加
console.log("提交数据:", formData.value);

// 在响应后添加
console.log("响应数据:", res);
```

### 3. 对比 Mock 和真实 API

```javascript
// 可以临时在代码中切换
const USE_MOCK = true; // 切换到Mock对比
const USE_MOCK = false; // 切换到真实API
```

---

## 📞 常见问题

### Q: 修改环境变量后不生效？

**A**: 必须重启开发服务器（Ctrl+C 后再 npm run dev）

### Q: 提示网络错误？

**A**:

1. 检查后端是否启动
2. 检查 URL 是否正确
3. 检查是否有 CORS 问题

### Q: 数据格式错误？

**A**:

1. 对比 API 文档和实际响应
2. 在 request.ts 中添加数据转换
3. 联系后端调整格式

### Q: Token 认证失败？

**A**:

1. 检查 localStorage 中是否有 token
2. 检查请求头是否正确添加
3. 检查 token 是否过期

---

## 🎯 总结

**最理想情况**：

- ✅ 只需修改 `.env.development`
- ✅ 无需修改任何代码
- ✅ 重启服务器即可

**实际情况**：

- 可能需要调整 `request.ts` 的拦截器
- 可能需要配置 CORS 或代理
- 可能需要调整日期格式
- 以上都是**小改动**，核心业务逻辑不变

**最坏情况**：

- 后端 API 完全不按文档实现
- 需要大量修改 API 调用代码
- **强烈建议后端严格按 API 文档开发**

---

_最后更新：2025 年 12 月 1 日 23:40_
