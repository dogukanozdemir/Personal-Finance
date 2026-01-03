import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import Dashboard from './pages/Dashboard';
import Transactions from './pages/Transactions';
import Insights from './pages/Insights';
import Budgets from './pages/Budgets';
import AIAnalysis from './pages/AIAnalysis';
import Import from './pages/Import';

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/insights" element={<Insights />} />
          <Route path="/budgets" element={<Budgets />} />
          <Route path="/ai" element={<AIAnalysis />} />
          <Route path="/import" element={<Import />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;

