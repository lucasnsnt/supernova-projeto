import { deploymentEnv, routes } from '@vercel/config/v1'

export const config = {
  framework: 'vite',
  buildCommand: 'npm run build',
  outputDirectory: 'dist',
  git: {
    deploymentEnabled: { prod: false },
  },
  rewrites: [
    routes.rewrite('/api/:path*', `${deploymentEnv('URL_BACKEND')}/api/:path*`),
    { source: '/(.*)', destination: '/index.html' },
  ],
}
