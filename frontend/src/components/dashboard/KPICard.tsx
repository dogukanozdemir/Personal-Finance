import { TrendingUp, TrendingDown } from 'lucide-react';

interface KPICardProps {
  title: string;
  value: string;
  change?: number;
  subtitle?: string | React.ReactNode;
  icon?: React.ReactNode;
}

const KPICard = ({ title, value, change, subtitle, icon }: KPICardProps) => {
  const isPositive = change && change > 0;
  const isNegative = change && change < 0;
  
  return (
    <div className="card">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-text-muted text-sm mb-2">{title}</p>
          <h3 className="text-3xl font-bold mb-2">{value}</h3>
          
          {change !== undefined && (
            <div className="flex items-center space-x-2">
              {isPositive && <TrendingUp size={16} className="text-danger" />}
              {isNegative && <TrendingDown size={16} className="text-success" />}
              <span className={`text-sm ${isPositive ? 'text-danger' : isNegative ? 'text-success' : 'text-text-muted'}`}>
                {Math.abs(change).toFixed(1)}% vs previous period
              </span>
            </div>
          )}
          
          {subtitle && <p className="text-text-muted text-sm mt-1">{subtitle}</p>}
        </div>
        
        {icon && (
          <div className="text-primary opacity-50">
            {icon}
          </div>
        )}
      </div>
    </div>
  );
};

export default KPICard;

