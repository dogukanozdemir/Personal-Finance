import { useState, useEffect } from 'react';
import { insightsAPI } from '../utils/api';
import { AlertCircle, TrendingUp, Clock, ShoppingBag } from 'lucide-react';

const Insights = () => {
  const [insights, setInsights] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const fetchInsights = async () => {
      try {
        const response = await insightsAPI.getInsights();
        setInsights(response.data);
      } catch (error) {
        console.error('Failed to fetch insights:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchInsights();
  }, []);
  
  const getIcon = (type: string) => {
    switch (type) {
      case 'recurring_charge': return <AlertCircle className="text-accent" />;
      case 'weekend_spending': return <TrendingUp className="text-primary" />;
      case 'late_night': return <Clock className="text-danger" />;
      default: return <ShoppingBag className="text-success" />;
    }
  };
  
  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'high': return 'border-danger';
      case 'medium': return 'border-accent';
      default: return 'border-primary';
    }
  };
  
  if (loading) return <div className="text-text-muted">Loading insights...</div>;
  
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Insights & Blind Spots</h1>
        <button className="btn-primary">Regenerate</button>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {insights.map((insight) => (
          <div 
            key={insight.id} 
            className={`card border-l-4 ${getSeverityColor(insight.severity)}`}
          >
            <div className="flex items-start justify-between mb-3">
              <div className="flex-1">
                <h3 className="text-lg font-semibold mb-2">{insight.title}</h3>
                <p className="text-text-muted text-sm">{insight.description}</p>
              </div>
              <div className="ml-4">
                {getIcon(insight.insightType)}
              </div>
            </div>
            <button className="mt-4 text-primary text-sm hover:text-accent">
              View Transactions →
            </button>
          </div>
        ))}
        
        {insights.length === 0 && (
          <div className="col-span-full text-center py-12 text-text-muted">
            No insights available. Upload transactions to generate insights.
          </div>
        )}
      </div>
    </div>
  );
};

export default Insights;

