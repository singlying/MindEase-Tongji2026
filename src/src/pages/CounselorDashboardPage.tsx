import { FormEvent, useEffect, useMemo, useState } from "react";
import { appointmentApi } from "@/api";

const weekdays = [
  { label: "周一", value: 1 }, { label: "周二", value: 2 }, { label: "周三", value: 3 },
  { label: "周四", value: 4 }, { label: "周五", value: 5 }, { label: "周六", value: 6 }, { label: "周日", value: 7 }
];

const statusText = (status?: string) => ({ PENDING: "待确认", CONFIRMED: "已确认", COMPLETED: "已完成", CANCELLED: "已取消" }[String(status || "").toUpperCase()] || status || "未知");
const formatTime = (value?: string) => String(value || "").replace("T", " ").slice(0, 16);

export function CounselorDashboardPage() {
  const [items, setItems] = useState<any[]>([]);
  const [workDays, setWorkDays] = useState([1, 2, 3, 4, 5]);
  const [morning, setMorning] = useState("09:00-12:00");
  const [afternoon, setAfternoon] = useState("14:00-18:00");
  const [message, setMessage] = useState("");
  const [failed, setFailed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const metrics = useMemo(() => [
    { label: "全部预约", value: items.length, caption: "当前列表" },
    { label: "待确认", value: items.filter((item) => String(item.status).toUpperCase() === "PENDING").length, caption: "需要处理" },
    { label: "已确认", value: items.filter((item) => String(item.status).toUpperCase() === "CONFIRMED").length, caption: "后续跟进" },
    { label: "开放工作日", value: workDays.length, caption: "天/周" }
  ], [items, workDays.length]);

  async function load() {
    setLoading(true);
    try {
      const response = await appointmentApi.mine({ page: 1, pageSize: 20 });
      setItems(response.data.list || response.data.appointments || []);
    } finally {
      setLoading(false);
    }
  }

  async function confirm(id: number) {
    await appointmentApi.confirm(id);
    await load();
  }

  function toggleDay(day: number) {
    setWorkDays((value) => value.includes(day) ? value.filter((item) => item !== day) : [...value, day].sort());
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setFailed(false);
    const toHour = (value: string) => {
      const [start, end] = value.split("-").map((item) => item.trim());
      return { start, end };
    };
    try {
      const response = await appointmentApi.schedule({ workDays, workHours: [toHour(morning), toHour(afternoon)] });
      setMessage(response.message || "排班已提交");
    } catch (err) {
      setFailed(true);
      setMessage(err instanceof Error ? err.message : "排班提交失败");
    } finally {
      setSaving(false);
    }
  }

  useEffect(() => { load().catch(() => undefined); }, []);

  return (
    <div className="staff-canvas">
      <section className="ops-hero staff-hero">
        <div><span className="eyebrow">咨询师工作台</span><h2>今日预约与排班管理</h2><p>集中查看来访者预约、确认待处理事项，并维护可预约时段。</p></div>
        <button className="primary" onClick={load}>{loading && <span className="spinner" />}刷新预约</button>
      </section>

      <section className="metric-strip">
        {metrics.map((metric) => <article className="metric status-metric" key={metric.label}><span className="muted">{metric.label}</span><b>{metric.value}</b><small>{metric.caption}</small></article>)}
      </section>

      <section className="staff-layout">
        <form className="form-card stack schedule-card" onSubmit={submit}>
          <div><span className="eyebrow">排班设置</span><h2>每周可预约时间</h2></div>
          <div className="weekday-grid">{weekdays.map((day) => <button key={day.value} type="button" className={`weekday-chip ${workDays.includes(day.value) ? "active" : ""}`} onClick={() => toggleDay(day.value)}>{day.label}</button>)}</div>
          <label className="field"><span>上午时段</span><input className="input" value={morning} onChange={(e) => setMorning(e.target.value)} /></label>
          <label className="field"><span>下午时段</span><input className="input" value={afternoon} onChange={(e) => setAfternoon(e.target.value)} /></label>
          <button className="primary" disabled={saving}>{saving && <span className="spinner" />}{saving ? "提交中" : "提交排班"}</button>
          {message && <p className={`feedback-note ${failed ? "danger-note" : ""}`}>{message}</p>}
        </form>

        <section className="table-card appointment-board">
          <div className="between"><div><span className="eyebrow">来访预约</span><h2>待处理队列</h2></div><span className="tag">{items.length} 条</span></div>
          {loading ? <div className="list"><div className="skeleton-card" /><div className="skeleton-card" /><div className="skeleton-card" /></div> : <div className="appointment-list">
            {items.map((item) => <article className="appointment-card" key={item.id || item.appointmentId}><div className="timeline-emoji">{String(item.status).toUpperCase() === "PENDING" ? "⏳" : "✓"}</div><div><div className="between"><b>{item.targetName || item.userName || "来访者"}</b><span className="tag">{statusText(item.status)}</span></div><p className="muted">{formatTime(item.startTime)} - {formatTime(item.endTime)}</p><p className="muted">{item.userNote || item.note || "暂无备注"}</p></div><button className="ghost" disabled={String(item.status).toUpperCase() !== "PENDING"} onClick={() => confirm(item.id || item.appointmentId)}>确认</button></article>)}
            {!items.length && <div className="empty-state">暂无待处理预约</div>}
          </div>}
        </section>
      </section>
    </div>
  );
}
