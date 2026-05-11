import { FormEvent, useEffect, useMemo, useState } from "react";
import { authApi } from "@/api";
import { useSession } from "@/state/session";

export function ProfilePage() {
  const session = useSession();
  const [nickname, setNickname] = useState("");
  const [avatar, setAvatar] = useState("");
  const [avatarOk, setAvatarOk] = useState(true);
  const [message, setMessage] = useState("");
  const [failed, setFailed] = useState(false);
  const [saving, setSaving] = useState(false);
  const initials = useMemo(() => (nickname || session.user?.username || "ME").slice(0, 2).toUpperCase(), [nickname, session.user?.username]);
  const roleText = session.user?.role?.toUpperCase() === "ADMIN" ? "管理员" : session.user?.role?.toUpperCase() === "COUNSELOR" ? "咨询师" : "普通用户";
  const createDate = String(session.user?.createTime || "").replace("T", " ").slice(0, 10) || "-";

  useEffect(() => {
    if (!session.user) {
      session.fetchUser().catch(() => undefined);
      return;
    }
    setNickname(session.user.nickname || "");
    setAvatar(session.user.avatar || "");
  }, [session]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setFailed(false);
    try {
      const response = await authApi.updateProfile({ nickname, avatar });
      setMessage(response.message || "资料已保存");
      session.fetchUser().catch(() => undefined);
    } catch (err) {
      setFailed(true);
      setMessage(err instanceof Error ? err.message : "保存失败，请稍后再试");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="profile-layout">
      <section className="card profile-card">
        <div className="profile-avatar">
          {avatar && avatarOk ? <img src={avatar} alt="用户头像" onError={() => setAvatarOk(false)} /> : <span>{initials}</span>}
        </div>
        <h2>{nickname || session.user?.username || "MindEase 用户"}</h2>
        <p className="muted">{roleText} · {session.user?.username}</p>
        <div className="profile-stats">
          <div><b>{session.user?.status ?? "-"}</b><span>账号状态</span></div>
          <div><b>{createDate}</b><span>加入时间</span></div>
        </div>
      </section>

      <form className="form-card stack profile-form" onSubmit={submit}>
        <div><span className="eyebrow">个人资料</span><h2>基础信息</h2><p className="muted">昵称和头像会显示在工作台、咨询记录和个人中心。</p></div>
        <label className="field"><span>昵称</span><input className="input" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="请输入昵称" /></label>
        <label className="field"><span>头像 URL</span><input className="input" value={avatar} onChange={(e) => { setAvatar(e.target.value); setAvatarOk(true); }} placeholder="https://..." /></label>
        <button className="primary" disabled={saving}>{saving && <span className="spinner" />}{saving ? "保存中" : "保存资料"}</button>
        {message && <p className={`feedback-note ${failed ? "danger-note" : ""}`}>{message}</p>}
      </form>
    </div>
  );
}
