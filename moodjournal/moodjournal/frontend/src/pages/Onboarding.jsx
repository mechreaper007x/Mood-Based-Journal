import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeft, ArrowRight, Brain, Briefcase, Check, Compass, Heart, Loader2, Shield, Star, User } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const API_URL = 'http://localhost:9092';

// TIPI Questions for Big 5
const TIPI_QUESTIONS = [
  { id: 1, text: "Extraverted, enthusiastic", trait: "extraversion", reversed: false },
  { id: 2, text: "Critical, quarrelsome", trait: "agreeableness", reversed: true },
  { id: 3, text: "Dependable, self-disciplined", trait: "conscientiousness", reversed: false },
  { id: 4, text: "Anxious, easily upset", trait: "emotionalStability", reversed: true },
  { id: 5, text: "Open to new experiences, complex", trait: "openness", reversed: false },
  { id: 6, text: "Reserved, quiet", trait: "extraversion", reversed: true },
  { id: 7, text: "Sympathetic, warm", trait: "agreeableness", reversed: false },
  { id: 8, text: "Disorganized, careless", trait: "conscientiousness", reversed: true },
  { id: 9, text: "Calm, emotionally stable", trait: "emotionalStability", reversed: false },
  { id: 10, text: "Conventional, uncreative", trait: "openness", reversed: true },
];

// Jungian Archetypes
const ARCHETYPES = [
  { id: "hero", name: "The Hero", desc: "Driven to prove worth through courageous action", icon: "⚔️" },
  { id: "caregiver", name: "The Caregiver", desc: "Motivated by compassion, generosity, and desire to help", icon: "🤲" },
  { id: "explorer", name: "The Explorer", desc: "Seeks freedom and discovery, avoids feeling trapped", icon: "🧭" },
  { id: "rebel", name: "The Rebel", desc: "Challenges the status quo, breaks rules for change", icon: "🔥" },
  { id: "lover", name: "The Lover", desc: "Values intimacy, beauty, and passionate connections", icon: "💕" },
  { id: "creator", name: "The Creator", desc: "Driven to create enduring value and express vision", icon: "🎨" },
  { id: "jester", name: "The Jester", desc: "Lives in the moment, brings joy and lightness", icon: "🃏" },
  { id: "sage", name: "The Sage", desc: "Seeks truth, wisdom, and understanding", icon: "📚" },
  { id: "magician", name: "The Magician", desc: "Transforms reality, makes dreams happen", icon: "✨" },
  { id: "ruler", name: "The Ruler", desc: "Creates order, takes responsibility, leads", icon: "👑" },
  { id: "innocent", name: "The Innocent", desc: "Optimistic, seeks happiness and safety", icon: "🕊️" },
  { id: "everyman", name: "The Everyman", desc: "Belongs, connects, down-to-earth", icon: "🤝" },
];

const STRESSORS = [
  { id: "work", label: "Work / Career", icon: "💼" },
  { id: "finances", label: "Finances", icon: "💰" },
  { id: "health", label: "Health", icon: "🏥" },
  { id: "relationships", label: "Relationships", icon: "💔" },
  { id: "family", label: "Family", icon: "👨‍👩‍👧" },
  { id: "academic", label: "Academic", icon: "📖" },
  { id: "social", label: "Social Life", icon: "👥" },
  { id: "self_image", label: "Self-Image", icon: "🪞" },
  { id: "future", label: "Future Uncertainty", icon: "❓" },
  { id: "loneliness", label: "Loneliness", icon: "🌑" },
];

const INTERESTS = [
  "Music", "Art", "Sports", "Gaming", "Reading", "Writing", "Coding", 
  "Philosophy", "Psychology", "Science", "Travel", "Cooking", "Fitness",
  "Movies", "Anime", "Nature", "Spirituality", "Politics", "History"
];

