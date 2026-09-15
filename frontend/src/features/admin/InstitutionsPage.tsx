import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { createInstitution, institutions, type InstitutionPayload } from './api'

const empty = { name: '', type: 'UNIVERSITY', street: '', number: '', neighborhood: '', city: '', state: '', zipCode: '' }

export function InstitutionsPage() {
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['institutions'], queryFn: institutions })
  const [form, setForm] = useState(empty)
  const create = useMutation({ mutationFn: (payload: InstitutionPayload) => createInstitution(payload), onSuccess: () => { setForm(empty); void client.invalidateQueries({ queryKey: ['institutions'] }) } })
  function submit(event: FormEvent) { event.preventDefault(); create.mutate({ name: form.name, type: form.type, address: { street: form.street, number: form.number, neighborhood: form.neighborhood, city: form.city, state: form.state.toUpperCase(), zipCode: form.zipCode } }) }
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Administração</p><h1>Instituições</h1><p className="muted">Cadastre os destinos disponíveis para os alunos.</p></header><div className="card-list">{list.data?.map((item) => <article className="trip-card" key={item.id}><div><strong>{item.name}</strong><p className="muted">{item.address.street}, {item.address.number} · {item.address.city}/{item.address.state}</p></div><span className="tag neutral">{item.type}</span></article>)}</div><form className="inline-form institution-form" onSubmit={submit}><h3>Nova instituição</h3><input placeholder="Nome" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required /><select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}><option value="UNIVERSITY">Universidade</option><option value="SCHOOL">Escola</option><option value="TECHNICAL_SCHOOL">Escola técnica</option><option value="COURSE">Curso</option><option value="OTHER">Outra</option></select>{(['street', 'number', 'neighborhood', 'city', 'state', 'zipCode'] as const).map((field) => <input key={field} placeholder={labels[field]} maxLength={field === 'state' ? 2 : undefined} value={form[field]} onChange={(event) => setForm({ ...form, [field]: event.target.value })} required />)}<button className="primary-button compact" disabled={create.isPending}>Cadastrar instituição</button>{create.isError && <p className="form-error">{create.error.message}</p>}</form></div>
}

const labels = { street: 'Rua', number: 'Número', neighborhood: 'Bairro', city: 'Cidade', state: 'UF', zipCode: 'CEP' }
