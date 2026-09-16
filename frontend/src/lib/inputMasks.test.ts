import { describe, expect, it } from 'vitest'
import { licensePlateMask, phoneMask, validCnh, validLicensePlate, validPhone } from './inputMasks'

describe('máscaras e validações de cadastro', () => {
  it('formata e valida celular brasileiro', () => {
    expect(phoneMask('79999999999')).toBe('(79) 99999-9999')
    expect(validPhone('(79) 99999-9999')).toBe(true)
    expect(validPhone('123')).toBe(false)
  })

  it('aceita placas antiga e Mercosul e limita caracteres', () => {
    expect(licensePlateMask('abc-1d234')).toBe('ABC1D23')
    expect(validLicensePlate('ABC1234')).toBe(true)
    expect(validLicensePlate('ABC1D23')).toBe(true)
    expect(validLicensePlate('1234567')).toBe(false)
  })

  it('exige os onze números da CNH', () => {
    expect(validCnh('12345678900')).toBe(true)
    expect(validCnh('123')).toBe(false)
  })
})
