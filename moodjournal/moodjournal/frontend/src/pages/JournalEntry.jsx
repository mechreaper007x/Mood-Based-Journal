import { motion } from 'framer-motion';
import { BedDouble, Brain, Flame, Mic, MicOff, Save, Zap } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/axios';

const CONTEXT_TAGS = [
  { id: 'work', label: 'Work', icon: '💼' },
  { id: 'family', label: 'Family', icon: '👨‍👩‍👧' },
  { id: 'health', label: 'Health', icon: '🏥' },
  { id: 'relationships', label: 'Relationships', icon: '💕' },
  { id: 'self', label: 'Self', icon: '🪞' },
  { id: 'money', label: 'Finances', icon: '💰' },
  { id: 'academic', label: 'Academic', icon: '📚' },
  { id: 'social', label: 'Social', icon: '👥' },
];

const JournalEntry = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  
  // Form state
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  
  // Voice to Text State
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef(null);

  useEffect(() => {
    // Initialize speech recognition
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = 'en-US';

      recognition.onresult = (event) => {
        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const transcript = event.results[i][0].transcript;
          if (event.results[i].isFinal) {
            finalTranscript += transcript + ' ';
          } else {
            interimTranscript += transcript;
          }
        }
        
        if (finalTranscript) {
          setContent(prev => prev + finalTranscript);
        }
      };

      recognition.onerror = (event) => {
        console.error('Speech recognition error', event.error);
        setIsListening(false);
      };

      recognition.onend = () => {
        if (isListening) {
            // If supposed to be listening but stopped (e.g. silence), restart
             // tailored logic can go here, but usually we just let user restart
             setIsListening(false);
        }
      };

      recognitionRef.current = recognition;
    }
  }, []);

  const toggleListening = () => {
    if (!recognitionRef.current) {
        alert("Your browser does not support speech recognition.");
        return;
    }

    if (isListening) {
      recognitionRef.current.stop();
      setIsListening(false);
    } else {
      recognitionRef.current.start();
      setIsListening(true);
    }
  };


  // New structured context
  const [contextTags, setContextTags] = useState([]);
  const [stressLevel, setStressLevel] = useState(5);

  const [energyLevel, setEnergyLevel] = useState(5);
  const [sleepQuality, setSleepQuality] = useState(3);
  const [triggerDescription, setTriggerDescription] = useState('');

  const toggleTag = (tagId) => {
    setContextTags(prev => 
      prev.includes(tagId) 
        ? prev.filter(t => t !== tagId) 
        : [...prev, tagId]
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      await api.post('/journal', {
        title,
        content,
        contextTags,
        stressLevel,
        energyLevel,
        sleepQuality,
        triggerDescription: triggerDescription || null,
      });
      navigate('/dashboard');
    } catch (error) {
      console.error("Failed to save", error);
      alert("Failed to save: " + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  // Slider component
  const SliderInput = ({ label, icon: Icon, value, onChange, min = 1, max = 10, lowLabel, highLabel, color }) => (
    <div className="mb-4">
      <div className="flex items-center gap-2 mb-2">
        <Icon className={`text-${color}`} size={18} />
        <label className="text-gray-300 text-sm font-medium">{label}</label>
        <span className={`ml-auto text-${color} font-bold`}>{value}/{max}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        value={value}
        onChange={e => onChange(parseInt(e.target.value))}
        className={`w-full accent-${color}`}
        style={{ accentColor: color === 'red-400' ? '#f87171' : color === 'yellow-400' ? '#facc15' : '#60a5fa' }}
      />
      <div className="flex justify-between text-xs text-gray-500 mt-1">
        <span>{lowLabel}</span>
        <span>{highLabel}</span>
      </div>
    </div>
  );

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <motion.h1 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-3xl font-bold bg-primary-gradient bg-clip-text text-transparent"
      >
        New Journal Entry
      </motion.h1>
      
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-dark-card border border-white/10 rounded-2xl p-6 shadow-xl"
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Title */}
          <div>
            <label className="block text-gray-400 mb-2 font-medium">Title</label>
            <input 
              type="text" 
              value={title} 
              onChange={e => setTitle(e.target.value)} 
              className="w-full bg-dark-input border border-white/10 rounded-xl p-4 text-white text-lg focus:border-primary-DEFAULT outline-none"
              placeholder="How was your day?"
              required
            />
          </div>

          {/* Quick Check-in Section */}
          <div className="bg-dark-bg/50 border border-white/5 rounded-xl p-4">
            <div className="flex items-center gap-2 mb-4">
              <Brain className="text-primary-DEFAULT" size={20} />
              <h3 className="text-white font-medium">Quick Check-in</h3>
            </div>
            
            <div className="grid md:grid-cols-3 gap-4">
              <SliderInput 
                label="Stress Level"
                icon={Flame}
                value={stressLevel}
                onChange={setStressLevel}
                min={1}
                max={10}
                lowLabel="Relaxed"
                highLabel="Overwhelmed"
                color="red-400"
              />
              <SliderInput 
                label="Energy Level"
                icon={Zap}
                value={energyLevel}
                onChange={setEnergyLevel}
                min={1}
                max={10}
                lowLabel="Exhausted"
                highLabel="Energetic"
                color="yellow-400"
              />
              <div className="mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <BedDouble className="text-blue-400" size={18} />
                  <label className="text-gray-300 text-sm font-medium">Sleep Last Night</label>
                </div>
                <div className="flex gap-1">
                  {[1, 2, 3, 4, 5].map(star => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setSleepQuality(star)}
                      className={`text-2xl transition-all ${star <= sleepQuality ? 'text-blue-400' : 'text-gray-600'}`}
                    >
                      {star <= sleepQuality ? '★' : '☆'}
                    </button>
                  ))}
                </div>
                <div className="flex justify-between text-xs text-gray-500 mt-1">
                  <span>Poor</span>
                  <span>Excellent</span>
                </div>
              </div>
            </div>
          </div>

          {/* Context Tags */}
          <div>
            <label className="block text-gray-400 mb-3 font-medium">What's this about? (optional)</label>
            <div className="flex flex-wrap gap-2">
              {CONTEXT_TAGS.map(tag => (
                <button
                  key={tag.id}
                  type="button"
                  onClick={() => toggleTag(tag.id)}
                  className={`px-3 py-2 rounded-lg transition-all flex items-center gap-2 text-sm ${
                    contextTags.includes(tag.id) 
                      ? 'bg-primary-DEFAULT/20 border-primary-DEFAULT border text-primary-DEFAULT' 
                      : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT/50'
                  }`}
                >
                  <span>{tag.icon}</span>
                  <span>{tag.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Trigger Description */}
          <div>
            <label className="block text-gray-400 mb-2 font-medium">What triggered this feeling? (optional)</label>
            <input 
              type="text" 
              value={triggerDescription} 
              onChange={e => setTriggerDescription(e.target.value)} 
              className="w-full bg-dark-input border border-white/10 rounded-xl p-3 text-white focus:border-primary-DEFAULT outline-none"
              placeholder="e.g., Had a difficult conversation, received good news..."
            />
          </div>

          {/* Main Content */}
          <div className="relative">
            <div className="flex justify-between items-center mb-2">
                <label className="block text-gray-400 font-medium">Your Thoughts</label>
                <button
                    type="button"
                    onClick={toggleListening}
                    className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
                        isListening 
                        ? 'bg-red-500/20 text-red-500 animate-pulse border border-red-500/50' 
                        : 'bg-dark-input text-gray-400 border border-white/5 hover:bg-white/5'
                    }`}
                >
                    {isListening ? <MicOff size={14} /> : <Mic size={14} />}
                    {isListening ? 'Recording...' : 'Voice Note'}
                </button>
            </div>
            <textarea 
              value={content} 
              onChange={e => setContent(e.target.value)} 
              className="w-full h-48 bg-dark-input border border-white/10 rounded-xl p-4 text-white text-lg resize-none focus:border-primary-DEFAULT outline-none"
              placeholder="Write freely... Express what's on your mind."
              required
            />
          </div>

          {/* Submit */}
          <div className="flex justify-end pt-4 border-t border-white/10">
            <button 
              type="submit" 
              disabled={loading}
              className="flex items-center gap-2 bg-primary-gradient text-white px-8 py-3 rounded-xl font-bold hover:opacity-90 transition-all shadow-lg shadow-purple-500/20 disabled:opacity-50"
            >
              <Save size={20} />
              {loading ? 'Saving...' : 'Save Entry'}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
};

export default JournalEntry;
