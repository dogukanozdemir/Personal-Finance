import { AlertCircle } from 'lucide-react';

interface Subscription {
  name: string;
  amount: number;
  frequency: string;
  status: 'active' | 'unused';
}

const SubscriptionsPanel = () => {
  // Mock data - will be replaced with real data from API
  const subscriptions: Subscription[] = [
    { name: 'Getir', amount: 450, frequency: 'Monthly', status: 'active' },
    { name: 'Migros', amount: 210, frequency: 'Weekly', status: 'active' },
  ];
  
  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-xl font-semibold">Active Subscriptions</h3>
        <AlertCircle size={20} className="text-accent" />
      </div>
      
      <div className="space-y-3">
        {subscriptions.map((sub, index) => (
          <div key={index} className="flex items-center justify-between p-3 bg-background rounded-lg">
            <div>
              <p className="font-medium">{sub.name}</p>
              <p className="text-sm text-text-muted">{sub.frequency}</p>
            </div>
            <div className="text-right">
              <p className="font-semibold">{sub.amount.toFixed(2)} TL</p>
              <span className={`text-xs ${sub.status === 'active' ? 'text-success' : 'text-danger'}`}>
                {sub.status}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SubscriptionsPanel;

