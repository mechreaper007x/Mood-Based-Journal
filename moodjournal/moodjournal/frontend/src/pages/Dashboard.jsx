import { ArrowUpRight, BookOpen, CalendarDays, PenLine } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/axios';

const moodTone = {
  HAPPY: '#d2a84f', JOYFUL: '#d2a84f', EXCITED: '#d2a84f',
  CALM: '#78977a', CONTENT: '#78977a', NEUTRAL: '#a4a397',
  SAD: '#8297ad', ANXIOUS: '#c68d6c', ANGRY: '#bc786b',
  ENERGETIC: '#b58b9e', PRODUCTIVE: '#8b9c79',
};
const formatDate = (value) => {
  if (!value) return new Date();
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
};

const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState([]);
  const [entries, setEntries] = useState([]);
  const [quote, setQuote] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    const fetchData = async () => {
      const [statsRes, quoteRes, entriesRes] = await Promise.allSettled([
        api.get('/journal/stats'), api.get('/ai/daily-quote'), api.get('/journal')
      ]);
      if (!mounted) return;
      if (statsRes.status === 'fulfilled') setStats(Array.isArray(statsRes.value.data) ? statsRes.value.data : []);
      if (quoteRes.status === 'fulfilled' && quoteRes.value.data) setQuote(quoteRes.value.data);
      if (entriesRes.status === 'fulfilled') setEntries(Array.isArray(entriesRes.value.data) ? entriesRes.value.data : []);
      setLoading(false);
    };
    fetchData();
    return () => { mounted = false; };
  }, []);

  const totalEntries = useMemo(() => stats.reduce((sum, item) => sum + (Number(item.count) || 0), 0), [stats]);
  const leadingMood = useMemo(() => stats.length ? [...stats].sort((a,b) => b.count-a.count)[0].mood : '—', [stats]);
  const recentEntries = useMemo(() => [...entries].sort((a,b) => formatDate(b.createdAt)-formatDate(a.createdAt)).slice(0,3), [entries]);
  const quoteText = quote?.quote || quote?.text || quote?.content || 'You do not have to make sense of everything today. Some days are just for noticing.';
  const firstName = user?.username?.split(/[\s@]/)[0] || 'there';
  const today = new Date();

  return (
    <div>
      <div className="welcome-row">
        <div><div className="page-kicker">Your journal · {today.toLocaleDateString('en-US',{month:'long',year:'numeric'})}</div>
          <h1 className="page-title">A little space to<br/>check in, {firstName}.</h1>
          <p className="page-intro">A softer place to land at the end of a day.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/journal" className="secondary-action"><BookOpen size={15}/> Browse notes</Link>
          <Link to="/entry" className="primary-action"><PenLine size={15}/> Write an entry</Link>
        </div>
      </div>

      <section className="paper-card checkin-card">
        <div className="checkin-copy">
          <div className="card-eyebrow">A moment for yourself</div>
          <h2 className="checkin-title">What's taking up space<br/>in your mind today?</h2>
          <p>It doesn't need to be profound, polished, or even complete. Start wherever you are.</p>
          <Link to="/entry" className="primary-action">Start writing <ArrowUpRight size={15}/></Link>
        </div>
      </section>

      <section className="stat-strip" aria-label="Journal summary">
        <div className="stat-card"><div className="stat-label">Pages written</div><div className="stat-value">{loading ? '—' : totalEntries}</div><div className="stat-foot">Every entry counts</div></div>
        <div className="stat-card"><div className="stat-label">Most visited feeling</div><div className="stat-value text-[20px] capitalize">{loading ? '—' : leadingMood.toLowerCase()}</div><div className="stat-foot">A pattern, not a label</div></div>
        <div className="stat-card"><div className="stat-label">This month</div><div className="stat-value">{loading ? '—' : entries.filter(e => { const d = formatDate(e.createdAt); return d.getMonth() === today.getMonth() && d.getFullYear() === today.getFullYear(); }).length}</div><div className="stat-foot">Moments you made time for</div></div>
      </section>

      <div className="dashboard-grid">
        <section className="paper-card">
          <div className="section-row"><div><div className="card-eyebrow">Looking back</div><h3 className="card-heading">Your recent pages</h3></div><Link to="/journal" className="text-link">All entries <ArrowUpRight size={12} className="inline"/></Link></div>
          {loading ? <div className="empty-note">Gathering your pages…</div> : recentEntries.length ? <div className="entry-list">
            {recentEntries.map((entry) => { const date = formatDate(entry.createdAt); return <Link to={`/entry/${entry.id}`} key={entry.id} className="entry-preview no-underline text-inherit">
              <div className="entry-date"><strong>{date.getDate()}</strong><span>{date.toLocaleDateString('en-US',{month:'short'})}</span></div>
              <div className="entry-info"><strong>{entry.title || 'Untitled reflection'}</strong><span>{date.toLocaleDateString('en-US',{weekday:'long',year:'numeric'})}</span></div>
              {entry.mood && <span className="mood-pill" style={{background:`${moodTone[entry.mood] || '#8b9c79'}22`,color:moodTone[entry.mood] || '#60745e'}}>{entry.mood.toLowerCase()}</span>}
            </Link>; })}
          </div> : <div className="empty-note">Your first page is waiting. It can be just one sentence.</div>}
        </section>

        <section className="paper-card quote-card">
          <div className="card-eyebrow">A thought to keep</div>
          <div className="quote-mark">“</div>
          <p className="quote-text">{quoteText}</p>
          <div className="quote-author">{quote?.author || 'A note for today'}</div>
        </section>
      </div>

      <div className="dashboard-grid mt-[18px]">
        <section className="paper-card">
          <div className="card-eyebrow">A gentle pattern</div><h3 className="card-heading">Feelings you've noted</h3>
          {stats.length ? <div className="mood-list">{[...stats].sort((a,b)=>b.count-a.count).slice(0,5).map(item => <div className="mood-row" key={item.mood}>
            <span className="capitalize">{String(item.mood).toLowerCase()}</span><div className="mood-track"><div className="mood-fill" style={{width:`${Math.max(3,(item.count/Math.max(totalEntries,1))*100)}%`,background:moodTone[item.mood] || '#78977a'}}/></div><span className="text-right">{item.count}</span>
          </div>)}</div> : <div className="empty-note">As you write, your patterns will gently take shape here.</div>}
        </section>
        <section className="paper-card flex flex-col justify-between">
          <div><div className="card-eyebrow">Make room for a thought</div><h3 className="card-heading">You don't have to carry it all in your head.</h3><p className="text-[12px] leading-6 text-stone-500 mt-3">A few honest lines can be enough for today. Your journal is yours; there is no score to chase.</p></div>
          <Link to="/entry" className="secondary-action self-start mt-6"><CalendarDays size={15}/> Make a note</Link>
        </section>
      </div>
    </div>
  );
};

export default Dashboard;
