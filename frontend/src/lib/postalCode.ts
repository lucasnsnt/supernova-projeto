export type PostalCodeAddress = { street: string; neighborhood: string; city: string; state: string }

export function formatPostalCode(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 8)
  return digits.length > 5 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : digits
}

export async function lookupPostalCode(value: string): Promise<PostalCodeAddress> {
  const digits = value.replace(/\D/g, '')
  if (digits.length !== 8) throw new Error('Informe um CEP com 8 números.')
  const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`)
  if (!response.ok) throw new Error('Não foi possível consultar o CEP agora.')
  const data = await response.json() as { erro?: boolean; logradouro?: string; bairro?: string; localidade?: string; uf?: string }
  if (data.erro) throw new Error('CEP não encontrado.')
  return { street: data.logradouro ?? '', neighborhood: data.bairro ?? '', city: data.localidade ?? '', state: data.uf ?? '' }
}
