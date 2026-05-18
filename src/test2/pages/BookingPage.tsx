import { useState } from "react";
import { useParams } from "react-router-dom";
import { appointmentApi } from "@/api";

export function BookingPage() {
  const { counselorId = "" } = useParams();
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [note, setNote] = useState("");
  const [slots, setSlots] = useState<any[]>([]);
  const [selected, setSelected] = useState<any>(null);
  const [message, setMessage] = useState("");

  async function loadSlots() {
    const response = await appointmentApi.slots(counselorId, date);
    setSlots(response.data.slots || []);
  }

  async function book() {
    if (!selected) return;
    const response = await appointmentApi.create({ counselorId: Number(counselorId), startTime: selected.startTime, endTime: selected.endTime, userNote: note });
    setMessage(response.message || "预约成功");
  }

  return (
    <div className="grid">
      <section className="form-card span-5 stack">
        <h2>预约</h2>
        <input className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <button className="ghost" onClick={loadSlots}>查询时段</button>
        <textarea className="textarea" value={note} onChange={(e) => setNote(e.target.value)} placeholder="备注" />
        <button className="primary" onClick={book} disabled={!selected}>提交预约</button>
        <p className="muted">{message}</p>
      </section>
      <section className="table-card span-7">
        <h2>可用时段</h2>
        <div className="list">{slots.map((slot) => <button className="record" key={slot.startTime} disabled={!slot.available} onClick={() => setSelected(slot)}>{slot.startTime} - {slot.endTime} · {slot.available ? "可预约" : "已占用"}</button>)}</div>
      </section>
    </div>
  );
}
