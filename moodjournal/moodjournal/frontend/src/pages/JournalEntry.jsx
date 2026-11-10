import { BedDouble, Mic, MicOff, Save } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import CrisisModal from '../components/CrisisModal';
import api from '../lib/axios';

const CONTEXT_TAGS = [
  { id: 'work', label: 'Work' }, { id: 'family', label: 'Family' },
  { id: 'health', label: 'Health' }, { id: 'relationships', label: 'Relationships' },
  { id: 'self', label: 'Self' }, { id: 'money', label: 'Finances' },
  { id: 'academic', label: 'Academic' }, { id: 'social', label: 'Social' },
];

const JournalEntry = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [showCrisisModal, setShowCrisisModal] = useState(false);
  const [detectedRiskScore, setDetectedRiskScore] = useState(0);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef(null);
  const [contextTags, setContextTags] = useState([]);
  const [stressLevel, setStressLevel] = useState(5);
  const [energyLevel, setEnergyLevel] = useState(5);
  const [sleepQuality, setSleepQuality] = useState(3);
  const [triggerDescription, setTriggerDescription] = useState('');

  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return undefined;
    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = 'en-US';
    recognition.onresult = (event) => {
      let finalTranscript = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        if (event.results[i].isFinal) finalTranscript += event.results[i][0].transcript + ' ';
      }
      if (finalTranscript) setContent(prev => prev + finalTranscript);
    };
    recognition.onerror = () => setIsListening(false);
    recognition.onend = () => setIsListening(false);
    recognitionRef.current = recognition;
    return () => { recognition.stop(); recognitionRef.current = null; };
  }, []);

  const toggleListening = () => {
    if (!recognitionRef.current) { window.alert('Speech recognition is not available in this browser.'); return; }
    if (isListening) { recognitionRef.current.stop(); setIsListening(false); }
    else { try { recognitionRef.current.start(); setIsListening(true); } catch { setIsListening(false); } }
  };

  const toggleTag = (tagId) => setContextTags(prev => prev.includes(tagId) ? prev.filter(tag => tag !== tagId) : [...prev, tagId]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    try {
      const response = await api.post('/journal', { title, content, contextTags, stressLevel, energyLevel, sleepQuality, triggerDescription: triggerDescription || null });
      const entry = response.data;
      if (entry.riskScore && entry.riskScore >= 7) {
        setDetectedRiskScore(entry.riskScore);
        setShowCrisisModal(true);
      } else navigate('/dashboard');
    } catch (error) {
      window.alert('Could not save your entry: ' + (error.response?.data?.error || error.message));
    } finally { setLoading(false); }
  };

  const Slider = ({ label, low, high, value, onChange }) => <div>
    <div className="flex justify-between items-center mb-3"><label className="journal-field-label mb-0">{label}</label><span className="text-xs font-semibold text-[#536b57]">{value}/10</span></div>
    <input className="journal-range" type="range" min="1" max="10" value={value} onChange={event => onChange(Number(event.target.value))}/>
    <div className="flex justify-between text-[10px] text-stone-400 mt-1"><span>{low}</span><span>{high}</span></div>
  </div>;

  return <div className="journal-wrap">
    <div className="mb-7"><div className="page-kicker">A blank page, a fresh start</div><h1 className="page-title">Put it into words.</h1><p className="page-intro">No audience. No judgement. Just a place to be honest.</p></div>
    <form onSubmit={handleSubmit} className="journal-paper">
      <div className="flex items-center justify-between mb-5"><span className="card-eyebrow">Today's entry</span><span className="text-[10px] text-stone-400">{new Date().toLocaleDateString('en-US',{weekday:'long',month:'long',day:'numeric'})}</span></div>
      <input className="journal-input journal-title-input" type="text" value={title} onChange={event=>setTitle(event.target.value)} placeholder="Give this page a name…" required aria-label="Entry title"/>
      <div className="flex justify-between items-center mt-7 mb-1"><label className="journal-field-label mb-0">Let your thoughts wander</label><button type="button" onClick={toggleListening} className={`inline-flex items-center gap-2 rounded-full px-3 py-2 text-[10px] font-bold ${isListening ? 'bg-red-50 text-red-600' : 'bg-[#edf0e9] text-[#536b57]'}`}>{isListening ? <MicOff size={14}/> : <Mic size={14}/>} {isListening ? 'Listening…' : 'Speak instead'}</button></div>
      <textarea className="journal-input journal-textarea" value={content} onChange={event=>setContent(event.target.value)} placeholder="Start anywhere. What happened? How did it feel? What do you wish someone understood?" required aria-label="Your journal entry"/>
      <hr className="journal-divider"/>
      <div className="mb-6"><div className="journal-field-label">What was around you? <span className="font-normal text-stone-400">(optional)</span></div><div className="flex flex-wrap gap-2">{CONTEXT_TAGS.map(tag=><button type="button" key={tag.id} onClick={()=>toggleTag(tag.id)} className={`tag-choice ${contextTags.includes(tag.id) ? 'selected' : ''}`}>{tag.label}</button>)}</div></div>
      <div className="mb-6"><label className="journal-field-label" htmlFor="trigger">Anything that set the tone?</label><input id="trigger" className="journal-input" value={triggerDescription} onChange={event=>setTriggerDescription(event.target.value)} placeholder="A conversation, a small win, something on your mind…"/></div>
      <div className="journal-meta-grid rounded-xl bg-[#f7f6f0] p-5">
        <Slider label="Stress" low="At ease" high="A lot going on" value={stressLevel} onChange={setStressLevel}/>
        <Slider label="Energy" low="Running low" high="Full of energy" value={energyLevel} onChange={setEnergyLevel}/>
        <div><div className="flex items-center gap-2 mb-3"><BedDouble size={15} className="text-[#71866e]"/><span className="journal-field-label mb-0">Sleep quality</span></div><div className="flex gap-1">{[1,2,3,4,5].map(star=><button type="button" key={star} aria-label={`${star} out of 5`} onClick={()=>setSleepQuality(star)} className={`sleep-choice ${star<=sleepQuality?'selected':''}`}>★</button>)}</div><div className="text-[10px] text-stone-400 mt-2">{['','Rough','Not great','Okay','Good','Restful'][sleepQuality]}</div></div>
      </div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mt-7"><p className="text-[10px] leading-5 text-stone-400 max-w-sm">Your entry is saved to your private journal. You can come back to it whenever you're ready.</p><button type="submit" disabled={loading} className="primary-action disabled:opacity-50"><Save size={15}/>{loading ? 'Saving your page…' : 'Save this page'}</button></div>
    </form>
    <CrisisModal isOpen={showCrisisModal} onClose={()=>{setShowCrisisModal(false);navigate('/dashboard');}} riskScore={detectedRiskScore}/>
  </div>;
};

export default JournalEntry;
