import { Target } from 'lucide-react';

const Budgets = () => {
  const budgets = [
    { category: 'Market', limit: 2000, spent: 1650, percentage: 82.5 },
    { category: 'Elektronik', limit: 1000, spent: 1180, percentage: 118 },
    { category: 'Yeme / İçme', limit: 1500, spent: 800, percentage: 53.3 },
  ];
  
  const getProgressColor = (percentage: number) => {
    if (percentage >= 100) return 'bg-danger';
    if (percentage >= 80) return 'bg-accent';
    return 'bg-success';
  };
  
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Budgets & Targets</h1>
        <button className="btn-primary">Add Budget</button>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {budgets.map((budget) => (
          <div key={budget.category} className="card">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">{budget.category}</h3>
              <Target className="text-primary" size={20} />
            </div>
            
            <div className="mb-4">
              <div className="flex justify-between text-sm mb-2">
                <span className="text-text-muted">Spent</span>
                <span className="font-medium">{budget.spent.toFixed(2)} TL</span>
              </div>
              <div className="flex justify-between text-sm mb-2">
                <span className="text-text-muted">Limit</span>
                <span className="font-medium">{budget.limit.toFixed(2)} TL</span>
              </div>
            </div>
            
            <div className="relative h-3 bg-background rounded-full overflow-hidden mb-2">
              <div 
                className={`absolute h-full ${getProgressColor(budget.percentage)} transition-all`}
                style={{ width: `${Math.min(budget.percentage, 100)}%` }}
              />
            </div>
            
            <div className="flex justify-between items-center">
              <span className={`text-sm font-medium ${
                budget.percentage >= 100 ? 'text-danger' : 
                budget.percentage >= 80 ? 'text-accent' : 
                'text-success'
              }`}>
                {budget.percentage.toFixed(1)}%
              </span>
              
              {budget.percentage >= 100 && (
                <span className="text-xs text-danger">
                  {((budget.percentage - 100) / 100 * budget.limit).toFixed(2)} TL over
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Budgets;

