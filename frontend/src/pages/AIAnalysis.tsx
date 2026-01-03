import { useState } from 'react';
import { Send } from 'lucide-react';
import { aiAPI } from '../utils/api';

const AIAnalysis = () => {
  const [messages, setMessages] = useState<any[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  
  const sampleQuestions = [
    "Why was November higher?",
    "What are 3 ways to cut 10% next month?",
    "Find suspicious charges",
    "What subscriptions can I cancel?",
  ];
  
  const handleSend = async () => {
    if (!input.trim()) return;
    
    const userMessage = { role: 'user', content: input };
    setMessages([...messages, userMessage]);
    setInput('');
    setLoading(true);
    
    try {
      const response = await aiAPI.chat(input);
      const aiMessage = { 
        role: 'assistant', 
        content: response.data.answer,
        citations: response.data.citations 
      };
      setMessages(prev => [...prev, aiMessage]);
    } catch (error) {
      console.error('AI chat error:', error);
      const errorMessage = { 
        role: 'assistant', 
        content: 'Sorry, I encountered an error. Please try again.' 
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="flex flex-col h-[calc(100vh-200px)]">
      <h1 className="text-3xl font-bold mb-6">AI Analysis</h1>
      
      {/* Sample Questions */}
      {messages.length === 0 && (
        <div className="mb-6">
          <h3 className="text-text-muted mb-3">Try asking:</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {sampleQuestions.map((question, index) => (
              <button
                key={index}
                onClick={() => setInput(question)}
                className="card text-left hover:border-primary border border-transparent"
              >
                {question}
              </button>
            ))}
          </div>
        </div>
      )}
      
      {/* Messages */}
      <div className="flex-1 overflow-y-auto space-y-4 mb-4">
        {messages.map((message, index) => (
          <div
            key={index}
            className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-2xl px-4 py-3 rounded-lg ${
                message.role === 'user'
                  ? 'bg-primary text-white'
                  : 'bg-card text-text'
              }`}
            >
              <p>{message.content}</p>
              {message.citations && message.citations.length > 0 && (
                <div className="mt-2 pt-2 border-t border-gray-700">
                  <p className="text-xs text-text-muted mb-1">Sources:</p>
                  {message.citations.map((citation: string, i: number) => (
                    <p key={i} className="text-xs text-primary">{citation}</p>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        
        {loading && (
          <div className="flex justify-start">
            <div className="bg-card px-4 py-3 rounded-lg">
              <p className="text-text-muted">Thinking...</p>
            </div>
          </div>
        )}
      </div>
      
      {/* Input */}
      <div className="flex space-x-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleSend()}
          placeholder="Ask a question about your spending..."
          className="flex-1 px-4 py-3 bg-card border border-gray-700 rounded-lg focus:outline-none focus:border-primary"
        />
        <button
          onClick={handleSend}
          disabled={loading}
          className="btn-primary"
        >
          <Send size={20} />
        </button>
      </div>
    </div>
  );
};

export default AIAnalysis;

