import { useEffect, useState } from 'react'
import { api } from '../api/client'

type ItemDto = {
  id: string
  upc: string
  name: string
  unitPrice: number
  availableUnits: number
}

export default function Items() {
  const [items, setItems] = useState<ItemDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await api<ItemDto[]>('/api/v1/items')
        if (!ignore) setItems(data)
      } catch (err: any) {
        if (!ignore) setError(err.message || 'Failed to load items')
      } finally {
        if (!ignore) setLoading(false)
      }
    }
    load()
    return () => { ignore = true }
  }, [])

  if (loading) return <div>Loading...</div>
  if (error) return <div style={{ color: 'red' }}>{error}</div>

  if (!items.length) return <div>No items found.</div>

  return (
    <div>
      <h2>Items</h2>
      <table style={{ borderCollapse: 'collapse', width: '100%' }}>
        <thead>
          <tr>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>ID</th>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>Name</th>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>UPC</th>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>Price</th>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>Available Units</th>
          </tr>
        </thead>
        <tbody>
          {items.map(it => (
            <tr key={it.id}>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>{it.id}</td>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>{it.name}</td>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>{it.upc}</td>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>${Number(it.unitPrice).toFixed(2)}</td>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>{it.availableUnits}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}


