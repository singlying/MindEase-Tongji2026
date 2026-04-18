import { FormEvent, useEffect, useMemo, useState } from "react";
import { adminApi } from "@/api";

export function AdminDashboardPage() {
  const [audits, setAudits] = useState<any[]>([]);
  const [scale, setScale] = useState({ scaleKey: "", title: "", coverUrl: "", description: "", status: "ACTIVE" });
  const [question, setQuestion] = useState({ scaleKey: "", questionText: "", options: "没有=0,几天=1,一半以上=2,几乎每天=3" });
  const [message, setMessage] = useState("");
  const [failed, setFailed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [savingScale, setSavingScale] = useState(false);
  const [savingQuestion, setSavingQuestion] = useState(false);
  const metrics = useMemo(() => [
    { label: "待审核", value: audits.length, caption: "咨询师资料" },
    { label: "审核操作", value: "PASS", caption: "通过/拒绝" },
    { label: "量表状态", value: scale.status, caption: "默认启用" },
    { label: "题目模板", value: 4, caption: "个选项" }
  ], [audits.length, scale.status]);

  async function load() {
    setLoading(true);
    try {
      const response = await adminApi.auditList({ page: 1, pageSize: 20 });
      setAudits(response.data.list || response.data.records || response.data.audits || []);
    } finally {
      setLoading(false);
    }
  }

  async function process(item: any, approved: boolean) {
    await adminApi.processAudit({
      auditId: item.auditId || item.id,
      action: approved ? "PASS" : "REJECT",
      remark: approved ? "资料完整，审核通过" : "资料信息不完整，请补充后重新提交"
    });
    await load();
  }

  async function createScale(event: FormEvent) {
    event.preventDefault();
    setSavingScale(true);
    setFailed(false);
    try {
      const response = await adminApi.createScale({
        ...scale,
        scoringRules: [
          { min: 0, max: 4, level: "低风险", desc: "保持当前节奏" },
          { min: 5, max: 9, level: "中风险", desc: "建议持续观察" },
          { min: 10, max: 21, level: "高风险", desc: "建议预约咨询" }
        ]
      });
      setMessage(response.message || "量表已保存");
    } catch (err) {
      setFailed(true);
      setMessage(err instanceof Error ? err.message : "量表保存失败");
    } finally {
      setSavingScale(false);
    }
  }

  async function createQuestion(event: FormEvent) {
    event.preventDefault();
    setSavingQuestion(true);
    setFailed(false);
    try {
      const options = question.options.split(",").map((item) => {
        const [label, score] = item.split("=").map((value) => value.trim());
        return { label, score: Number(score) };
      });
      const response = await adminApi.createQuestion({
        scaleKey: question.scaleKey,
        questions: [{ questionText: question.questionText, sortOrder: 1, options, deleted: false }]
      });
      setMessage(response.message || "题目已保存");
    } catch (err) {
      setFailed(true);
      setMessage(err instanceof Error ? err.message : "题目保存失败");
    } finally {
      setSavingQuestion(false);
    }
  }

  useEffect(() => { load().catch(() => undefined); }, []);

  return (
    <div className="staff-canvas">
      <section className="ops-hero admin-hero">
        <div><span className="eyebrow">管理员控制台</span><h2>审核、量表与题目配置</h2><p>集中处理咨询师资质审核，维护心理测评量表与题目内容。</p></div>
        <button className="primary" onClick={load}>{loading && <span className="spinner" />}刷新审核</button>
      </section>

      <section className="metric-strip">
        {metrics.map((metric) => <article className="metric status-metric" key={metric.label}><span className="muted">{metric.label}</span><b>{metric.value}</b><small>{metric.caption}</small></article>)}
      </section>

      <section className="staff-layout admin-layout">
        <section className="table-card audit-board">
          <div className="between"><div><span className="eyebrow">咨询师审核</span><h2>待审核队列</h2></div><span className="tag">{audits.length} 条</span></div>
          <div className="appointment-list">{audits.map((item) => <article className="audit-card" key={item.auditId || item.id || item.userId}><div className="timeline-emoji">资</div><div><div className="between"><b>{item.realName || item.nickname || item.username || "咨询师"}</b><span className="tag">{item.title || "待审核"}</span></div><p className="muted">{item.specialty || item.certification || item.reason || "资料已提交，等待管理员处理。"}</p></div><div className="row audit-actions"><button className="primary" onClick={() => process(item, true)}>通过</button><button className="danger" onClick={() => process(item, false)}>拒绝</button></div></article>)}
          {!audits.length && <div className="empty-state">暂无待审核资料</div>}</div>
        </section>

        <div className="admin-tools">
          <form className="form-card stack" onSubmit={createScale}>
            <div><span className="eyebrow">量表管理</span><h2>新增 / 更新量表</h2></div>
            <input className="input" value={scale.scaleKey} onChange={(e) => setScale({ ...scale, scaleKey: e.target.value })} placeholder="量表 Key，例如 GAD7" />
            <input className="input" value={scale.title} onChange={(e) => setScale({ ...scale, title: e.target.value })} placeholder="量表标题" />
            <input className="input" value={scale.coverUrl} onChange={(e) => setScale({ ...scale, coverUrl: e.target.value })} placeholder="封面 URL（可选）" />
            <textarea className="textarea" value={scale.description} onChange={(e) => setScale({ ...scale, description: e.target.value })} placeholder="量表说明" />
            <button className="primary" disabled={savingScale}>{savingScale && <span className="spinner" />}保存量表</button>
          </form>
          <form className="form-card stack" onSubmit={createQuestion}>
            <div><span className="eyebrow">题目管理</span><h2>保存题目</h2></div>
            <input className="input" value={question.scaleKey} onChange={(e) => setQuestion({ ...question, scaleKey: e.target.value })} placeholder="归属量表 Key" />
            <textarea className="textarea" value={question.questionText} onChange={(e) => setQuestion({ ...question, questionText: e.target.value })} placeholder="题目内容" />
            <input className="input" value={question.options} onChange={(e) => setQuestion({ ...question, options: e.target.value })} placeholder="选项：没有=0,几天=1,一半以上=2,几乎每天=3" />
            <button className="ghost" disabled={savingQuestion}>{savingQuestion && <span className="spinner" />}保存题目</button>
          </form>
          {message && <p className={`feedback-note ${failed ? "danger-note" : ""}`}>{message}</p>}
        </div>
      </section>
    </div>
  );
}
