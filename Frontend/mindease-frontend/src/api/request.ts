// 前端A负责：统一请求实例
import axios from "axios";

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 10000,
});

export default request;
