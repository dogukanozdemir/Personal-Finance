declare namespace NodeJS {
  interface ProcessEnv {
    [key: string]: string | undefined;
    DOCKER?: string;
    CONTAINER?: string;
  }
}

declare var process: {
  env: NodeJS.ProcessEnv;
};

