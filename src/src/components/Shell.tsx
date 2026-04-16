import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { homeForUser, useSession } from "@/state/session";

const titles: Record<string, string> = {
  "/home": "患者支持总览",
  "/mood-diary": "情绪记录",
  "/ai-chat": "AI 咨询会话",
  "/assessment": "心理测评",
  "/counselor-list": "咨询师检索",
  "/my-appointments": "预约管理",
  "/emotion-report": "情绪报告",
  "/meditation": "冥想训练",
  "/profile": "个人资料",
  "/counselor/dashboard": "咨询师工作台",
  "/counselor/audit-pending": "资质审核",
  "/admin/dashboard": "管理控制台"
};

const icons: Record<string, string> = {
  "/home": "fa-solid fa-house-chimney",
  "/mood-diary": "fa-solid fa-book-journal-whills",
  "/ai-chat": "fa-solid fa-robot",
  "/assessment": "fa-solid fa-brain",
  "/counselor-list": "fa-solid fa-user-doctor",
  "/my-appointments": "fa-solid fa-calendar-check",
  "/emotion-report": "fa-solid fa-chart-line",
  "/meditation": "fa-solid fa-om",
  "/profile": "fa-solid fa-user-astronaut",
  "/counselor/dashboard": "fa-solid fa-stethoscope",
  "/counselor/audit-pending": "fa-solid fa-file-signature",
  "/admin/dashboard": "fa-solid fa-server"
};

export function Shell() {
  const session = useSession();
  const location = useLocation();
  const navigate = useNavigate();
  const [globalLoading, setGlobalLoading] = useState(false);
  const [avatarOk, setAvatarOk] = useState(true);

  useEffect(() => {
    const listener = (event: Event) => setGlobalLoading(Boolean((event as CustomEvent).detail?.loading));
    window.addEventListener("mindease:loading", listener);
    if (session.token && !session.user) {
      session.fetchUser().catch(() => {
        session.logout();
        navigate("/login");
      });
    }
    return () => window.removeEventListener("mindease:loading", listener);
  }, [navigate, session]);

  const role = session.user?.role?.toUpperCase();
  const links =
    role === "ADMIN"
      ? [{ to: "/admin/dashboard", label: "管理控制台" }, { to: "/profile", label: "个人资料" }]
      : role === "COUNSELOR"
        ? session.user?.status === 2
          ? [{ to: "/counselor/audit-pending", label: "资质审核" }]
          : [{ to: "/counselor/dashboard", label: "咨询工作台" }, { to: "/profile", label: "个人资料" }]
        : [
            { to: "/home", label: "总览" },
            { to: "/mood-diary", label: "情绪记录" },
            { to: "/ai-chat", label: "AI 咨询" },
            { to: "/assessment", label: "测评" },
            { to: "/counselor-list", label: "咨询师" },
            { to: "/my-appointments", label: "预约" },
            { to: "/emotion-report", label: "报告" },
            { to: "/meditation", label: "冥想" }
          ];

  const logout = () => {
    session.logout();
    navigate("/login");
  };

  return (
    <div className="workspace">
      <div className={`top-progress ${globalLoading ? "active" : ""}`} />
      <div className={`loading-toast ${globalLoading ? "active" : ""}`}><span className="spinner" />正在同步数据</div>
      <aside className="rail">
        <button className="logo" onClick={() => navigate(homeForUser(session.user?.role, session.user?.status))}>ME</button>
        <nav className="nav-list">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to}>
              <i className={icons[link.to] || "fa-solid fa-star"} style={{ marginRight: '8px' }}></i>
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="operator-card">
          <div className="mini-profile">
            {session.user?.avatar && avatarOk ? <img src={session.user.avatar} alt="头像" onError={() => setAvatarOk(false)} /> : <span>{(session.user?.nickname || session.user?.username || "ME").slice(0, 2).toUpperCase()}</span>}
            <div>
              <strong>{session.user?.nickname || "MindEase"}</strong>
              <span>{session.user?.role || "user"}</span>
            </div>
          </div>
          <button className="ghost" onClick={logout}><i className="fa-solid fa-power-off"></i> 退出</button>
        </div>
      </aside>
      <main className="main">
        <header className="masthead">
          <div>
            <h1><i className={icons[location.pathname] || "fa-solid fa-meteor"} style={{ color: 'var(--teal)', marginRight: '12px' }}></i> {titles[location.pathname] || "MindEase Clinic Console"}</h1>
            <p>集中管理情绪记录、心理测评、咨询预约与个人资料。</p>
          </div>
          <button className="ghost" onClick={() => navigate("/profile")}><i className="fa-solid fa-user-astronaut"></i> 个人资料</button>
        </header>
        <Outlet />
      </main>
    </div>
  );
}
