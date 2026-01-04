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

