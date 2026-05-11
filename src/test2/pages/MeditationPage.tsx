import { useEffect, useMemo, useState } from "react";

const programs = [
  { name: "晨间稳定", icon: "☀️", detail: "启动注意力与身体感", guidance: "坐直，感受脚底和椅面的支撑。" },
  { name: "焦虑降噪", icon: "🌿", detail: "把警觉状态调低", guidance: "允许紧张存在，然后把注意力放回呼气。" },
  { name: "睡前呼吸", icon: "🌙", detail: "进入更慢的睡前节奏", guidance: "放松下颌、肩颈和手指，准备结束一天。" }
];
const steps = [
  { name: "吸气", text: "4 秒吸气" },
  { name: "停留", text: "4 秒停留" },
  { name: "呼气", text: "4 秒呼气" }
];

export function MeditationPage() {
  const [active, setActive] = useState(programs[0].name);
  const [running, setRunning] = useState(false);
  const [seconds, setSeconds] = useState(300);
  const current = programs.find((item) => item.name === active) || programs[0];
  const phase = seconds % 12;
  const breathLabel = phase < 4 ? "吸气" : phase < 8 ? "停留" : "呼气";
  const timeText = useMemo(() => `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`, [seconds]);
  const progress = Math.round(((300 - seconds) / 300) * 100);

  useEffect(() => {
    if (!running) return undefined;
    const timer = window.setInterval(() => setSeconds((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [running]);

  useEffect(() => {
    setRunning(false);
    setSeconds(300);
  }, [active]);

  useEffect(() => {
    if (seconds === 0) setRunning(false);
  }, [seconds]);

  return (
    <div className="meditation-console">
      <section className="card meditation-stage">
        <div>
          <span className="eyebrow">冥想训练</span>
          <h2>{active}</h2>
          <p className="muted">{current.guidance}</p>
          <button className="primary" onClick={() => setRunning((value) => !value)}>{running ? "暂停" : "开始"}</button>
        </div>
        <div className="timer-module">
          <div className={`breath-orb ${running ? "running" : ""}`}>{breathLabel}</div>
          <b>{timeText}</b>
        </div>
      </section>

      <section className="practice-row">
        {programs.map((program) => <button className={`practice-card ${program.name === active ? "active" : ""}`} key={program.name} onClick={() => setActive(program.name)}><span>{program.icon}</span><b>{program.name}</b><small>{program.detail}</small></button>)}
      </section>

      <section className="table-card">
        <div className="between"><div><span className="eyebrow">呼吸节奏</span><h2>{breathLabel}，保持注意力在身体</h2></div><span className="tag">{progress}%</span></div>
        <div className="breath-steps">{steps.map((step) => <div className={step.name === breathLabel ? "active" : ""} key={step.name}><b>{step.name}</b><span>{step.text}</span></div>)}</div>
        <div className="session-progress"><i style={{ width: `${progress}%` }} /></div>
      </section>
    </div>
  );
}
