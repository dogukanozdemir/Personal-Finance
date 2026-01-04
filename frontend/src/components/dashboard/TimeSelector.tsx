import { Calendar } from 'lucide-react';

interface TimeSelectorProps {
  period: string;
  onPeriodChange: (period: string) => void;
}

const TimeSelector = ({ period, onPeriodChange }: TimeSelectorProps) => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth();
  const months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const handleThisMonth = () => {
    onPeriodChange('THIS_MONTH');
  };

  const handleMonthSelect = (year: number, month: number) => {
    onPeriodChange(`MONTH:${year}:${month + 1}`);
  };

  const handleYTD = () => {
    onPeriodChange('YTD');
  };

  const isThisMonth = period === 'THIS_MONTH';
  const isYTD = period === 'YTD' || period === 'YTD';
  const isSpecificMonth = period === 'MONTH' || period.startsWith('MONTH:');
  
  // Parse period to get year and month for dropdowns
  let selectedYear = currentYear;
  let selectedMonth = currentMonth;
  
  if (isSpecificMonth && period.startsWith('MONTH:')) {
    const parts = period.split(':');
    if (parts.length === 3) {
      selectedYear = parseInt(parts[1]) || currentYear;
      selectedMonth = (parseInt(parts[2]) || (currentMonth + 1)) - 1;
    }
  }

  return (
    <div className="card mb-6">
      <div className="flex items-center space-x-4">
        <Calendar size={20} className="text-primary" />
        <h2 className="text-xl font-semibold">Time Period</h2>
      </div>
      
      <div className="mt-4 flex flex-wrap items-center gap-3">
        {/* This Month Button */}
        <button
          onClick={handleThisMonth}
          className={`px-4 py-2 rounded-lg font-medium transition-all ${
            isThisMonth
              ? 'bg-primary text-white'
              : 'bg-background text-text-muted hover:bg-accent/20'
          }`}
        >
          This Month
        </button>

        {/* Month Picker */}
        <div className="flex items-center space-x-2">
          <select
            value={selectedYear}
            onChange={(e) => {
              const year = parseInt(e.target.value);
              handleMonthSelect(year, selectedMonth);
            }}
            className="bg-background border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {Array.from({ length: 5 }, (_, i) => currentYear - 2 + i).map(year => (
              <option key={year} value={year}>{year}</option>
            ))}
          </select>
          
          <select
            value={selectedMonth}
            onChange={(e) => {
              const month = parseInt(e.target.value);
              handleMonthSelect(selectedYear, month);
            }}
            className="bg-background border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {months.map((month, index) => (
              <option key={index} value={index}>{month}</option>
            ))}
          </select>
        </div>

        {/* YTD Button */}
        <button
          onClick={handleYTD}
          className={`px-4 py-2 rounded-lg font-medium transition-all ${
            isYTD
              ? 'bg-primary text-white'
              : 'bg-background text-text-muted hover:bg-accent/20'
          }`}
        >
          Year to Date
        </button>
      </div>
    </div>
  );
};

export default TimeSelector;

