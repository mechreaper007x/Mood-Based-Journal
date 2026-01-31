import { motion } from 'framer-motion';
import { ArrowLeft, Brain, CheckCircle, Heart, Loader2, Sparkles, Target, Zap } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AssessmentCard from '../components/AssessmentCard';
import {
  BFPT_OPTIONS,
  BFPT_QUESTIONS_SORTED,
  ENNEAGRAM_QUESTIONS,
  EQ60_OPTIONS,
  getEQBatch,
  PHQ9_OPTIONS,
  PHQ9_QUESTIONS
} from '../data/assessmentData';
import api from '../lib/axios';

// Test cards metadata
const TEST_CARDS = [
  {
    id: 'phq9',
    name: 'Mental Health',
    subtitle: 'PHQ-9 Depression Screening',
    icon: Heart,
    color: 'from-red-500/20 to-pink-500/20',
    borderColor: 'border-red-500/50',
    iconColor: 'text-red-400',
    questions: 9
  },
  {
    id: 'bfpt',
    name: 'Big 5 Personality',
    subtitle: 'BFPT 50-Item Assessment',
    icon: Brain,
    color: 'from-blue-500/20 to-cyan-500/20',
    borderColor: 'border-blue-500/50',
    iconColor: 'text-blue-400',
    questions: 50
  },
  {
    id: 'enneagram',
    name: 'Enneagram',
    subtitle: 'Core Motivations',
    icon: Target,
    color: 'from-purple-500/20 to-violet-500/20',
    borderColor: 'border-purple-500/50',
    iconColor: 'text-purple-400',
    questions: 36
  },
  {
    id: 'eq',
    name: 'Empathy Style',
    subtitle: 'EQ Assessment',
    icon: Zap,
    color: 'from-yellow-500/20 to-orange-500/20',
    borderColor: 'border-yellow-500/50',
    iconColor: 'text-yellow-400',
    questions: 20
  }
];

