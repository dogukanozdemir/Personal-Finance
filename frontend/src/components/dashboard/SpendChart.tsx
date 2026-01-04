import { ComposedChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine, Legend, Cell } from 'recharts';

interface SpendChartProps {
  dataPoints: Map<string, number> | Record<string, number> | undefined;
  isMonthly?: boolean;
  overallAvgPerDay?: number;
  period?: string;
}

const SpendChart = ({ 
  dataPoints, 
  isMonthly = false,
  overallAvgPerDay = 0,
  period = 'THIS_MONTH'
}: SpendChartProps) => {
  // Convert Map or object to array format for recharts
  const chartData = dataPoints 
    ? Object.entries(dataPoints).map(([date, amount]) => {
        const amountNum = typeof amount === 'number' ? amount : parseFloat(String(amount)) || 0;
        return {
          period: date,
          amount: amountNum,
          isToday: false, // Will set below
        };
      })
    : [];

  // Identify today
  const today = new Date();
  const todayStr = today.toISOString().split('T')[0]; // YYYY-MM-DD format
  
  const processedData = chartData.map((item) => {
    const isToday = !isMonthly && item.period === todayStr && (period === 'THIS_MONTH' || period.startsWith('MONTH:'));
    return {
      ...item,
      isToday,
    };
  });

  if (!processedData || processedData.length === 0) {
    return (
      <div className="card">
        <h3 className="text-xl font-semibold mb-4">Daily Spending</h3>
        <div className="flex items-center justify-center h-[400px] text-text-muted">
          No data available for this period
        </div>
      </div>
    );
  }

  // Custom tooltip
  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const amountPayload = payload.find((p: any) => p.dataKey === 'amount');
      
      if (!amountPayload) return null;

      const dailySpend = amountPayload.value;
      const diff = dailySpend - overallAvgPerDay;
      const diffPercent = overallAvgPerDay > 0 ? (diff / overallAvgPerDay) * 100 : 0;
      const isAboveAverage = diff > 0;

      return (
        <div className="bg-card border border-gray-700 rounded-lg p-3 shadow-lg">
          <p className="text-text-muted text-sm mb-2">{label}</p>
          <div>
            <p className="text-text font-semibold">
              {`${dailySpend.toFixed(2)} TL`}
              <span className="text-text-muted text-sm ml-2">Daily Spend</span>
            </p>
            {overallAvgPerDay > 0 && (
              <p className={`text-xs mt-1 ${isAboveAverage ? 'text-danger' : 'text-success'}`}>
                {isAboveAverage ? 'Above average' : 'Below average'} by {Math.abs(diff).toFixed(2)} TL ({Math.abs(diffPercent).toFixed(0)}%)
              </p>
            )}
          </div>
        </div>
      );
    }
    return null;
  };

  // Color for bars - orange if above overall average, blue if below
  const getBarColor = (entry: any) => {
    if (overallAvgPerDay <= 0) {
      return '#3b82f6'; // blue/calm
    }
    if (entry.amount > overallAvgPerDay) {
      return '#f59e0b'; // orange/warning
    }
    return '#3b82f6'; // blue/calm
  };

  // Find today's index for vertical line
  const todayIndex = processedData.findIndex((entry) => entry.isToday);

  return (
    <div className="card">
      <h3 className="text-xl font-semibold mb-4">Daily Spending</h3>

      <ResponsiveContainer width="100%" height={400}>
        <ComposedChart 
          data={processedData}
          margin={{ top: 5, right: 50, left: 60, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#333" />
          <XAxis 
            dataKey="period" 
            stroke="#a3a3a3"
            tick={{ fill: '#a3a3a3', fontSize: 12 }}
            angle={isMonthly ? 0 : -45}
            textAnchor={isMonthly ? 'middle' : 'end'}
            height={isMonthly ? 30 : 80}
            tickFormatter={(value) => {
              if (isMonthly) {
                // For YTD, format as "YYYY-MM"
                return value;
              }
              // For daily, format as "MM/DD"
              try {
                const date = new Date(value);
                return `${date.getMonth() + 1}/${date.getDate()}`;
              } catch {
                return value;
              }
            }}
          />
          <YAxis 
            yAxisId="spend"
            stroke="#a3a3a3"
            tick={{ fill: '#a3a3a3' }}
            label={{ value: 'Spend (TL)', angle: -90, position: 'insideLeft', fill: '#a3a3a3', style: { textAnchor: 'middle' } }}
            width={60}
          />
          <Tooltip content={<CustomTooltip />} />
          
          {/* Average per day reference line */}
          {overallAvgPerDay > 0 && (
            <ReferenceLine 
              yAxisId="spend"
              y={overallAvgPerDay} 
              stroke="#a3a3a3" 
              strokeDasharray="5 5" 
              label={{ value: "Average per day", position: "right", fill: "#a3a3a3", fontSize: 12, offset: 10 }}
            />
          )}
          
          {/* Bars for daily/monthly spending with conditional coloring */}
          <Bar 
            yAxisId="spend"
            dataKey="amount" 
            radius={[4, 4, 0, 0]}
            name="Daily Spend"
            fill="#3b82f6"
          >
            {processedData.map((entry, index) => {
              const barColor = getBarColor(entry);
              return (
                <Cell 
                  key={`cell-${index}`} 
                  fill={barColor}
                  stroke={entry.isToday ? '#ef4444' : barColor}
                  strokeWidth={entry.isToday ? 3 : 1}
                />
              );
            })}
          </Bar>
        </ComposedChart>
      </ResponsiveContainer>
      
    </div>
  );
};

export default SpendChart;
