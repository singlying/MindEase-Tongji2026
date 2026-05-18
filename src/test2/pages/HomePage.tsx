import { useEffect, useMemo, useState } from "react";
import { moodApi, userApi } from "@/api";

const fallbackNotifications = [
  { id: "n1", title: "晚间呼吸练习", content: "今晚 21:30 有一段 5 分钟训练", priority: "低" },
  { id: "n2", title: "咨询提醒", content: "明天下午有一场预约咨询", priority: "中" },
  { id: "n3", title: "测评建议", content: "可以复测 GAD-7 观察近期变化", priority: "低" }
];

export function HomePage() {
  const [trend, setTrend] = useState<any>({});
  const [notifications, setNotifications] = useState<any[]>([]);
  const scores = useMemo(() => trend.scores || [4, 5, 6, 5, 7, 8, 7], [trend]);
  const dates = trend.dates || ["一", "二", "三", "四", "五", "六", "日"];
  const queue = notifications.length ? notifications : fallbackNotifications;
  const metrics = [
    { label: "平均情绪", value: Number(trend.avgScore || 0).toFixed(1), change: "近 7 天" },
    { label: "积极占比", value: `${Math.round((trend.positiveRate || 0) * 100)}%`, change: "趋势稳定" },
    { label: "连续记录", value: trend.continuousDays || 0, change: "天" },
    { label: "通知", value: queue.length, change: "待处理" }
  ];

  useEffect(() => {
    moodApi.trend(7).then((res) => setTrend(res.data)).catch(() => setTrend({}));
    userApi.notifications(8).then((res) => setNotifications(res.data.notifications || [])).catch(() => setNotifications([]));
  }, []);

  return (
    <div className="command-center">
      <section className="ops-hero">
        <div>
          <span className="eyebrow"><i className="fa-solid fa-satellite-dish" style={{ marginRight: '8px' }}></i> 今日服务队列</span>
          <h2>把情绪记录、测评、咨询预约汇总成清晰下一步</h2>
          <p>面向连续使用的心理健康工作台，优先呈现状态、风险和可执行操作。</p>
        </div>
        <a className="primary hero-cta" href="/mood-diary"><i className="fa-solid fa-plus" style={{ marginRight: '8px' }}></i> 新增记录</a>
      </section>

      <section className="metric-strip">
        {metrics.map((metric) => (
          <article className="metric status-metric" key={metric.label}>
            <span className="muted">{metric.label}</span>
            <b>{metric.value}</b>
            <small>{metric.change}</small>
          </article>
        ))}
      </section>

      <section className="ops-grid">
        <article className="card trend-card">
          <div className="between">
            <div>
              <span className="eyebrow"><i className="fa-solid fa-chart-area" style={{ marginRight: '8px' }}></i> 情绪趋势</span>
              <h2>7 天观察</h2>
            </div>
            <span className="tag">avg {Number(trend.avgScore || 0).toFixed(1)}</span>
          </div>
          <div className="bars clinical-bars">
            {scores.map((score: number, index: number) => (
              <div className="clinical-bar" key={index}>
                <i style={{ height: Math.max(18, score * 16) }} />
                <span>{dates[index] || index + 1}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="card workflow-card">
          <span className="eyebrow"><i className="fa-solid fa-code-branch" style={{ marginRight: '8px' }}></i> 工作流入口</span>
          <h2>快速处置</h2>
          <div className="workflow-list">
            <a className="workflow-row active" href="/mood-diary"><span><i className="fa-solid fa-pen-nib"></i></span><strong>记录情绪</strong><small>补充触发事件</small></a>
            <a className="workflow-row" href="/ai-chat"><span><i className="fa-solid fa-robot"></i></span><strong>AI 咨询</strong><small>即时陪伴与拆解</small></a>
            <a className="workflow-row" href="/assessment"><span><i className="fa-solid fa-brain"></i></span><strong>心理测评</strong><small>复测 GAD-7 / PHQ-9</small></a>
          </div>
        </article>
      </section>

      <section className="ops-grid lower">
        <article className="table-card">
          <div className="between">
            <div>
              <span className="eyebrow"><i className="fa-solid fa-bell" style={{ marginRight: '8px' }}></i> 通知队列</span>
              <h2>近期事项</h2>
            </div>
            <span className="tag">{queue.length} items</span>
          </div>
          <div className="list">
            {queue.map((item) => <article className="record queue-row" key={item.id}><b>{item.title}</b><p className="muted">{item.content}</p></article>)}
          </div>
        </article>

        <article className="table-card">
          <span className="eyebrow"><i className="fa-solid fa-shield-halved" style={{ marginRight: '8px' }}></i> 风险观察</span>
          <h2>处置建议</h2>
          <div className="risk-matrix">
            <div><span>情绪波动</span><b>中低</b></div>
            <div><span>睡眠关联</span><b>需观察</b></div>
            <div><span>建议复测</span><b>GAD-7</b></div>
          </div>
        </article>
      </section>
    </div>
  );
}
