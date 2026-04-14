// Axios 请求封装
import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import { ElMessage } from "element-plus";

// 响应数据接口
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 15000,
  headers: {
    "Content-Type": "application/json;charset=utf-8",
  },
});

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token
    // 后端通过 "token" 请求头获取JWT（不需要Bearer前缀）
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.token = token;
    }
    return config;
  },
  (error) => {
    console.error("请求错误:", error);
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>): any => {
    const res = response.data;

    // 如果返回的状态码不是 200，则认为是错误
    if (res.code !== 200) {
      ElMessage.error(res.message || "请求失败");

      // 401: 未授权，跳转登录页
      if (res.code === 401) {
        // 清除所有认证信息
        localStorage.removeItem("token");
        // 注意：直接跳转，避免循环
        if (window.location.pathname !== "/login") {
          window.location.href = "/login";
        }
      }

      return Promise.reject(new Error(res.message || "请求失败"));
    }

    // 返回解包后的数据，类型为 ApiResponse
    return res;
  },
  (error) => {
    console.error("响应错误:", error);

    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error("未授权，请重新登录");
          // 清除所有认证信息
          localStorage.removeItem("token");
          // 注意：直接跳转，避免循环
          if (window.location.pathname !== "/login") {
            window.location.href = "/login";
          }
          break;
        case 403:
          ElMessage.error("拒绝访问");
          break;
        case 404:
          ElMessage.error("请求的资源不存在");
          break;
        case 500:
          ElMessage.error("服务器错误");
          break;
        default:
          ElMessage.error(error.response.data?.message || "请求失败");
      }
    } else {
      ElMessage.error("网络错误，请检查网络连接");
    }

    return Promise.reject(error);
  }
);

// 导出请求方法
export default service;
