import { Search } from 'lucide-react';
import { useState } from 'react';

interface TopBarProps {
  onPeriodChange: (period: string) => void;
  currentPeriod: string;
}

const TopBar = ({ onPeriodChange, currentPeriod }: TopBarProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  
  const periods = ['Today', 'Week', 'Month', 'Year'];
  
  return (
    <div className="bg-card border-b border-gray-800 px-6 py-4">
      <div className="flex items-center justify-between">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-text-muted" size={20} />
          <input
            type="text"
            placeholder="Search transactions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-background border border-gray-700 rounded-lg text-text placeholder-text-muted focus:outline-none focus:border-primary"
          />
        </div>
        
        {/* Period Selector */}
        <div className="flex space-x-2 ml-6">
          {periods.map((period) => (
            <button
              key={period}
              onClick={() => onPeriodChange(period.toLowerCase())}
              className={`px-4 py-2 rounded-lg transition-all ${
                currentPeriod === period.toLowerCase()
                  ? 'bg-primary text-white'
                  : 'bg-background text-text-muted hover:text-text hover:bg-card-hover'
              }`}
            >
              {period}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default TopBar;

