declare module 'lucide-react' {
  import { FC, SVGProps } from 'react';
  
  export interface IconProps extends SVGProps<SVGSVGElement> {
    size?: string | number;
    strokeWidth?: string | number;
  }
  
  export const Calendar: FC<IconProps>;
  export const Wallet: FC<IconProps>;
  export const TrendingUp: FC<IconProps>;
  export const TrendingDown: FC<IconProps>;
  export const Upload: FC<IconProps>;
  export const CheckCircle: FC<IconProps>;
  export const XCircle: FC<IconProps>;
  export const FileText: FC<IconProps>;
  export const Loader: FC<IconProps>;
  export const Trash2: FC<IconProps>;
  export const AlertTriangle: FC<IconProps>;
  export const AlertCircle: FC<IconProps>;
  export const Send: FC<IconProps>;
  export const Target: FC<IconProps>;
  export const Clock: FC<IconProps>;
  export const ShoppingBag: FC<IconProps>;
  export const Search: FC<IconProps>;
  // Add other icons as needed
}

