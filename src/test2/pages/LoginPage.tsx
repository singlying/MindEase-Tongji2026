import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "@/api";
import { homeForUser, useSession } from "@/state/session";

export function LoginPage() {
  const session = useSession();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      const login = await authApi.login(form);
      session.setToken(login.data.token);
      const profile = await session.fetchUser();
      navigate(homeForUser(profile.role, profile.status));
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || "登录失败");
    } finally {
      setLoading(false);
    }
  }

  function demoLogin() {
    session.setToken(`demo_token_${Date.now()}`);
    session.setUser({
      userId: 1001,
      username: "demo_user",
      nickname: "演示用户",
      avatar: "",
      role: "user",
      status: 1
    });
    navigate("/home");
  }

  return (
    <div className="auth">
      <form className="auth-panel" onSubmit={submit}>
        <h1>MindEase Clinic Console</h1>
        <p className="muted">登录后进入心理健康工作台，查看记录、测评、咨询和报告。</p>
        <input className="input" placeholder="用户名" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
        <input className="input" placeholder="密码" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <button className="primary">{loading ? "登录中..." : "登录"}</button>
        <button className="ghost" type="button" onClick={demoLogin}>演示模式</button>
        <p className="muted">没有账号？<Link to="/register">注册</Link></p>
        {error && <p className="muted" style={{ color: "var(--danger)" }}>{error}</p>}
      </form>
      <div className="auth-visual"><h2>把情绪、测评和咨询安排放进一个清晰的工作台。</h2></div>
    </div>
  );
}
