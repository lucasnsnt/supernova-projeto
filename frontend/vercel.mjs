const backendValue = process.env.URL_BACKEND
if (!backendValue) throw new Error('Configure URL_BACKEND neste ambiente da Vercel.')

const backend = new URL(backendValue)
if (backend.protocol !== 'https:' || backend.pathname !== '/' || backend.search || backend.hash || backend.username || backend.password) {
  throw new Error('URL_BACKEND deve ser uma origem HTTPS, sem /api, credenciais ou parâmetros.')
}

export const config = {
  framework: 'vite',
  buildCommand: 'npm run build',
  outputDirectory: 'dist',
  rewrites: [
    { source: '/api/:path*', destination: `${backendValue.replace(/\/$/, '')}/api/:path*` },
    { source: '/(.*)', destination: '/index.html' },
  ],
}
