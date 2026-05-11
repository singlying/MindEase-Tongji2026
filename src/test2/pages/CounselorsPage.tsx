import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { counselorApi } from "@/api";

export function CounselorsPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState<"smart" | "price_asc" | "rating_desc">("smart");
  const [status, setStatus] = useState<any>(null);
  const [counselors, setCounselors] = useState<any[]>([]);

  async function load() {
    setStatus((await counselorApi.recommendStatus().catch(() => ({ data: null }))).data);
    const response = await counselorApi.recommend({ keyword, sort });
    setCounselors(response.data.counselors || []);
  }

  useEffect(() => { load().catch(() => undefined); }, []);
  const fallbackAvatar = (name = "ME") => `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=E7EEF4&color=111827&size=128`;

  return (
    <div className="grid">
      <section className="form-card span-4 stack">
        <h2>检索条件</h2>
        <input className="input" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="关键词" />
        <select className="select" value={sort} onChange={(e) => setSort(e.target.value as typeof sort)}>
          <option value="smart">智能排序</option><option value="price_asc">价格优先</option><option value="rating_desc">评分优先</option>
        </select>
        <button className="primary" onClick={load}>查询</button>
        <p className="muted">推荐状态：{status?.recommendationReady ? "可推荐" : "数据不足或待后端返回"}</p>
      </section>
      <section className="table-card span-8">
        <h2>咨询师</h2>
        <div className="list">{counselors.map((item) => <article className="record between" key={item.id}><div className="row"><img className="avatar-img" src={item.avatar || fallbackAvatar(item.realName)} alt={item.realName} onError={(event) => { event.currentTarget.src = fallbackAvatar(item.realName); }} /><div><b>{item.realName}</b><p className="muted">{item.title} · ￥{item.pricePerHour}/小时</p>{(item.tags || item.specialty || []).map((tag: string) => <span className="tag" key={tag}>{tag}</span>)}</div></div><button className="primary" onClick={() => navigate(`/booking/${item.id}`)}>预约</button></article>)}</div>
      </section>
    </div>
  );
}
