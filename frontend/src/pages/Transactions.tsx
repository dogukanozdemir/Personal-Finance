import { useTransactions } from '../hooks/useTransactions';
import { format } from 'date-fns';

const Transactions = () => {
  const { transactions, loading, error } = useTransactions(30);
  
  if (loading) return <div className="text-text-muted">Loading transactions...</div>;
  if (error) return <div className="text-danger">Error: {error}</div>;
  
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Transactions</h1>
        <button className="btn-primary">Export</button>
      </div>
      
      {/* Filters */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <input
            type="text"
            placeholder="Search merchant..."
            className="px-4 py-2 bg-background border border-gray-700 rounded-lg"
          />
          <select className="px-4 py-2 bg-background border border-gray-700 rounded-lg">
            <option>All Categories</option>
            <option>Market</option>
            <option>Elektronik</option>
            <option>Yeme / İçme</option>
          </select>
          <input
            type="number"
            placeholder="Min amount"
            className="px-4 py-2 bg-background border border-gray-700 rounded-lg"
          />
          <input
            type="number"
            placeholder="Max amount"
            className="px-4 py-2 bg-background border border-gray-700 rounded-lg"
          />
        </div>
      </div>
      
      {/* Transactions Table */}
      <div className="card overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-700">
              <th className="text-left py-3 px-4">Date</th>
              <th className="text-left py-3 px-4">Merchant</th>
              <th className="text-left py-3 px-4">Category</th>
              <th className="text-right py-3 px-4">Amount</th>
              <th className="text-left py-3 px-4">Account</th>
              <th className="text-right py-3 px-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((transaction) => (
              <tr key={transaction.id} className="border-b border-gray-800 hover:bg-card-hover">
                <td className="py-3 px-4">
                  {format(new Date(transaction.transactionDate), 'dd/MM/yyyy')}
                </td>
                <td className="py-3 px-4">{transaction.merchant}</td>
                <td className="py-3 px-4">
                  <span className="px-2 py-1 bg-primary/20 text-primary rounded text-sm">
                    {transaction.category || 'Uncategorized'}
                  </span>
                </td>
                <td className={`py-3 px-4 text-right font-medium ${
                  transaction.amount < 0 ? 'text-danger' : 'text-success'
                }`}>
                  {transaction.amount.toFixed(2)} TL
                </td>
                <td className="py-3 px-4">
                  {transaction.accountId === 1 ? 'Debit' : 'Credit'}
                </td>
                <td className="py-3 px-4 text-right">
                  <button className="text-primary hover:text-accent text-sm">
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Transactions;

