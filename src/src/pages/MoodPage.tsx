import { FormEvent, useEffect, useMemo, useState } from "react";
import { moodApi } from "@/api";

const moodOptions = [
  { type: "Happy", label: "愉悦", emoji: "😊", score: 8 },
  { type: "Calm", label: "平静", emoji: "😌", score: 6 },
  { type: "Sad", label: "低落", emoji: "😔", score: 3 },
  { type: "Anxious", label: "焦虑", emoji: "😰", score: 4 },
  { type: "Angry", label: "烦躁", emoji: "😤", score: 3 }
];

function localDateTime() {
  const date = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function MoodPage() {
  const [logs, setLogs] = useState<any[]>([]);
  const [message, setMessage] = useState("");
  const [tagText, setTagText] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ moodType: "Calm", moodScore: 6, content: "" });
  const selectedMood = useMemo(() => moodOptions.find((item) => item.type === form.moodType) || moodOptions[1], [form.moodType]);
  const moodMeta = (type: string) => moodOptions.find((item) => item.type.toLowerCase() === String(type).toLowerCase()) || moodOptions[1];

  async function load() {
    setLoading(true);
    try {
      const response = await moodApi.list({ limit: 12, offset: 0 });
      setLogs(response.data.logs || []);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "情绪记录加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const response = await moodApi.create({
        ...form,
        tags: tagText.split(",").map((tag) => tag.trim()).filter(Boolean),
        logDate: localDateTime()
      });
      setMessage(response.data?.aiAnalysis || response.message || "已保存");
      setForm({ ...form, content: "" });
      await load();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "保存失败，请稍后再试");
    } finally {
      setSaving(false);
    }
  }

  useEffect(() => { load().catch(() => setLogs([])); }, []);

  return (
    <div className="grid">
      <form className="form-card span-5 stack mood-composer" onSubmit={submit}>
        <div className="between">
          <div><h2>情绪记录</h2><p className="muted">用更快的方式捕捉今天的情绪状态。</p></div>
          <div className="mood-hero">{selectedMood.emoji}</div>
        </div>
        <div className="mood-picker">
          {moodOptions.map((mood) => <button key={mood.type} type="button" className={`mood-option ${form.moodType === mood.type ? "active" : ""}`} onClick={() => setForm({ ...form, moodType: mood.type, moodScore: mood.score })}><span>{mood.emoji}</span><strong>{mood.label}</strong></button>)}
        </div>
        <label className="field"><span>情绪强度 {form.moodScore}/10</span><input className="range" type="range" min="1" max="10" value={form.moodScore} onChange={(e) => setForm({ ...form, moodScore: Number(e.target.value) })} /></label>
        <textarea className="textarea journal-paper" value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} placeholder="今天哪一刻最影响你的心情？" />
        <input className="input" value={tagText} onChange={(e) => setTagText(e.target.value)} placeholder="标签，例如：睡眠, 汇报, 运动" />
        <button className="primary submit-glow">{saving && <span className="spinner" />}{saving ? "正在保存" : "保存记录"}</button>
        {message && <p className="feedback-note">{message}</p>}
      </form>
      <section className="table-card span-7">
        <div className="between"><div><h2>情绪时间线</h2><p className="muted">最近的记录会用于后续报告和咨询推荐。</p></div><button className="ghost" onClick={load}>{loading && <span className="spinner" />}刷新</button></div>
        {loading ? <div className="list"><div className="skeleton-card" /><div className="skeleton-card" /><div className="skeleton-card" /></div> : <div className="list">
          {logs.map((item) => <article className="timeline-item" key={item.id || item.logId}><div className="timeline-emoji">{moodMeta(item.moodType).emoji}</div><div><div className="between"><b>{moodMeta(item.moodType).label} · {item.moodScore || item.score}/10</b><span className="tag">{String(item.logDate || item.createTime || "").replace("T", " ").slice(0, 16)}</span></div><p className="muted">{item.content}</p>{(item.tags || []).map((tag: string) => <span className="tag" key={tag}>{tag}</span>)}</div></article>)}
        </div>}
      </section>
    </div>
  );
}
