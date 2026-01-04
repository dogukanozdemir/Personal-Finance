import { Link, useLocation } from 'react-router-dom';
import { 
  Home, 
  Upload,
  Settings
} from 'lucide-react';

const Sidebar = () => {
  const location = useLocation();
  
  const menuItems = [
    { path: '/', label: 'Dashboard', icon: Home },
    { path: '/import', label: 'Import', icon: Upload },
    { path: '/settings', label: 'Settings', icon: Settings },
  ];
  
  return (
    <div className="w-64 min-h-screen bg-card border-r border-gray-800 p-4">
      <div className="mb-8">
        <h1 className="text-2xl font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
          Spending Analytics
        </h1>
      </div>
      
      <nav className="space-y-2">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;
          
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-all ${
                isActive
                  ? 'bg-primary text-white'
                  : 'text-text-muted hover:bg-card-hover hover:text-text'
              }`}
            >
              <Icon size={20} />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
};

export default Sidebar;

