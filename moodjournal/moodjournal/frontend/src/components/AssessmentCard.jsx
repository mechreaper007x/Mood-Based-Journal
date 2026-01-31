import { motion } from 'framer-motion';

/**
 * AssessmentCard - Reusable card component for psychological assessments.
 * Displays a question with MCQ options as styled buttons.
 * 
 * @param {Object} props
 * @param {number} props.number - Question number
 * @param {string} props.question - The question text
 * @param {Array} props.options - Array of { value, label } options
 * @param {string|number} props.selectedAnswer - Currently selected answer value
 * @param {Function} props.onSelect - Callback when an option is selected
 * @param {string} props.type - 'likert' | 'paired' | 'mcq'
 */
const AssessmentCard = ({ 
  number, 
  question, 
  options, 
  selectedAnswer, 
  onSelect, 
  type = 'mcq' 
}) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: number * 0.02 }}
      className="bg-dark-card/50 backdrop-blur-sm border border-white/10 rounded-xl p-5 hover:border-primary-DEFAULT/30 transition-all"
    >
      {/* Question Header */}
      <div className="flex items-start gap-3 mb-4">
        <span className="flex-shrink-0 w-8 h-8 bg-primary-DEFAULT/20 text-primary-DEFAULT rounded-lg flex items-center justify-center text-sm font-bold">
          {number}
        </span>
        <p className="text-white font-medium leading-relaxed">{question}</p>
      </div>

      {/* Options */}
      {type === 'likert' ? (
        <div className="flex justify-between gap-1 sm:gap-2">
          {options.map(opt => (
            <button
              key={opt.value}
              onClick={() => onSelect(opt.value)}
              className={`flex-1 py-2.5 px-1 rounded-lg text-sm font-medium transition-all ${
                selectedAnswer === opt.value
                  ? 'bg-primary-DEFAULT text-white shadow-lg shadow-primary-DEFAULT/30'
                  : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT/50 hover:text-white'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      ) : type === 'paired' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {options.map(opt => (
            <button
              key={opt.value}
              onClick={() => onSelect(opt.value)}
              className={`p-4 rounded-xl text-left transition-all ${
                selectedAnswer === opt.value
                  ? 'bg-primary-DEFAULT/20 border-2 border-primary-DEFAULT text-white'
                  : 'bg-dark-input border-2 border-white/10 text-gray-300 hover:border-primary-DEFAULT/50'
              }`}
            >
              <span className="text-sm leading-relaxed">{opt.label}</span>
            </button>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {options.map(opt => (
            <button
              key={opt.value}
              onClick={() => onSelect(opt.value)}
              className={`p-3 rounded-lg text-sm transition-all ${
                selectedAnswer === opt.value
                  ? 'bg-primary-DEFAULT text-white shadow-lg shadow-primary-DEFAULT/30'
                  : 'bg-dark-input border border-white/10 text-gray-400 hover:border-primary-DEFAULT/50 hover:text-white'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </motion.div>
  );
};

export default AssessmentCard;
