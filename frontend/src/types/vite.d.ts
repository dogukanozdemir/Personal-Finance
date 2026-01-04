declare module 'vite' {
  export interface Plugin {
    name: string;
    [key: string]: any;
  }
  
  export interface UserConfig {
    plugins?: Plugin[];
    server?: {
      host?: string;
      port?: number;
      proxy?: Record<string, {
        target: string;
        changeOrigin?: boolean;
      }>;
    };
  }
  
  export function defineConfig(config: UserConfig): UserConfig;
}

