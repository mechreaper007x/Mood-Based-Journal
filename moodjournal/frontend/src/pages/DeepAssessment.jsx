import { motion } from 'framer-motion';
import { Brain, CheckCircle, Loader2, RefreshCw, Sparkles } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/axios';

const DeepAssessment = () => {
  const navigate = useNavigate();
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  // Fetch questions on mount
  useEffect(() => {
    fetchQuestions();
  }, []);

  const fetchQuestions = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/assessment/questions');
      setQuestions(res.data);
      // Initialize answer state
      const initialAnswers = {};
      res.data.forEach(q => { initialAnswers[q.id] = ''; });
      setAnswers(initialAnswers);
    } catch (err) {
      setError('Failed to load questions. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAnswerChange = (questionId, value) => {
    setAnswers(prev => ({ ...prev, [questionId]: value }));
  };

  const handleSubmit = async () => {
    // Validate all answered
    const unanswered = questions.filter(q => !answers[q.id]?.trim());
    if (unanswered.length > 0) {
      setError(`Please answer all questions. ${unanswered.length} remaining.`);
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const submission = {
        responses: questions.map(q => ({
          questionId: q.id,
          question: q.question,
          answer: answers[q.id]
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

  // Results view
  if (result) {
    return (
      <div className="max-w-3xl mx-auto space-y-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center"
        >
          <CheckCircle className="mx-auto text-green-500 mb-4" size={64} />
          <h1 className="text-3xl font-bold text-white mb-2">Analysis Complete</h1>
          <p className="text-gray-400">Your psychological profile has been analyzed</p>
        </motion.div>

        <div className="bg-dark-card border border-white/10 rounded-2xl p-6">
          {/* Big 5 */}
          <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
            <Brain className="text-primary-DEFAULT" size={24} />
            Big Five Personality
          </h2>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
            {[
              { label: 'Extraversion', value: result.extraversion },
              { label: 'Agreeableness', value: result.agreeableness },
              { label: 'Conscientiousness', value: result.conscientiousness },
              { label: 'Emotional Stability', value: result.emotionalStability },
              { label: 'Openness', value: result.openness },
            ].map(trait => (
              <div key={trait.label} className="bg-dark-bg rounded-lg p-3 text-center">
                <div className="text-3xl font-bold text-primary-DEFAULT">{trait.value}/7</div>
                <div className="text-xs text-gray-400 mt-1">{trait.label}</div>
              </div>
            ))}
          </div>

          {/* Archetypes */}
          <h2 className="text-xl font-bold text-white mb-3">Archetypes</h2>
          <div className="flex gap-4 mb-6">
            <div className="bg-primary-DEFAULT/20 border border-primary-DEFAULT rounded-lg px-4 py-2">
              <span className="text-primary-DEFAULT font-medium">Primary: </span>
              <span className="text-white capitalize">{result.primaryArchetype}</span>
            </div>
            <div className="bg-purple-500/20 border border-purple-500 rounded-lg px-4 py-2">
              <span className="text-purple-400 font-medium">Secondary: </span>
              <span className="text-white capitalize">{result.secondaryArchetype}</span>
            </div>
          </div>

          {/* Empathy */}
          <h2 className="text-xl font-bold text-white mb-3">Empathy Profile</h2>
          <div className="grid grid-cols-3 gap-4 mb-6">
            {[
              { label: 'Cognitive', value: result.cognitiveEmpathy },
              { label: 'Affective', value: result.affectiveEmpathy },
              { label: 'Compassionate', value: result.compassionateEmpathy },
            ].map(emp => (
              <div key={emp.label} className="bg-dark-bg rounded-lg p-3">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-gray-400 text-sm">{emp.label}</span>
                  <span className="text-white font-bold">{emp.value}/10</span>
                </div>
                <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-gradient-to-r from-primary-DEFAULT to-purple-500"
                    style={{ width: `${emp.value * 10}%` }}
                  />
                </div>
              </div>
            ))}
          </div>

          {/* Stressors */}
          {result.detectedStressors?.length > 0 && (
            <>
              <h2 className="text-xl font-bold text-white mb-3">Detected Stressors</h2>
              <div className="flex flex-wrap gap-2 mb-6">
                {result.detectedStressors.map(s => (
                  <span key={s} className="bg-red-500/20 border border-red-500/50 text-red-300 px-3 py-1 rounded-full text-sm capitalize">
                    {s.replace('_', ' ')}
                  </span>
                ))}
              </div>
            </>
          )}

          {/* Insights */}
          <h2 className="text-xl font-bold text-white mb-3 flex items-center gap-2">
            <Sparkles className="text-yellow-400" size={20} />
            Psychological Insights
          </h2>
          <p className="text-gray-300 bg-dark-bg rounded-lg p-4 italic">
            "{result.insights}"
          </p>
        </div>

        <button
          onClick={() => navigate('/dashboard')}
          className="w-full bg-primary-gradient text-white py-3 rounded-xl font-bold hover:opacity-90 transition-all"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  // Loading state
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96">
        <Loader2 className="animate-spin text-primary-DEFAULT mb-4" size={48} />
        <p className="text-gray-400">Generating personalized questions...</p>
      </div>
    );
  }

  // Questions form
  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="text-center">
        <h1 className="text-3xl font-bold bg-primary-gradient bg-clip-text text-transparent mb-2">
          Deep Psychological Assessment
        </h1>
        <p className="text-gray-400">
          Answer honestly - there are no right or wrong answers. Your responses help us understand you better.
        </p>
      </div>

      {error && (
        <div className="bg-red-500/20 border border-red-500 text-red-400 rounded-lg p-3">
          {error}
        </div>
      )}

      <div className="bg-dark-card border border-white/10 rounded-2xl p-6 space-y-6">
        {questions.map((q, idx) => (
          <motion.div
            key={q.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.05 }}
            className="space-y-2"
          >
            <label className="block text-white font-medium">
              <span className="text-primary-DEFAULT mr-2">Q{q.id}.</span>
              {q.question}
            </label>
            <textarea
              value={answers[q.id] || ''}
              onChange={e => handleAnswerChange(q.id, e.target.value)}
              placeholder="Type your answer here..."
              className="w-full bg-dark-input border border-white/10 rounded-xl p-3 text-white resize-none h-24 focus:border-primary-DEFAULT outline-none"
            />
          </motion.div>
        ))}

        <div className="flex gap-4 pt-4 border-t border-white/10">
          <button
            onClick={fetchQuestions}
            disabled={submitting}
            className="flex items-center gap-2 px-4 py-2 text-gray-400 hover:text-white transition-all"
          >
            <RefreshCw size={18} />
            New Questions
          </button>
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="flex-1 flex items-center justify-center gap-2 bg-primary-gradient text-white py-3 rounded-xl font-bold hover:opacity-90 transition-all disabled:opacity-50"
          >
            {submitting ? (
              <>
                <Loader2 className="animate-spin" size={20} />
                Analyzing...
              </>
            ) : (
              <>
                <Brain size={20} />
                Analyze My Responses
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default DeepAssessment;
