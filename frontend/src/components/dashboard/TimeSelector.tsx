import { Calendar } from 'lucide-react';
import { useState, useEffect } from 'react';

interface TimeSelectorProps {
  period: string;
  onPeriodChange: (period: string) => void;
}

type PeriodMode = 'THIS_MONTH' | 'SPECIFIC_MONTH' | 'YTD';

const TimeSelector = ({ period, onPeriodChange }: TimeSelectorProps) => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth(); // 0-indexed
  const months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  // Determine current mode from period string
  const getCurrentMode = (): PeriodMode => {
    if (period === 'THIS_MONTH') return 'THIS_MONTH';
    if (period === 'YTD') return 'YTD';
    if (period.startsWith('MONTH:')) return 'SPECIFIC_MONTH';
    return 'THIS_MONTH'; // default
  };

  const [mode, setMode] = useState<PeriodMode>(getCurrentMode());
  const [selectedYear, setSelectedYear] = useState(currentYear);
  const [selectedMonth, setSelectedMonth] = useState(currentMonth);

  // Parse period to extract year/month when mode is SPECIFIC_MONTH
  useEffect(() => {
    const currentMode = getCurrentMode();
    setMode(currentMode);
    
    if (currentMode === 'SPECIFIC_MONTH' && period.startsWith('MONTH:')) {
      const parts = period.split(':');
      if (parts.length === 3) {
        setSelectedYear(parseInt(parts[1]) || currentYear);
        setSelectedMonth((parseInt(parts[2]) || (currentMonth + 1)) - 1); // Convert 1-indexed to 0-indexed
      }
    }
  }, [period]);

  const handleModeChange = (newMode: PeriodMode) => {
    setMode(newMode);
    
    if (newMode === 'THIS_MONTH') {
      onPeriodChange('THIS_MONTH');
    } else if (newMode === 'YTD') {
      onPeriodChange('YTD');
    } else if (newMode === 'SPECIFIC_MONTH') {
      // Trigger with current selections
      onPeriodChange(`MONTH:${selectedYear}:${selectedMonth + 1}`);
    }
  };

  const handleYearChange = (year: number) => {
    setSelectedYear(year);
    if (mode === 'SPECIFIC_MONTH') {
      onPeriodChange(`MONTH:${year}:${selectedMonth + 1}`);
    } else if (mode === 'YTD') {
      // For YTD, we might want to pass year, but backend expects just 'YTD'
      // Keeping it as YTD for now, backend should handle current year
      onPeriodChange('YTD');
    }
  };

  const handleMonthChange = (month: number) => {
    setSelectedMonth(month);
    if (mode === 'SPECIFIC_MONTH') {
      onPeriodChange(`MONTH:${selectedYear}:${month + 1}`);
    }
  };

  return (
    <div className="card mb-6">
      <div className="flex items-center space-x-4 mb-4">
        <Calendar size={20} className="text-primary" />
        <h2 className="text-xl font-semibold">Time Period</h2>
      </div>
      
      <div className="flex flex-wrap items-center gap-3">
        {/* Toggle Group: Three buttons */}
        <div className="flex items-center gap-2 bg-background rounded-lg p-1 border border-gray-700">
          <button
            onClick={() => handleModeChange('THIS_MONTH')}
            className={`px-4 py-2 rounded-md font-medium transition-all ${
              mode === 'THIS_MONTH'
                ? 'bg-primary text-white'
                : 'text-text-muted hover:text-text hover:bg-card-hover'
            }`}
          >
            This Month
          </button>
          
          <button
            onClick={() => handleModeChange('SPECIFIC_MONTH')}
            className={`px-4 py-2 rounded-md font-medium transition-all ${
              mode === 'SPECIFIC_MONTH'
                ? 'bg-primary text-white'
                : 'text-text-muted hover:text-text hover:bg-card-hover'
            }`}
          >
            Specific Month
          </button>
          
          <button
            onClick={() => handleModeChange('YTD')}
            className={`px-4 py-2 rounded-md font-medium transition-all ${
              mode === 'YTD'
                ? 'bg-primary text-white'
                : 'text-text-muted hover:text-text hover:bg-card-hover'
            }`}
          >
            Year to Date
          </button>
        </div>

        {/* Conditional Dropdowns */}
        {mode === 'SPECIFIC_MONTH' && (
          <div className="flex items-center space-x-2">
            <select
              value={selectedYear}
              onChange={(e) => handleYearChange(parseInt(e.target.value))}
              className="bg-background border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary text-text"
            >
              {Array.from({ length: 5 }, (_, i) => currentYear - 2 + i).map(year => (
                <option key={year} value={year}>{year}</option>
              ))}
            </select>
            
            <select
              value={selectedMonth}
              onChange={(e) => handleMonthChange(parseInt(e.target.value))}
              className="bg-background border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary text-text"
            >
              {months.map((monthName, index) => (
                <option key={index} value={index}>{monthName}</option>
              ))}
            </select>
          </div>
        )}
      </div>
    </div>
  );
};

export default TimeSelector;