const Onboarding = () => {
  const navigate = useNavigate();
  const { checkProfileComplete } = useAuth();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Form State
  const [profile, setProfile] = useState({
    // Demographics
    gender: '',
    employmentStatus: '',
    relationshipStatus: '',
    livingArrangement: '',
    // TIPI responses (raw)
    tipiResponses: {},
    // Archetypes
    primaryArchetype: '',
    secondaryArchetype: '',
    // Empathy
    cognitiveEmpathy: 5,
    affectiveEmpathy: 5,
    compassionateEmpathy: 5,
    // Life Context
    currentStressors: [],
    baselineStressLevel: 5,
    baselineEnergyLevel: 5,
    sleepQuality: 5,
    // Beliefs
    coreBeliefs: '',
    lifeValues: '',
    interests: [],
    // Trauma
    hasReportedTrauma: false,
    traumaContext: '',
  });

  const totalSteps = 5;

  // Calculate Big 5 from TIPI responses
  const calculateBig5 = () => {
    const traits = {
      extraversion: [],
      agreeableness: [],
      conscientiousness: [],
      emotionalStability: [],
      openness: []
    };

    TIPI_QUESTIONS.forEach(q => {
      const response = profile.tipiResponses[q.id] || 4;
      const score = q.reversed ? (8 - response) : response;
      traits[q.trait].push(score);
    });

    return {
      extraversion: Math.round((traits.extraversion[0] + traits.extraversion[1]) / 2),
      agreeableness: Math.round((traits.agreeableness[0] + traits.agreeableness[1]) / 2),
      conscientiousness: Math.round((traits.conscientiousness[0] + traits.conscientiousness[1]) / 2),
      emotionalStability: Math.round((traits.emotionalStability[0] + traits.emotionalStability[1]) / 2),
      openness: Math.round((traits.openness[0] + traits.openness[1]) / 2),
    };
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError('');

    try {
      const token = localStorage.getItem('token');
      const big5 = calculateBig5();

      const payload = {
        ...profile,
        ...big5,
        currentStressors: profile.currentStressors,
        interests: profile.interests,
        isComplete: true,
      };
      delete payload.tipiResponses;

      const response = await fetch(`${API_URL}/api/profile`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) throw new Error('Failed to save profile');

      // Mark as complete
      await fetch(`${API_URL}/api/profile/complete`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
      });

      // Update AuthContext state so ProtectedRoute knows profile is complete
      await checkProfileComplete(token);

      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const nextStep = () => setStep(s => Math.min(s + 1, totalSteps));
  const prevStep = () => setStep(s => Math.max(s - 1, 1));

  const updateProfile = (key, value) => {
    setProfile(prev => ({ ...prev, [key]: value }));
  };

  const toggleArrayItem = (key, item) => {
    setProfile(prev => {
      const arr = prev[key];
      return {
        ...prev,
        [key]: arr.includes(item) ? arr.filter(i => i !== item) : [...arr, item]
      };
    });
  };

  // Step Components
  const StepIndicator = () => (
    <div className="flex justify-center gap-2 mb-8">
      {[1, 2, 3, 4, 5].map(s => (
        <div
          key={s}
          className={`w-3 h-3 rounded-full transition-all ${s === step ? 'bg-primary-DEFAULT scale-125' : s < step ? 'bg-green-500' : 'bg-gray-600'}`}
        />
      ))}
    </div>
  );

  const SliderInput = ({ label, value, onChange, min = 1, max = 10, labels }) => (
    <div className="mb-6">
      <label className="block text-gray-300 mb-2">{label}</label>
      <input
        type="range"
        min={min}
        max={max}
        value={value}
        onChange={e => onChange(parseInt(e.target.value))}
        className="w-full accent-primary-DEFAULT"
      />
      <div className="flex justify-between text-xs text-gray-500 mt-1">
        <span>{labels?.[0] || 'Low'}</span>
        <span className="text-primary-DEFAULT font-bold">{value}</span>
        <span>{labels?.[1] || 'High'}</span>
      </div>
    </div>
  );

  const renderStep = () => {
    switch (step) {
      case 1:
        return (
          <motion.div
            key="step1"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
          >
            <div className="flex items-center gap-3 mb-6">
              <User className="text-primary-DEFAULT" size={28} />
              <h2 className="text-2xl font-bold text-white">About You</h2>
            </div>
            <p className="text-gray-400 mb-6">Help us understand your current life situation.</p>

            <div className="space-y-4">
              <div>
                <label className="block text-gray-300 mb-2">Gender</label>
                <div className="flex flex-wrap gap-2">
                  {['Male', 'Female', 'Non-binary', 'Prefer not to say'].map(g => (
                    <button
                      key={g}
                      onClick={() => updateProfile('gender', g.toLowerCase())}
                      className={`px-4 py-2 rounded-lg transition-all ${profile.gender === g.toLowerCase() ? 'bg-primary-DEFAULT text-white' : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT'}`}
                    >
                      {g}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-gray-300 mb-2">Employment Status</label>
                <div className="flex flex-wrap gap-2">
                  {[
                    { val: 'student', label: 'Student' },
                    { val: 'employed', label: 'Employed' },
                    { val: 'self_employed', label: 'Self-Employed' },
                    { val: 'unemployed', label: 'Unemployed' },
                    { val: 'retired', label: 'Retired' },
                  ].map(e => (
                    <button
                      key={e.val}
                      onClick={() => updateProfile('employmentStatus', e.val)}
                      className={`px-4 py-2 rounded-lg transition-all ${profile.employmentStatus === e.val ? 'bg-primary-DEFAULT text-white' : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT'}`}
                    >
                      {e.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-gray-300 mb-2">Relationship Status</label>
                <div className="flex flex-wrap gap-2">
                  {['Single', 'Dating', 'Married', 'Divorced', 'Widowed'].map(r => (
                    <button
                      key={r}
                      onClick={() => updateProfile('relationshipStatus', r.toLowerCase())}
                      className={`px-4 py-2 rounded-lg transition-all ${profile.relationshipStatus === r.toLowerCase() ? 'bg-primary-DEFAULT text-white' : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT'}`}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-gray-300 mb-2">Living Arrangement</label>
                <div className="flex flex-wrap gap-2">
                  {[
                    { val: 'alone', label: 'Alone' },
                    { val: 'with_family', label: 'With Family' },
                    { val: 'with_partner', label: 'With Partner' },
                    { val: 'with_roommates', label: 'With Roommates' },
                  ].map(l => (
                    <button
                      key={l.val}
                      onClick={() => updateProfile('livingArrangement', l.val)}
                      className={`px-4 py-2 rounded-lg transition-all ${profile.livingArrangement === l.val ? 'bg-primary-DEFAULT text-white' : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT'}`}
                    >
                      {l.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        );

      case 2:
        return (
          <motion.div
            key="step2"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
          >
            <div className="flex items-center gap-3 mb-6">
              <Brain className="text-primary-DEFAULT" size={28} />
              <h2 className="text-2xl font-bold text-white">Personality</h2>
            </div>
            <p className="text-gray-400 mb-6">Rate how these traits describe you (1 = Disagree strongly, 7 = Agree strongly)</p>

            <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2">
              {TIPI_QUESTIONS.map(q => (
                <div key={q.id} className="bg-dark-input border border-white/10 rounded-lg p-4">
                  <p className="text-white mb-3">I see myself as: <span className="text-primary-DEFAULT">{q.text}</span></p>
                  <div className="flex gap-2 flex-wrap">
                    {[1, 2, 3, 4, 5, 6, 7].map(n => (
                      <button
                        key={n}
                        onClick={() => updateProfile('tipiResponses', { ...profile.tipiResponses, [q.id]: n })}
                        className={`w-10 h-10 rounded-lg font-bold transition-all ${profile.tipiResponses[q.id] === n ? 'bg-primary-DEFAULT text-white' : 'bg-dark-bg border border-white/10 text-gray-400 hover:border-primary-DEFAULT'}`}
                      >
                        {n}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        );

      case 3:
        return (
          <motion.div
            key="step3"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
          >
            <div className="flex items-center gap-3 mb-6">
              <Compass className="text-primary-DEFAULT" size={28} />
              <h2 className="text-2xl font-bold text-white">Your Archetype</h2>
            </div>
            <p className="text-gray-400 mb-4">Select 1-2 archetypes that resonate most with who you are.</p>

            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 max-h-[350px] overflow-y-auto pr-2">
              {ARCHETYPES.map(a => {
                const isSelected = profile.primaryArchetype === a.id || profile.secondaryArchetype === a.id;
                return (
                  <button
                    key={a.id}
                    onClick={() => {
                      if (profile.primaryArchetype === a.id) {
                        updateProfile('primaryArchetype', '');
                      } else if (profile.secondaryArchetype === a.id) {
                        updateProfile('secondaryArchetype', '');
                      } else if (!profile.primaryArchetype) {
                        updateProfile('primaryArchetype', a.id);
                      } else if (!profile.secondaryArchetype) {
                        updateProfile('secondaryArchetype', a.id);
                      }
                    }}
                    className={`p-3 rounded-lg text-left transition-all border ${isSelected ? 'bg-primary-DEFAULT/20 border-primary-DEFAULT' : 'bg-dark-input border-white/10 hover:border-primary-DEFAULT/50'}`}
                  >
                    <div className="text-2xl mb-1">{a.icon}</div>
                    <div className="text-white font-medium text-sm">{a.name}</div>
                    <div className="text-gray-500 text-xs">{a.desc}</div>
                  </button>
                );
              })}
            </div>

            <div className="mt-6">
              <div className="flex items-center gap-3 mb-4">
                <Heart className="text-primary-DEFAULT" size={24} />
                <h3 className="text-xl font-bold text-white">Empathy Levels</h3>
              </div>
              
              <SliderInput
                label="Cognitive Empathy (Understanding others' perspectives)"
                value={profile.cognitiveEmpathy}
                onChange={v => updateProfile('cognitiveEmpathy', v)}
                labels={['Hard to understand', 'Highly intuitive']}
              />
              <SliderInput
                label="Affective Empathy (Feeling others' emotions)"
                value={profile.affectiveEmpathy}
                onChange={v => updateProfile('affectiveEmpathy', v)}
                labels={['Rarely affected', 'Deeply affected']}
              />
              <SliderInput
                label="Compassionate Empathy (Taking action to help)"
                value={profile.compassionateEmpathy}
                onChange={v => updateProfile('compassionateEmpathy', v)}
                labels={['Observe only', 'Always help']}
              />
            </div>
          </motion.div>
        );

      case 4:
        return (
          <motion.div
            key="step4"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
          >
            <div className="flex items-center gap-3 mb-6">
              <Briefcase className="text-primary-DEFAULT" size={28} />
              <h2 className="text-2xl font-bold text-white">Life Context</h2>
            </div>
            
            <div className="mb-6">
              <label className="block text-gray-300 mb-3">Current Stressors (select all that apply)</label>
              <div className="flex flex-wrap gap-2">
                {STRESSORS.map(s => (
                  <button
                    key={s.id}
                    onClick={() => toggleArrayItem('currentStressors', s.id)}
                    className={`px-3 py-2 rounded-lg transition-all flex items-center gap-2 ${profile.currentStressors.includes(s.id) ? 'bg-red-500/20 border-red-500 border text-red-300' : 'bg-dark-input border border-white/10 text-gray-400 hover:border-red-500/50'}`}
                  >
                    <span>{s.icon}</span>
                    <span>{s.label}</span>
                  </button>
                ))}
              </div>
            </div>

            <SliderInput
              label="Current Stress Level"
              value={profile.baselineStressLevel}
              onChange={v => updateProfile('baselineStressLevel', v)}
              labels={['Relaxed', 'Overwhelmed']}
            />
            <SliderInput
              label="Baseline Energy Level"
              value={profile.baselineEnergyLevel}
              onChange={v => updateProfile('baselineEnergyLevel', v)}
              labels={['Exhausted', 'Energetic']}
            />
            <SliderInput
              label="Sleep Quality"
              value={profile.sleepQuality}
              onChange={v => updateProfile('sleepQuality', v)}
              labels={['Poor', 'Excellent']}
            />

            <div className="mt-4">
              <label className="block text-gray-300 mb-3">Interests (select what resonates)</label>
              <div className="flex flex-wrap gap-2">
                {INTERESTS.map(i => (
                  <button
                    key={i}
                    onClick={() => toggleArrayItem('interests', i.toLowerCase())}
                    className={`px-3 py-1.5 rounded-full text-sm transition-all ${profile.interests.includes(i.toLowerCase()) ? 'bg-primary-DEFAULT text-white' : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT'}`}
                  >
                    {i}
                  </button>
                ))}
              </div>
            </div>
          </motion.div>
        );

      case 5:
        return (
          <motion.div
            key="step5"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
          >
            <div className="flex items-center gap-3 mb-6">
              <Shield className="text-primary-DEFAULT" size={28} />
              <h2 className="text-2xl font-bold text-white">Beliefs & Context</h2>
            </div>
            <p className="text-gray-400 mb-6">This section is <span className="text-green-400">completely optional</span>. Share only what you're comfortable with.</p>

            <div className="space-y-4">
              <div>
                <label className="block text-gray-300 mb-2">Core Beliefs (optional)</label>
                <textarea
                  value={profile.coreBeliefs}
                  onChange={e => updateProfile('coreBeliefs', e.target.value)}
                  placeholder="What do you fundamentally believe about life, purpose, or the world?"
                  className="w-full bg-dark-input border border-white/10 rounded-lg p-3 text-white focus:border-primary-DEFAULT outline-none resize-none h-24"
                />
              </div>

              <div>
                <label className="block text-gray-300 mb-2">Life Values (optional)</label>
                <textarea
                  value={profile.lifeValues}
                  onChange={e => updateProfile('lifeValues', e.target.value)}
                  placeholder="What matters most to you? (e.g., freedom, family, creativity, truth)"
                  className="w-full bg-dark-input border border-white/10 rounded-lg p-3 text-white focus:border-primary-DEFAULT outline-none resize-none h-24"
                />
              </div>

              <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-yellow-500">⚠️</span>
                  <span className="text-yellow-400 font-medium">Sensitive (Optional)</span>
                </div>
                
                <label className="flex items-center gap-3 cursor-pointer mb-3">
                  <input
                    type="checkbox"
                    checked={profile.hasReportedTrauma}
                    onChange={e => updateProfile('hasReportedTrauma', e.target.checked)}
                    className="w-5 h-5 accent-primary-DEFAULT"
                  />
                  <span className="text-gray-300">I have experienced significant past events that affect me today</span>
                </label>

                {profile.hasReportedTrauma && (
                  <textarea
                    value={profile.traumaContext}
                    onChange={e => updateProfile('traumaContext', e.target.value)}
                    placeholder="If you're comfortable, briefly describe the nature of these experiences (this helps personalize insights)"
                    className="w-full bg-dark-input border border-white/10 rounded-lg p-3 text-white focus:border-yellow-500 outline-none resize-none h-24"
                  />
                )}
              </div>
            </div>

            <div className="mt-8 bg-green-500/10 border border-green-500/30 rounded-lg p-4">
              <div className="flex items-center gap-2">
                <Check className="text-green-500" />
                <span className="text-green-400 font-medium">Ready to complete your profile!</span>
              </div>
              <p className="text-gray-400 text-sm mt-2">Your answers help us provide personalized insights. You can update these anytime.</p>
            </div>
          </motion.div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-dark-bg p-4 md:p-8 relative overflow-hidden">
      {/* Background glow */}
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-primary-DEFAULT/10 rounded-full blur-[150px]" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[40%] h-[40%] bg-purple-500/10 rounded-full blur-[150px]" />

      <div className="max-w-2xl mx-auto relative z-10">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Star className="text-primary-DEFAULT" size={32} />
            <h1 className="text-3xl font-bold text-white">Welcome to Your Journey</h1>
          </div>
          <p className="text-gray-400">Let's build your psychological profile for personalized insights.</p>
        </div>

        <StepIndicator />

        {/* Main Card */}
        <div className="bg-dark-card backdrop-blur-xl border border-white/10 rounded-2xl p-6 md:p-8 shadow-2xl">
          {error && <div className="bg-red-500/20 border border-red-500 text-red-400 rounded-lg p-3 mb-4">{error}</div>}
          
          <AnimatePresence mode="wait">
            {renderStep()}
          </AnimatePresence>

          {/* Navigation */}
          <div className="flex justify-between mt-8 pt-6 border-t border-white/10">
            <button
              onClick={prevStep}
              disabled={step === 1}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-all ${step === 1 ? 'text-gray-600 cursor-not-allowed' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}
            >
              <ArrowLeft size={18} />
              Back
            </button>

            {step < totalSteps ? (
              <button
                onClick={nextStep}
                className="flex items-center gap-2 px-6 py-2 bg-primary-gradient rounded-lg text-white font-medium hover:opacity-90 transition-all"
              >
                Next
                <ArrowRight size={18} />
              </button>
            ) : (
              <button
                onClick={handleSubmit}
                disabled={loading}
                className="flex items-center gap-2 px-6 py-2 bg-green-600 hover:bg-green-500 rounded-lg text-white font-medium transition-all"
              >
                {loading ? <Loader2 className="animate-spin" size={18} /> : <Check size={18} />}
                Complete
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Onboarding;
