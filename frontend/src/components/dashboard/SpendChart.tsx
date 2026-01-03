import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts';

interface SpendChartProps {
  data: any[];
}

const SpendChart = ({ data }: SpendChartProps) => {
  return (
    <div className="card">
      <h3 className="text-xl font-semibold mb-4">Spending Over Time</h3>
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={data}>
          <defs>
            <linearGradient id="colorSpent" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#333" />
          <XAxis dataKey="date" stroke="#a3a3a3" />
          <YAxis stroke="#a3a3a3" />
          <Tooltip 
            contentStyle={{ 
              backgroundColor: '#151515', 
              border: '1px solid #333',
              borderRadius: '8px',
              color: '#f5f5f5'
            }} 
          />
          <Area 
            type="monotone" 
            dataKey="amount" 
            stroke="#3b82f6" 
            fillOpacity={1} 
            fill="url(#colorSpent)" 
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

export default SpendChart;

