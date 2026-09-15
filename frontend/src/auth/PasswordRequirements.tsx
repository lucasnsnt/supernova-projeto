import { passwordRules } from './passwordRules'

export function PasswordRequirements({ password, id }: { password: string; id: string }) {
  return <div id={id} className="password-requirements">
    <p>A senha precisa ter:</p>
    <ul aria-label="Requisitos da senha">{passwordRules.map(rule => {
      const met = rule.test(password)
      return <li key={rule.label} className={met ? 'requirement-met' : 'requirement-pending'}>
        <span aria-hidden="true">{met ? '✓' : '✕'}</span>
        <span className="sr-only">{met ? 'Atendido: ' : 'Pendente: '}</span>{rule.label}
      </li>
    })}</ul>
  </div>
}
