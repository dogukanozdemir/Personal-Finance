import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';

interface SpendChartProps {
  dataPoints: Map<string, number> | Record<string, number> | undefined;
  isMonthly?: boolean;
}

const SpendChart = ({ dataPoints, isMonthly = false }: SpendChartProps) => {
  // Convert Map or object to array format for recharts
  const chartData = dataPoints 
    ? Object.entries(dataPoints).map(([date, amount]) => ({
        period: date,
        amount: typeof amount === 'number' ? amount : parseFloat(String(amount)) || 0,
      }))
    : [];

  if (!chartData || chartData.length === 0) {
    return (
      <div className="card">
        <h3 className="text-xl font-semibold mb-4">Spending Over Time</h3>
        <div className="flex items-center justify-center h-[400px] text-text-muted">
          No data available for this period
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <h3 className="text-xl font-semibold mb-4">Spending Over Time</h3>
      <ResponsiveContainer width="100%" height={400}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#333" />
          <XAxis 
            dataKey="period" 
            stroke="#a3a3a3"
            tick={{ fill: '#a3a3a3' }}
            angle={isMonthly ? 0 : -45}
            textAnchor={isMonthly ? 'middle' : 'end'}
            height={isMonthly ? 30 : 80}
          />
          <YAxis 
            stroke="#a3a3a3"
            tick={{ fill: '#a3a3a3' }}
            label={{ value: 'Spend (TL)', angle: -90, position: 'insideLeft', fill: '#a3a3a3' }}
          />
          <Tooltip 
            contentStyle={{ 
              backgroundColor: '#151515', 
              border: '1px solid #333',
              borderRadius: '8px',
              color: '#f5f5f5'
            }}
            formatter={(value: number) => [`${value.toFixed(2)} TL`, 'Spend']}
          />
          <Legend />
          <Line 
            type="monotone" 
            dataKey="amount" 
            stroke="#3b82f6" 
            strokeWidth={2}
            dot={{ fill: '#3b82f6', r: 3 }}
            name="Spend"
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default SpendChart;
