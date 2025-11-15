import { FormEvent, useEffect, useState, type Dispatch, type SetStateAction } from 'react'
import { api } from '../api/client'
import { decodeJwt, getToken } from '../auth'

type AddressDto = {
  line1?: string
  line2?: string
  city?: string
  state?: string
  zip?: string
  country?: string
}

type AccountResponse = {
  id: number
  email: string
  username: string
  shippingAddress?: AddressDto
  billingAddress?: AddressDto
}

export default function Profile() {
  const token = getToken()
  const payload = token ? decodeJwt<any>(token) : null
  const uid: number | null = payload?.uid ?? null

  const [account, setAccount] = useState<AccountResponse | null>(null)
  const [username, setUsername] = useState('')
  const [ship, setShip] = useState<AddressDto>({})
  const [bill, setBill] = useState<AddressDto>({})
  const [status, setStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    async function load() {
      if (!uid) return
      setError(null)
      try {
        const data = await api<AccountResponse>(`/api/v1/accounts/${uid}`)
        if (ignore) return
        setAccount(data)
        setUsername(data.username)
        setShip(data.shippingAddress || {})
        setBill(data.billingAddress || {})
      } catch (err: any) {
        if (!ignore) setError(err.message || 'Failed to load profile')
      }
    }
    load()
    return () => { ignore = true }
  }, [uid])

  function setAddr(setter: Dispatch<SetStateAction<AddressDto>>, key: keyof AddressDto, val: string) {
    setter(prev => ({ ...prev, [key]: val }))
  }

  async function onSave(e: FormEvent) {
    e.preventDefault()
    if (!uid) return
    setStatus(null)
    setError(null)
    try {
      const updated = await api<AccountResponse>(`/api/v1/accounts/${uid}`, {
        method: 'PUT',
        body: JSON.stringify({
          username,
          shippingAddress: ship,
          billingAddress: bill,
        }),
      })
      setAccount(updated)
      setStatus('Saved')
    } catch (err: any) {
      setError(err.message || 'Update failed')
    }
  }

  if (!uid) return <div>Invalid token</div>
  if (!account) return <div>Loading...</div>

  const addrFields: (keyof AddressDto)[] = ['line1', 'line2', 'city', 'state', 'zip', 'country']

  return (
    <form onSubmit={onSave} style={{ display: 'grid', gap: 8, maxWidth: 520 }}>
      <h2>Profile</h2>
      <div>Email: {account.email}</div>
      <input placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} />

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

      <button type="submit">Save</button>
      {status && <div style={{ color: 'green' }}>{status}</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
    </form>
  )
}


