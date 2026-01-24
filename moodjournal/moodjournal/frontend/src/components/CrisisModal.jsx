import { AnimatePresence, motion } from 'framer-motion';
import { Heart, Phone } from 'lucide-react';
import { useState } from 'react';
import { CRISIS_RESOURCES, GROUNDING_EXERCISES } from '../data/crisisResources';

/**
 * CrisisModal
 * A modal that appears when high-risk content is detected.
 * It blocks interaction until acknowledged and provides immediate help resources.
 */
const CrisisModal = ({ isOpen, onClose, riskScore }) => {
  const [showGrounding, setShowGrounding] = useState(false);
  const [acknowledged, setAcknowledged] = useState(false);

  if (!isOpen) return null;

  const handleAcknowledge = () => {
    setAcknowledged(true);
    // Small delay before allowing close
    setTimeout(() => onClose(), 500);
  };

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
        onClick={(e) => e.stopPropagation()} // Prevent closing by clicking outside
      >
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.9, opacity: 0 }}
          className="bg-gradient-to-br from-[#1a1c23] to-[#0d0f14] border border-red-500/30 rounded-2xl p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto shadow-2xl"
        >
          {/* Header */}
          <div className="flex items-center gap-3 mb-6">
            <div className="p-3 bg-red-500/20 rounded-full">
              <Heart className="w-6 h-6 text-red-400" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">We're Here For You</h2>
              <p className="text-gray-400 text-sm">Your feelings are valid</p>
            </div>
          </div>

          {/* Main Message */}
          <div className="bg-white/5 rounded-xl p-4 mb-6">
            <p className="text-gray-300 leading-relaxed">
              It sounds like you're going through a difficult time. 
              <span className="text-white font-medium"> You don't have to face this alone.</span>
            </p>
            <p className="text-gray-400 text-sm mt-2">
              This is not a diagnosis. But if you're feeling overwhelmed, 
              please consider reaching out to someone who can help.
            </p>
          </div>

          {/* Helplines */}
          <div className="mb-6">
            <h3 className="text-white font-semibold mb-3 flex items-center gap-2">
              <Phone className="w-4 h-4" />
              Talk to Someone Now
            </h3>
            <div className="space-y-2">
              {CRISIS_RESOURCES.india.map((resource, idx) => (
                <div key={idx} className="bg-white/5 rounded-lg p-3 hover:bg-white/10 transition-colors">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-white font-medium">{resource.name}</p>
                      <p className="text-gray-400 text-sm">{resource.description}</p>
                    </div>
                    <a 
                      href={`tel:${resource.number.replace(/-/g, '')}`}
                      className="text-primary-DEFAULT font-bold hover:underline"
                    >
                      {resource.number}
                    </a>
                  </div>
                  <p className="text-gray-500 text-xs mt-1">{resource.hours}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Grounding Exercise Toggle */}
          <button
            onClick={() => setShowGrounding(!showGrounding)}
            className="w-full text-left bg-blue-500/10 hover:bg-blue-500/20 rounded-lg p-3 mb-4 transition-colors"
          >
            <span className="text-blue-400 font-medium">
              {showGrounding ? '▼' : '▶'} Try a Grounding Exercise
            </span>
          </button>

          {showGrounding && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              className="mb-6 space-y-4"
            >
              {GROUNDING_EXERCISES.map((exercise, idx) => (
                <div key={idx} className="bg-white/5 rounded-lg p-4">
                  <h4 className="text-white font-medium mb-2">{exercise.name}</h4>
                  <ol className="list-decimal list-inside space-y-1">
                    {exercise.steps.map((step, sIdx) => (
                      <li key={sIdx} className="text-gray-400 text-sm">{step}</li>
                    ))}
                  </ol>
                </div>
              ))}
            </motion.div>
          )}

          {/* Acknowledge Button */}
          <button
            onClick={handleAcknowledge}
            disabled={acknowledged}
            className={`w-full py-3 rounded-xl font-semibold transition-all ${
              acknowledged 
                ? 'bg-green-500/30 text-green-300 cursor-not-allowed'
                : 'bg-gradient-to-r from-primary-DEFAULT to-primary-light text-white hover:opacity-90'
            }`}
          >
            {acknowledged ? '✓ Take Care' : 'I Understand, Continue'}
          </button>

          <p className="text-center text-gray-500 text-xs mt-4">
            Your journal entry has been saved. This message is shown because we care.
          </p>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

export default CrisisModal;
