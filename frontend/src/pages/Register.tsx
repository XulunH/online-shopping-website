import { FormEvent, useState, type Dispatch, type SetStateAction } from 'react'
import { api } from '../api/client'
import { useNavigate } from 'react-router-dom'

type AddressDto = {
  line1?: string
  line2?: string
  city?: string
  state?: string
  zip?: string
  country?: string
}

export default function Register() {
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const [ship, setShip] = useState<AddressDto>({})
  const [bill, setBill] = useState<AddressDto>({})

  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const navigate = useNavigate()

  function setAddr(setter: Dispatch<SetStateAction<AddressDto>>, key: keyof AddressDto, val: string) {
    setter(prev => ({ ...prev, [key]: val }))
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await api('/api/v1/accounts/register', {
        method: 'POST',
        body: JSON.stringify({
          email,
          username,
          password,
          shippingAddress: ship,
          billingAddress: bill,
        }),
      })
      setSuccess('Registered. You can now login.')
      setTimeout(() => navigate('/login', { replace: true }), 800)
    } catch (err: any) {
      setError(err.message || 'Registration failed')
    }
  }

  const addrFields: (keyof AddressDto)[] = ['line1', 'line2', 'city', 'state', 'zip', 'country']

  return (
    <form onSubmit={onSubmit} style={{ display: 'grid', gap: 8, maxWidth: 520 }}>
      <h2>Register</h2>
      <input placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} />
      <input placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} />
      <input placeholder="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} />

      <fieldset>
        <legend>Shipping Address</legend>
        {addrFields.map(k => (
          <input key={k} placeholder={k} value={ship[k] || ''} onChange={e => setAddr(setShip, k, e.target.value)} />
        ))}
      </fieldset>

      <fieldset>
        <legend>Billing Address</legend>
        {addrFields.map(k => (
          <input key={k} placeholder={k} value={bill[k] || ''} onChange={e => setAddr(setBill, k, e.target.value)} />
        ))}
      </fieldset>

      <button type="submit">Create Account</button>
      {success && <div style={{ color: 'green' }}>{success}</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
    </form>
  )
}


