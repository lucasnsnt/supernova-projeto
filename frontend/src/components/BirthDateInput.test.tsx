import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { BirthDateInput } from './BirthDateInput'
import { birthDateError, displayBirthDate, isoBirthDate } from '../lib/birthDate'

afterEach(cleanup)

function Form() {
  const [value, setValue] = useState('')
  return <label>Nascimento<BirthDateInput value={value} onChange={setValue} /></label>
}

describe('data de nascimento', () => {
  it('exibe e converte dia/mês/ano sem mudar o dia por fuso horário', () => {
    expect(displayBirthDate('2004-09-15')).toBe('15/09/2004')
    expect(isoBirthDate('15/09/2004')).toBe('2004-09-15')
  })
  it('rejeita datas impossíveis e respeita anos bissextos', () => {
    expect(isoBirthDate('31/04/2004')).toBeNull()
    expect(isoBirthDate('29/02/2003')).toBeNull()
    expect(isoBirthDate('29/02/2004')).toBe('2004-02-29')
    expect(isoBirthDate('15/09/04')).toBeNull()
    expect(isoBirthDate('00/09/2004')).toBeNull()
    expect(birthDateError('01/01/9999')).toContain('anterior a hoje')
  })
  it('adiciona barras durante a digitação e sinaliza uma data inválida', async () => {
    render(<Form />)
    const input = screen.getByLabelText('Nascimento')
    await userEvent.type(input, '15092004')
    expect(input).toHaveValue('15/09/2004')
    expect(input).toBeValid()
    await userEvent.clear(input)
    await userEvent.type(input, '31042004')
    expect(input).toHaveValue('31/04/2004')
    expect(input).toBeInvalid()
  })
})
