import { useEffect, useState } from "react";
import { reportApi } from "@/api";

export function ReportPage() {
  const [report, setReport] = useState<any>({});

  useEffect(() => {
    reportApi.emotion().then((res) => setReport(res.data)).catch(() => setReport({}));
  }, []);

  return (
    <div className="grid">
      <section className="metric span-3"><span className="muted">报告周期</span><b>{report.period || "-"}</b></section>
      <section className="metric span-3"><span className="muted">平均情绪</span><b>{Number(report.avgScore || 0).toFixed(1)}</b></section>
      <section className="metric span-3"><span className="muted">积极占比</span><b>{Math.round((report.positiveRate || 0) * 100)}%</b></section>
      <section className="metric span-3"><span className="muted">连续记录</span><b>{report.continuousDays || 0}</b></section>
      <section className="card span-12">
        <h2>AI 建议</h2>
        <div className="list">{(report.aiSuggestions || [report.summary || report.aiSummary || "暂无报告数据"]).map((item: string, index: number) => <article className="record" key={index}>{item}</article>)}</div>
      </section>
    </div>
  );
}
