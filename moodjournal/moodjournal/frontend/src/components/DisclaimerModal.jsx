import { AlertTriangle } from 'lucide-react';
import { useEffect, useState } from 'react';






const DisclaimerModal = () => {
  const [isOpen, setIsOpen] = useState(false);
  
  useEffect(() => {
    
    const acknowledged = localStorage.getItem('disclaimer_acknowledged');
    if (!acknowledged) {
      setIsOpen(true);
    }
  }, []);
  
  const handleAccept = () => {
    localStorage.setItem('disclaimer_acknowledged', 'true');
    setIsOpen(false);
  };
  
  if (!isOpen) return null;
  
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="bg-dark-card border border-yellow-500/50 rounded-2xl max-w-lg mx-4 p-6 shadow-2xl">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 bg-yellow-500/20 rounded-full">
            <AlertTriangle className="w-6 h-6 text-yellow-500" />
          </div>
          <h2 className="text-xl font-bold text-white">⚠️ Important Disclaimer</h2>
        </div>
        
        <div className="space-y-4 text-gray-300 text-sm leading-relaxed">
          <p className="font-semibold text-white">
            This is a <span className="text-yellow-400">PROTOTYPE</span> application for educational purposes only.
          </p>
          
          <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4">
            <p className="font-medium text-red-400 mb-2">🚫 NOT a Professional Mental Health Tool</p>
            <ul className="list-disc list-inside space-y-1 text-gray-400">
              <li>Not designed, reviewed, or validated by mental health professionals</li>
              <li>Not FDA approved or clinically tested</li>
              <li>Not intended to diagnose, treat, or replace professional care</li>
            </ul>
          </div>
          
          <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
            <p className="font-medium text-blue-400 mb-2">📋 Purpose</p>
            <p className="text-gray-400">
              This application is a <strong>technical demonstration</strong> of AI-assisted journaling 
              and mood tracking concepts. It was created as a college project to explore 
              human-computer interaction and natural language processing.
            </p>
          </div>
          
          <p className="text-yellow-500/80 text-xs">
            If you are experiencing mental health issues, please consult a qualified healthcare provider 
            or call a crisis helpline (988 in USA, iCALL: 9152987821 in India).
          </p>
        </div>
        
        <button
          onClick={handleAccept}
          className="w-full mt-6 py-3 bg-gradient-to-r from-yellow-600 to-orange-600 hover:from-yellow-500 hover:to-orange-500 text-white font-semibold rounded-xl transition-all duration-200"
        >
          I Understand - This is a Prototype
        </button>
        
        <p className="text-center text-gray-500 text-xs mt-3">
          By clicking above, you acknowledge reading this disclaimer.
        </p>
      </div>
    </div>
  );
};

export default DisclaimerModal;
