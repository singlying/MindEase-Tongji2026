import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { authApi, type RegisterParams } from "@/api";

export function RegisterPage() {
  const [form, setForm] = useState<RegisterParams>({ username: "", password: "", nickname: "", phone: "", role: "user", invitationCode: "" });
  const [message, setMessage] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      const response = await authApi.register(form);
      setMessage(response.message || "注册成功，请登录");
    } catch (err: any) {
      setMessage(err?.response?.data?.message || err?.message || "注册失败");
    }
  }

  return (
    <div className="auth">
      <form className="auth-panel" onSubmit={submit}>
        <h1>创建账号</h1>
        <input className="input" placeholder="用户名" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
        <input className="input" placeholder="昵称" value={form.nickname} onChange={(e) => setForm({ ...form, nickname: e.target.value })} />
        <input className="input" placeholder="手机号" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        <input className="input" placeholder="密码" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <select className="select" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as RegisterParams["role"] })}>
          <option value="user">普通用户</option>
          <option value="counselor">咨询师</option>
          <option value="admin">管理员</option>
        </select>
        {form.role === "admin" && <input className="input" placeholder="邀请码" value={form.invitationCode} onChange={(e) => setForm({ ...form, invitationCode: e.target.value })} />}
        <button className="primary">注册</button>
        <p className="muted">已有账号？<Link to="/login">登录</Link></p>
        {message && <p className="muted">{message}</p>}
      </form>
      <div className="auth-visual"><h2>选择身份，开启心理健康支持服务。</h2></div>
    </div>
  );
}
