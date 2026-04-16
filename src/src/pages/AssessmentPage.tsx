import { useEffect, useState } from "react";
import { assessmentApi } from "@/api";

export function AssessmentPage() {
  const [scales, setScales] = useState<any[]>([]);
  const [records, setRecords] = useState<any[]>([]);

  async function load() {
    const scaleResponse = await assessmentApi.scales();
    setScales(scaleResponse.data.scales || scaleResponse.data.list || scaleResponse.data || []);
    const recordResponse = await assessmentApi.records({ limit: 8, offset: 0 }).catch(() => ({ data: { records: [] } }));
    setRecords(recordResponse.data.records || recordResponse.data.list || []);
  }

  useEffect(() => { load().catch(() => undefined); }, []);
  const fallbackCover = "https://images.unsplash.com/photo-1493836512294-502baa1986e2?auto=format&fit=crop&w=320&q=70";

  return (
    <div className="grid">
      <section className="table-card span-7">
        <div className="between"><h2>量表</h2><button className="ghost" onClick={load}>刷新</button></div>
        <div className="list">{scales.map((scale) => <article className="record row" key={scale.scaleKey || scale.id}><img className="cover-img" src={scale.coverUrl || fallbackCover} alt={scale.title || scale.name || scale.scaleName} onError={(event) => { event.currentTarget.src = fallbackCover; }} /><div><b>{scale.title || scale.name || scale.scaleName}</b><p className="muted">{scale.description || "可进入作答"}</p></div></article>)}</div>
      </section>
      <section className="table-card span-5">
        <h2>历史</h2>
        <div className="list">{records.map((record) => <article className="record" key={record.id || record.recordId}><b>{record.scaleName || record.scaleKey}</b><p className="muted">{record.resultLevel || record.createTime}</p></article>)}</div>
      </section>
    </div>
  );
}
