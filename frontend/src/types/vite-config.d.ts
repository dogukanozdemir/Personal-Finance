/// <reference types="node" />

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

declare module '@vitejs/plugin-react' {
  interface Plugin {
    name: string;
    [key: string]: any;
  }
  
  interface Options {
    include?: string | RegExp | Array<string | RegExp>;
    exclude?: string | RegExp | Array<string | RegExp>;
    babel?: any;
    jsxRuntime?: 'automatic' | 'classic';
    jsxImportSource?: string;
    jsxPure?: boolean;
  }
  
  export default function react(options?: Options): Plugin;
}

