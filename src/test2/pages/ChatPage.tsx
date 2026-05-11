import { FormEvent, useEffect, useRef, useState } from "react";
import { chatApi } from "@/api";

function renderMarkdown(text: string) {
  const escaped = String(text || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  return escaped
    .replace(/^### (.*)$/gm, "<h3>$1</h3>")
    .replace(/^## (.*)$/gm, "<h2>$1</h2>")
    .replace(/^# (.*)$/gm, "<h1>$1</h1>")
    .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/^\- (.*)$/gm, "<li>$1</li>")
    .replace(/\n/g, "<br />");
}

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
const prompts = ["我现在很焦虑，想先稳定下来", "帮我复盘今天的情绪波动", "睡前脑子停不下来怎么办？", "我想把压力拆成下一步行动"];

export function ChatPage() {
  const [sessions, setSessions] = useState<any[]>([]);
  const [messages, setMessages] = useState<any[]>([]);
  const [activeSession, setActiveSession] = useState("");
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const chatRef = useRef<HTMLDivElement>(null);

  function scrollBottom() {
    window.requestAnimationFrame(() => {
      if (chatRef.current) chatRef.current.scrollTop = chatRef.current.scrollHeight;
    });
  }

  async function loadSessions() {
    const response = await chatApi.sessions(20);
    setSessions(response.data.sessions || []);
  }

  async function createSession() {
    const response = await chatApi.createSession();
    setActiveSession(response.data.sessionId);
    setMessages([]);
    await loadSessions();
    return response.data.sessionId;
  }

  async function openSession(sessionId: string) {
    setError("");
    setActiveSession(sessionId);
    const response = await chatApi.history(sessionId);
    setMessages(response.data.messages || []);
    scrollBottom();
  }

  async function send(event: FormEvent) {
    event.preventDefault();
    if (!draft.trim() || sending) return;
    setSending(true);
    setError("");
    const text = draft.trim();
    setDraft("");
    try {
      const sessionId = activeSession || await createSession();
      const base = [...messages, { sender: "user", content: text }, { sender: "ai", content: "", streaming: true }];
      setMessages(base);
      scrollBottom();
      const reader = (await chatApi.stream(sessionId, text)).getReader();
      const decoder = new TextDecoder();
      let aiText = "";
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value, { stream: true });
        for (const char of chunk) {
          aiText += char;
          setMessages([...base.slice(0, -1), { sender: "ai", content: aiText, streaming: true }]);
          scrollBottom();
          await wait(8);
        }
      }
      setMessages([...base.slice(0, -1), { sender: "ai", content: aiText, streaming: false }]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "消息发送失败");
    } finally {
      setSending(false);
    }
  }

  useEffect(() => { loadSessions().catch(() => setSessions([])); }, []);

  return (
    <div className="chat-console">
      <section className="card session-pane">
        <div className="between"><div><span className="eyebrow">会话</span><h2>咨询记录</h2></div><button className="ghost" onClick={createSession}>新建</button></div>
        <div className="list">{sessions.map((item) => <button className={`record session-record ${item.sessionId === activeSession ? "active" : ""}`} key={item.sessionId} onClick={() => openSession(item.sessionId)}><b>{item.sessionTitle || "新的对话"}</b><span>{String(item.createTime || "").replace("T", " ").slice(0, 16)}</span></button>)}</div>
      </section>
      <section className="card chat-surface">
        <div className="chat" ref={chatRef}>
          {!messages.length && <div className="chat-empty-state">
            <div className="pulse-mark">AI</div>
            <h2>从一个具体感受开始</h2>
            <p className="muted">系统会保留上下文，适合连续追问、复盘和行动拆解。</p>
            <div className="prompt-grid">{prompts.map((prompt) => <button key={prompt} onClick={() => setDraft(prompt)}>{prompt}</button>)}</div>
          </div>}
          {messages.map((item, index) => <div key={index} className={`bubble ${item.sender === "user" ? "user" : "ai"}`} dangerouslySetInnerHTML={{ __html: renderMarkdown(item.content || (item.streaming ? "正在组织回应..." : "")) }} />)}
        </div>
        {error && <p className="feedback-note danger-note">{error}</p>}
        <form className="chat-composer" onSubmit={send}>
          <input className="input" value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="输入消息，支持 Markdown 响应" disabled={sending} />
          <button className="primary" disabled={sending || !draft.trim()}>{sending && <span className="spinner" />}{sending ? "回应中" : "发送"}</button>
        </form>
      </section>
    </div>
  );
}
