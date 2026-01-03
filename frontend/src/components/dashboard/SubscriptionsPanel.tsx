import { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import { subscriptionAPI } from '../../utils/api';

interface Subscription {
  merchant: string;
  averageAmount: number;
  transactionCount: number;
  frequency: string;
  lastTransactionDate: string;
  active: boolean;
  amountVariance?: number;
}

const SubscriptionsPanel = () => {
  const [activeSubscriptions, setActiveSubscriptions] = useState<Subscription[]>([]);
  const [potentialSubscriptions, setPotentialSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [showPotential, setShowPotential] = useState(false);

  useEffect(() => {
    loadSubscriptions();
  }, []);

  const loadSubscriptions = async () => {
    try {
      setLoading(true);
      const [activeRes, potentialRes] = await Promise.all([
        subscriptionAPI.getActive(),
        subscriptionAPI.getPotential()
      ]);
      setActiveSubscriptions(activeRes.data || []);
      setPotentialSubscriptions(potentialRes.data || []);
    } catch (error) {
      console.error('Error loading subscriptions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async (merchant: string) => {
    try {
      await subscriptionAPI.confirm(merchant);
      await loadSubscriptions(); // Reload to update lists
    } catch (error) {
      console.error('Error confirming subscription:', error);
    }
  };

  const handleUnmark = async (merchant: string) => {
    try {
      await subscriptionAPI.unmark(merchant);
      await loadSubscriptions(); // Reload to update lists
    } catch (error) {
      console.error('Error unmarking subscription:', error);
    }
  };

  if (loading) {
    return (
      <div className="card">
        <div className="text-text-muted">Loading subscriptions...</div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-xl font-semibold">Subscriptions</h3>
        <AlertCircle size={20} className="text-accent" />
      </div>

      {/* Active Subscriptions */}
      {activeSubscriptions.length > 0 && (
        <div className="mb-4">
          <h4 className="text-sm font-semibold text-text-muted mb-2">Active ({activeSubscriptions.length})</h4>
          <div className="space-y-2">
            {activeSubscriptions.map((sub, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-background rounded-lg">
                <div className="flex-1">
                  <p className="font-medium">{sub.merchant}</p>
                  <p className="text-sm text-text-muted">{sub.frequency} • {sub.transactionCount} transactions</p>
                </div>
                <div className="text-right mr-3">
                  <p className="font-semibold">{sub.averageAmount.toFixed(2)} TL</p>
                  <span className={`text-xs ${sub.active ? 'text-success' : 'text-text-muted'}`}>
                    {sub.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <button
                  onClick={() => handleUnmark(sub.merchant)}
                  className="text-danger hover:text-danger/80"
                  title="Unmark as subscription"
                >
                  <XCircle size={18} />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Potential Subscriptions (needs confirmation) */}
      {potentialSubscriptions.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-sm font-semibold text-text-muted">
              Potential ({potentialSubscriptions.length}) - Needs Confirmation
            </h4>
            <button
              onClick={() => setShowPotential(!showPotential)}
              className="text-xs text-primary hover:text-primary/80"
            >
              {showPotential ? 'Hide' : 'Show'}
            </button>
          </div>
          
          {showPotential && (
            <div className="space-y-2">
              {potentialSubscriptions.map((sub, index) => (
                <div key={index} className="flex items-center justify-between p-3 bg-background rounded-lg border border-accent/30">
                  <div className="flex-1">
                    <p className="font-medium">{sub.merchant}</p>
                    <p className="text-xs text-text-muted">
                      {sub.frequency} • {sub.transactionCount} transactions • 
                      {sub.amountVariance && ` ${sub.amountVariance.toFixed(1)}% variance`}
                    </p>
                  </div>
                  <div className="text-right mr-3">
                    <p className="font-semibold text-sm">{sub.averageAmount.toFixed(2)} TL</p>
                  </div>
                  <button
                    onClick={() => handleConfirm(sub.merchant)}
                    className="text-success hover:text-success/80"
                    title="Confirm as subscription"
                  >
                    <CheckCircle size={18} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeSubscriptions.length === 0 && potentialSubscriptions.length === 0 && (
        <div className="text-text-muted text-sm text-center py-4">
          No subscriptions detected yet. Upload more transactions to detect recurring charges.
        </div>
      )}
    </div>
  );
};

export default SubscriptionsPanel;

