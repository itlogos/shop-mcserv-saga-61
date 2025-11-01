import React, { useEffect, useState } from 'react'
import Keycloak from 'keycloak-js'

const kc = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8085',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'shop',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT || 'frontend'
})

import Admin from './Admin.jsx'

export default function App(){
  const [token, setToken] = useState(null)
  const [tab, setTab] = useState('shop')
  const [products, setProducts] = useState([])

  useEffect(()=>{
    kc.init({ onLoad: 'login-required' }).then(auth => {
      if(auth){ setToken(kc.token); fetchProducts(kc.token); }
    })
  }, [])

  async function fetchProducts(tok){
    const res = await fetch((import.meta.env.VITE_API_BASE || 'http://localhost:8080') + '/store/api/products', {
      headers: { 'Authorization': 'Bearer ' + tok }
    })
    setProducts(await res.json())
  }

  async function orderOne(id){
    await fetch((import.meta.env.VITE_API_BASE || 'http://localhost:8080') + `/order/api/orders`, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token, 'Content-Type':'application/json' },
      body: JSON.stringify({ customerId: 1, items: [{ productId: id, quantity: 1, price: 0 }] })
    })
    alert('Order created; will confirm asynchronously')
  }

  
  const hasAdmin = kc.tokenParsed && (kc.tokenParsed.realm_access?.roles || []).includes('ROLE_ADMIN')

  return (

    <div style={{fontFamily:'sans-serif', maxWidth:900, margin:'30px auto'}}>
      <h1>Shop</h1><div style={{display:'flex', gap:8}}><button onClick={()=>kc.logout()}>Logout</button>{hasAdmin && <button onClick={()=>setTab('admin')}>Admin</button>}<button onClick={()=>setTab('shop')}>Shop</button></div>
      <button onClick={()=>kc.logout()}>Logout</button>
      {tab==='shop' && (<>
      <h2>Products</h2>
      <table border="1" cellPadding="6">
        <thead><tr><th>ID</th><th>Name</th><th>Price</th><th>Qty</th><th></th></tr></thead>
        <tbody>
          {products.map(p => (
            <tr key={p.id}>
              <td>{p.id}</td><td>{p.name}</td><td>{p.price}</td><td>{p.quantity}</td>
              <td><button onClick={()=>orderOne(p.id)} disabled={p.quantity<1}>Order 1</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      </>) }
      {tab==='admin' && hasAdmin && (<Admin token={token} />)}
    </div>
  )
}
