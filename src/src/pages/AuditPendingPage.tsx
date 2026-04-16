import { FormEvent, useEffect, useState } from "react";
import { counselorApi } from "@/api";

export function AuditPendingPage() {
  const [form, setForm] = useState({ realName: "", title: "", experienceYears: 0, qualificationUrl: "", bio: "" });
  const [status, setStatus] = useState<any>({});
  const [message, setMessage] = useState("");

  async function load() {
    setStatus((await counselorApi.auditStatus().catch(() => ({ data: {} }))).data);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const response = await counselorApi.submitAudit(form);
    setMessage(response.message || "已提交");
    await load();
  }

  useEffect(() => { load().catch(() => undefined); }, []);

  return (
    <div className="grid">
      <form className="form-card span-6 stack" onSubmit={submit}>
        <h2>提交资质</h2>
        <input className="input" value={form.realName} onChange={(e) => setForm({ ...form, realName: e.target.value })} placeholder="真实姓名" />
        <input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="职称" />
        <input className="input" type="number" value={form.experienceYears} onChange={(e) => setForm({ ...form, experienceYears: Number(e.target.value) })} placeholder="年限" />
        <input className="input" value={form.qualificationUrl} onChange={(e) => setForm({ ...form, qualificationUrl: e.target.value })} placeholder="资质 URL" />
        <textarea className="textarea" value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} placeholder="简介" />
        <button className="primary">提交审核</button>
        {message && <p className="muted">{message}</p>}
      </form>
      <section className="card span-6"><h2>状态</h2><pre className="muted">{JSON.stringify(status, null, 2)}</pre><button className="ghost" onClick={load}>刷新</button></section>
    </div>
  );
}
