import { useEffect, useState } from "react";
import { appointmentApi } from "@/api";

export function AppointmentsPage() {
  const [status, setStatus] = useState("");
  const [items, setItems] = useState<any[]>([]);

  async function load() {
    const response = await appointmentApi.mine({ status: status || undefined, page: 1, pageSize: 20 });
    setItems(response.data.list || response.data.appointments || []);
  }

  async function cancel(id: number) {
    await appointmentApi.cancel(id, "用户主动取消");
    await load();
  }

  useEffect(() => { load().catch(() => undefined); }, []);

  return (
    <section className="table-card">
      <div className="between">
        <h2>预约列表</h2>
        <select className="select" style={{ maxWidth: 160 }} value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">全部</option><option value="pending">待确认</option><option value="confirmed">已确认</option><option value="cancelled">已取消</option>
        </select>
        <button className="ghost" onClick={load}>筛选</button>
      </div>
      <div className="list">{items.map((item) => <article className="record between" key={item.id}><div><b>{item.targetName || item.counselorName}</b><p className="muted">{item.startTime} - {item.endTime} · {item.status}</p></div><button className="danger" onClick={() => cancel(item.id)}>取消</button></article>)}</div>
    </section>
  );
}
