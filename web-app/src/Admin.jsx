import React, { useEffect, useState } from 'react'

export default function Admin({ token }){
  const api = (path) => (import.meta.env.VITE_API_BASE || 'http://localhost:8080') + path
  const h = { 'Authorization':'Bearer ' + token, 'Content-Type':'application/json' }

  const [products, setProducts] = useState([])
  const [customers, setCustomers] = useState([])
  const [p, setP] = useState({ name:'', price:'', quantity:'' })
  const [c, setC] = useState({ name:'', phone:'' })

  async function load(){
    const rp = await fetch(api('/store/api/products'), { headers: h })
    setProducts(await rp.json())
    const rc = await fetch(api('/customer/api/customers'), { headers: h })
    setCustomers(await rc.json())
  }

  useEffect(()=>{ load() }, [])

  async function addProduct(e){
    e.preventDefault()
    await fetch(api('/store/api/admin/products'), { method:'POST', headers: h, body: JSON.stringify({ ...p, price: +p.price, quantity: +p.quantity }) })
    setP({ name:'', price:'', quantity:'' }); await load()
  }

  async function delProduct(id){
    await fetch(api('/store/api/admin/products/'+id), { method:'DELETE', headers: h })
    await load()
  }

  async function addCustomer(e){
    e.preventDefault()
    await fetch(api('/customer/api/admin/customers'), { method:'POST', headers: h, body: JSON.stringify(c) })
    setC({ name:'', phone:'' }); await load()
  }

  async function delCustomer(id){
    await fetch(api('/customer/api/admin/customers/'+id), { method:'DELETE', headers: h })
    await load()
  }

  return (
    <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'24px'}}>
      <section>
        <h2>Products</h2>
        <form onSubmit={addProduct} style={{display:'flex', gap:8, marginBottom:12}}>
          <input placeholder="Name" value={p.name} onChange={e=>setP({...p,name:e.target.value})}/>
          <input placeholder="Price" type="number" step="0.01" value={p.price} onChange={e=>setP({...p,price:e.target.value})}/>
          <input placeholder="Qty" type="number" value={p.quantity} onChange={e=>setP({...p,quantity:e.target.value})}/>
          <button type="submit">Add</button>
        </form>
        <table border="1" cellPadding="6" width="100%">
          <thead><tr><th>ID</th><th>Name</th><th>Price</th><th>Qty</th><th/></tr></thead>
          <tbody>
            {products.map(x=>(
              <tr key={x.id}>
                <td>{x.id}</td><td>{x.name}</td><td>{x.price}</td><td>{x.quantity}</td>
                <td><button onClick={()=>delProduct(x.id)}>Delete</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      <section>
        <h2>Customers</h2>
        <form onSubmit={addCustomer} style={{display:'flex', gap:8, marginBottom:12}}>
          <input placeholder="Name" value={c.name} onChange={e=>setC({...c,name:e.target.value})}/>
          <input placeholder="Phone" value={c.phone} onChange={e=>setC({...c,phone:e.target.value})}/>
          <button type="submit">Add</button>
        </form>
        <table border="1" cellPadding="6" width="100%">
          <thead><tr><th>ID</th><th>Name</th><th>Phone</th><th/></tr></thead>
          <tbody>
            {customers.map(x=>(
              <tr key={x.id}>
                <td>{x.id}</td><td>{x.name}</td><td>{x.phone}</td>
                <td><button onClick={()=>delCustomer(x.id)}>Delete</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}
