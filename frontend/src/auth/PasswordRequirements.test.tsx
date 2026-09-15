import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { PasswordRequirements } from './PasswordRequirements'
import { passwordRules } from './passwordRules'

afterEach(cleanup)

describe('requisitos da senha', () => {
  it('mostra todos os requisitos pendentes quando vazia', () => {
    render(<PasswordRequirements id="requirements" password="" />)
    expect(screen.getAllByRole('listitem')).toHaveLength(5)
    screen.getAllByRole('listitem').forEach(item => {
      expect(item).toHaveClass('requirement-pending')
      expect(item).toHaveTextContent('✕')
    })
  })

  it('atualiza os checks conforme a senha muda e volta a ficar incompleta', () => {
    const { rerender } = render(<PasswordRequirements id="requirements" password="abc" />)
    expect(screen.getByText('Uma letra minúscula (a–z)').closest('li')).toHaveClass('requirement-met')
    expect(screen.getByText('Um número (0–9)').closest('li')).toHaveClass('requirement-pending')
    rerender(<PasswordRequirements id="requirements" password="Teste123!" />)
    screen.getAllByRole('listitem').forEach(item => {
      expect(item).toHaveClass('requirement-met')
      expect(item).toHaveTextContent('✓')
    })
    rerender(<PasswordRequirements id="requirements" password="Teste123" />)
    expect(screen.getByText('Um caractere especial (ex.: @, !, #)').closest('li')).toHaveClass('requirement-pending')
  })

  it('respeita os limites de tamanho e as letras ASCII do backend', () => {
    const length = passwordRules[0].test
    expect(length('Aa1!abc')).toBe(false)
    expect(length('Aa1!abcd')).toBe(true)
    expect(length('Aa1!' + 'a'.repeat(60))).toBe(true)
    expect(length('Aa1!' + 'a'.repeat(61))).toBe(false)
    expect(passwordRules[1].test('Á')).toBe(false)
    expect(passwordRules[2].test('á')).toBe(false)
  })
})