const DeepAssessment = () => {
  const navigate = useNavigate();
  const [activeTest, setActiveTest] = useState(null); // null = show grid, otherwise = test id
  const [answers, setAnswers] = useState({
    phq9: {},
    bfpt: {},
    enneagram: {},
    eq: {},
    personalized: {}
  });
  const [personalizedQuestions, setPersonalizedQuestions] = useState([]);
  const [eqBatch, setEqBatch] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchPersonalizedQuestions();
    fetchEQProgress();
  }, []);

  const fetchPersonalizedQuestions = async () => {
    try {
      const res = await api.get('/assessment/personalized-questions');
      setPersonalizedQuestions(res.data || []);
    } catch (err) {
      console.error('Failed to fetch personalized questions:', err);
      // Themed fallback questions when Mistral unavailable
      setPersonalizedQuestions([
        { id: 1, question: "How do you typically respond when under social pressure?", focus: "personality" },
        { id: 2, question: "What fears or anxieties seem to drive your behavior most often?", focus: "shadow" },
        { id: 3, question: "What brings you the most fulfillment in your daily life?", focus: "personal" }
      ]);
    }
  };

  const fetchEQProgress = async () => {
    try {
      const res = await api.get('/assessment/eq-progress');
      setEqBatch(res.data?.nextBatch || 1);
    } catch (err) {
      console.error('Failed to fetch EQ progress:', err);
      setEqBatch(1);
    }
  };

  const handleAnswer = (testId, questionId, value) => {
    setAnswers(prev => ({
      ...prev,
      [testId]: { ...prev[testId], [questionId]: value }
    }));
  };

  const getTestQuestions = (testId) => {
    switch (testId) {
      case 'phq9':
        return { questions: PHQ9_QUESTIONS, options: PHQ9_OPTIONS, type: 'likert' };
      case 'bfpt':
        return { questions: BFPT_QUESTIONS_SORTED, options: BFPT_OPTIONS, type: 'likert' };
      case 'enneagram':
        return { questions: ENNEAGRAM_QUESTIONS, options: null, type: 'paired' };
      case 'eq':
        return { questions: getEQBatch(eqBatch), options: EQ60_OPTIONS, type: 'likert' };
      default:
        return { questions: [], options: [], type: 'mcq' };
    }
  };

  const getTestProgress = (testId) => {
    const answered = Object.keys(answers[testId] || {}).length;
    const card = TEST_CARDS.find(c => c.id === testId);
    const total = card?.questions || 1;
    return { answered, total, percent: Math.round((answered / total) * 100) };
  };

  const isTestComplete = (testId) => {
    const { answered, total } = getTestProgress(testId);
    return answered === total;
  };

  const handleSubmitAll = async () => {
    setSubmitting(true);
    setError('');

    try {
      const submission = {
        phq9Responses: answers.phq9,
        bfptResponses: answers.bfpt,
        enneagramResponses: answers.enneagram,
        eqResponses: answers.eq,
        eqBatch: eqBatch,
        personalizedResponses: Object.entries(answers.personalized).map(([id, answer]) => ({
          questionId: parseInt(id),
          question: personalizedQuestions.find(q => q.id === parseInt(id))?.question,
          answer
        }))
      };

      const res = await api.post('/assessment/analyze', submission);
      setResult(res.data);
    } catch (err) {
      setError('Analysis failed. Please try again.');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  // ===== RESULTS VIEW =====
  if (result) {
    return (
      <div className="max-w-3xl mx-auto space-y-6">
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="text-center">
          <CheckCircle className="mx-auto text-green-500 mb-4" size={64} />
          <h1 className="text-3xl font-bold text-white mb-2">Analysis Complete</h1>
          <p className="text-gray-400">Your psychological profile has been updated</p>
        </motion.div>

        <div className="bg-dark-card border border-white/10 rounded-2xl p-6 space-y-6">
          {result.phq9Score !== undefined && (
            <div>
              <h2 className="text-xl font-bold text-white mb-3 flex items-center gap-2">
                <Heart className="text-red-400" size={20} />
                Mental Health: {result.phq9Severity}
              </h2>
              <div className="bg-dark-bg rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-gray-400">PHQ-9 Score</span>
                  <span className="text-2xl font-bold text-white">{result.phq9Score}/27</span>
                </div>
                <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
                  <div
                    className={`h-full ${result.phq9Score < 5 ? 'bg-green-500' : result.phq9Score < 10 ? 'bg-yellow-500' : result.phq9Score < 15 ? 'bg-orange-500' : 'bg-red-500'}`}
                    style={{ width: `${(result.phq9Score / 27) * 100}%` }}
                  />
                </div>
              </div>
            </div>
          )}

          <div>
            <h2 className="text-xl font-bold text-white mb-3 flex items-center gap-2">
              <Brain className="text-primary-DEFAULT" size={20} />
              Big Five Personality
            </h2>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
              {[
                { label: 'Extraversion', value: result.extraversion },
                { label: 'Agreeableness', value: result.agreeableness },
                { label: 'Conscientiousness', value: result.conscientiousness },
                { label: 'Stability', value: result.emotionalStability },
                { label: 'Openness', value: result.openness },
              ].map(trait => (
                <div key={trait.label} className="bg-dark-bg rounded-lg p-3 text-center">
                  <div className="text-2xl font-bold text-primary-DEFAULT">{trait.value}/7</div>
                  <div className="text-xs text-gray-400 mt-1">{trait.label}</div>
                </div>
              ))}
            </div>
          </div>

          {result.enneagramType && (
            <div>
              <h2 className="text-xl font-bold text-white mb-3 flex items-center gap-2">
                <Target className="text-purple-400" size={20} />
                Enneagram Type
              </h2>
              <div className="bg-purple-500/20 border border-purple-500 rounded-lg px-4 py-3">
                <span className="text-3xl font-bold text-purple-300">Type {result.enneagramType}</span>
                {result.enneagramWing && <span className="text-purple-400 ml-2">w{result.enneagramWing}</span>}
              </div>
            </div>
          )}

          {result.eqScore !== undefined && (
            <div>
              <h2 className="text-xl font-bold text-white mb-3 flex items-center gap-2">
                <Zap className="text-yellow-400" size={20} />
                Empathy Quotient ({result.eqCompletionPercent || 33}% Complete)
              </h2>
              <div className="bg-dark-bg rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-gray-400">Current EQ Score</span>
                  <span className="text-2xl font-bold text-white">{result.eqScore}</span>
                </div>
                <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
                  <div className="h-full bg-gradient-to-r from-yellow-500 to-primary-DEFAULT" style={{ width: `${result.eqCompletionPercent || 33}%` }} />
                </div>
              </div>
            </div>
          )}

          {result.insights && (
            <div>
              <h2 className="text-xl font-bold text-white mb-3 flex items-center gap-2">
                <Sparkles className="text-yellow-400" size={20} />
                Psychological Insights
              </h2>
              <p className="text-gray-300 bg-dark-bg rounded-lg p-4 italic">"{result.insights}"</p>
            </div>
          )}
        </div>

        <button onClick={() => navigate('/dashboard')} className="w-full bg-primary-gradient text-white py-3 rounded-xl font-bold hover:opacity-90 transition-all">
          Return to Dashboard
        </button>
      </div>
    );
  }

  // ===== ACTIVE TEST VIEW =====
  if (activeTest) {
    const testCard = TEST_CARDS.find(c => c.id === activeTest);
    const { questions, options, type } = getTestQuestions(activeTest);
    const progress = getTestProgress(activeTest);

    return (
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Header with back button */}
        <div className="flex items-center gap-4">
          <button onClick={() => setActiveTest(null)} className="p-2 hover:bg-white/10 rounded-lg transition-all">
            <ArrowLeft className="text-gray-400" size={24} />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-white flex items-center gap-2">
              {testCard && <testCard.icon className={testCard.iconColor} size={28} />}
              {testCard?.name}
            </h1>
            <p className="text-gray-400 text-sm">{testCard?.subtitle}</p>
          </div>
        </div>

        {/* Progress bar */}
        <div className="bg-dark-card border border-white/10 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-400">{progress.answered} of {progress.total} answered</span>
            <span className="text-xl font-bold text-primary-DEFAULT">{progress.percent}%</span>
          </div>
          <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
            <div className="h-full bg-primary-DEFAULT transition-all" style={{ width: `${progress.percent}%` }} />
          </div>
        </div>

        {/* Questions */}
        <div className="space-y-4 max-h-[55vh] overflow-y-auto pr-2">
          {type === 'paired' ? (
            questions.map((q, idx) => (
              <motion.div
                key={q.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: idx * 0.02 }}
                className="bg-dark-card/50 backdrop-blur-sm border border-white/10 rounded-xl p-5"
              >
                <div className="flex items-start gap-3 mb-4">
                  <span className="flex-shrink-0 w-8 h-8 bg-purple-500/20 text-purple-400 rounded-lg flex items-center justify-center text-sm font-bold">
                    {q.id}
                  </span>
                  <p className="text-gray-400 text-sm">Choose the statement that describes you best:</p>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <button
                    onClick={() => handleAnswer('enneagram', q.id, 'A')}
                    className={`p-4 rounded-xl text-left transition-all ${
                      answers.enneagram[q.id] === 'A'
                        ? 'bg-primary-DEFAULT/20 border-2 border-primary-DEFAULT text-white'
                        : 'bg-dark-input border-2 border-white/10 text-gray-300 hover:border-primary-DEFAULT/50'
                    }`}
                  >
                    {q.optionA.text}
                  </button>
                  <button
                    onClick={() => handleAnswer('enneagram', q.id, 'B')}
                    className={`p-4 rounded-xl text-left transition-all ${
                      answers.enneagram[q.id] === 'B'
                        ? 'bg-primary-DEFAULT/20 border-2 border-primary-DEFAULT text-white'
                        : 'bg-dark-input border-2 border-white/10 text-gray-300 hover:border-primary-DEFAULT/50'
                    }`}
                  >
                    {q.optionB.text}
                  </button>
                </div>
              </motion.div>
            ))
          ) : (
            questions.map((q) => (
              <AssessmentCard
                key={q.id}
                number={q.id}
                question={q.text}
                options={options}
                selectedAnswer={answers[activeTest][q.id]}
                onSelect={(value) => handleAnswer(activeTest, q.id, value)}
                type="likert"
              />
            ))
          )}
        </div>

        {/* Done button */}
        <button
          onClick={() => setActiveTest(null)}
          className={`w-full py-3 rounded-xl font-bold transition-all ${
            isTestComplete(activeTest)
              ? 'bg-green-600 hover:bg-green-500 text-white'
              : 'bg-primary-gradient text-white hover:opacity-90'
          }`}
        >
          {isTestComplete(activeTest) ? '✓ Done - Back to Tests' : 'Save & Return'}
        </button>
      </div>
    );
  }

  // ===== TEST SELECTOR GRID (Main View) =====
  const completedCount = TEST_CARDS.filter(c => isTestComplete(c.id)).length;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="text-center">
        <h1 className="text-3xl font-bold bg-primary-gradient bg-clip-text text-transparent mb-2">
          Deep Psychological Assessment
        </h1>
        <p className="text-gray-400">Choose a test to begin. Complete all for comprehensive analysis.</p>
      </div>

      {error && (
        <div className="bg-red-500/20 border border-red-500 text-red-400 rounded-lg p-3">{error}</div>
      )}

      {/* 2x2 Test Card Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {TEST_CARDS.map((card, idx) => {
          const progress = getTestProgress(card.id);
          const complete = isTestComplete(card.id);
          const Icon = card.icon;

          return (
            <motion.button
              key={card.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              onClick={() => setActiveTest(card.id)}
              className={`relative bg-gradient-to-br ${card.color} border ${complete ? 'border-green-500' : card.borderColor} rounded-2xl p-6 text-left hover:scale-[1.02] transition-all group`}
            >
              {/* Complete badge */}
              {complete && (
                <div className="absolute top-3 right-3">
                  <CheckCircle className="text-green-500" size={24} />
                </div>
              )}

              <Icon className={`${card.iconColor} mb-3`} size={40} />
              <h3 className="text-xl font-bold text-white mb-1">{card.name}</h3>
              <p className="text-sm text-gray-400 mb-4">{card.subtitle}</p>

              {/* Progress */}
              <div className="space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-gray-400">{progress.answered}/{progress.total} answered</span>
                  <span className={complete ? 'text-green-400' : 'text-gray-400'}>{progress.percent}%</span>
                </div>
                <div className="h-1.5 bg-black/30 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${complete ? 'bg-green-500' : 'bg-white/50'}`}
                    style={{ width: `${progress.percent}%` }}
                  />
                </div>
              </div>
            </motion.button>
          );
        })}
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="bg-gradient-to-br from-green-500/20 to-teal-500/20 border border-green-500/50 rounded-2xl p-6"
      >
        <div className="flex items-start gap-4">
          <Sparkles className="text-green-400 flex-shrink-0" size={40} />
          <div className="flex-1">
            <h3 className="text-xl font-bold text-white mb-1">Personal Reflection</h3>
            <p className="text-sm text-gray-400 mb-4">AI-generated questions based on your journal entries</p>
            
            {personalizedQuestions.length > 0 && (
              <div className="space-y-3">
                {personalizedQuestions.map((q, idx) => (
                  <div key={q.id} className="bg-black/20 rounded-lg p-3">
                    <p className="text-sm text-white mb-2">{idx + 1}. {q.question}</p>
                    <textarea
                      value={answers.personalized[q.id] || ''}
                      onChange={(e) => handleAnswer('personalized', q.id, e.target.value)}
                      placeholder="Share your thoughts..."
                      className="w-full bg-dark-input border border-white/10 rounded-lg p-2 text-white text-sm resize-none h-16 focus:border-green-500 outline-none"
                    />
                  </div>
                ))}
                
                {/* Save Reflections Button */}
                <button
                  onClick={handleSubmitAll}
                  disabled={submitting || Object.keys(answers.personalized).length === 0}
                  className={`w-full mt-4 py-3 rounded-lg font-semibold text-sm transition-all flex items-center justify-center gap-2 ${
                    Object.keys(answers.personalized).length > 0 && !submitting
                      ? 'bg-green-500/30 border border-green-500/50 text-green-400 hover:bg-green-500/40'
                      : 'bg-gray-700/50 border border-gray-600/50 text-gray-500 cursor-not-allowed'
                  }`}
                >
                  {submitting ? (
                    <>
                      <Loader2 className="animate-spin" size={16} />
                      Saving...
                    </>
                  ) : (
                    <>
                      <CheckCircle size={16} />
                      Save Reflections
                    </>
                  )}
                </button>
              </div>
            )}
          </div>
        </div>
      </motion.div>

      {/* Submit Button */}
      <button
        onClick={handleSubmitAll}
        disabled={submitting || completedCount < 2}
        className={`w-full py-4 rounded-xl font-bold text-lg transition-all flex items-center justify-center gap-2 ${
          completedCount >= 2 && !submitting
            ? 'bg-primary-gradient text-white hover:opacity-90'
            : 'bg-gray-700 text-gray-500 cursor-not-allowed'
        }`}
      >
        {submitting ? (
          <>
            <Loader2 className="animate-spin" size={20} />
            Analyzing Your Profile...
          </>
        ) : (
          <>
            <Brain size={20} />
            Analyze ({completedCount}/4 tests complete)
          </>
        )}
      </button>

      <p className="text-center text-xs text-gray-500">
        Complete at least 2 tests to unlock analysis. More tests = deeper insights.
      </p>
    </div>
  );
};

export default DeepAssessment;
