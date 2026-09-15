// Mesmas regras de RegisterRequest no backend.
export const passwordRules = [
  { label: 'Entre 8 e 64 caracteres', test: (value: string) => value.length >= 8 && value.length <= 64 },
  { label: 'Uma letra maiúscula (A–Z)', test: (value: string) => /[A-Z]/.test(value) },
  { label: 'Uma letra minúscula (a–z)', test: (value: string) => /[a-z]/.test(value) },
  { label: 'Um número (0–9)', test: (value: string) => /[0-9]/.test(value) },
  { label: 'Um caractere especial (ex.: @, !, #)', test: (value: string) => /[^A-Za-z0-9]/.test(value) },
]
