import React, {useEffect, useState, useMemo} from 'react'
import Admin from './Admin.jsx'

// === Keycloak config ===
const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8085'
const REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'shop'
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT || 'frontend'

const AUTH_URL =
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth`
const TOKEN_URL =
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`
const LOGOUT_URL =
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`

// ===== PKCE helpers =====
function base64UrlEncode(buffer) {
    const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer)
    let binary = ''
    for (let i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '')
}

async function createPkcePair() {
    const random = new Uint8Array(32)
    crypto.getRandomValues(random)

    const verifier = base64UrlEncode(random)

    const encoder = new TextEncoder()
    const data = encoder.encode(verifier)
    const digest = await crypto.subtle.digest('SHA-256', data)
    const challenge = base64UrlEncode(digest)

    return {verifier, challenge}
}

// ===== JWT helper (для ролей) =====
function parseJwt(token) {
    try {
        const parts = token.split('.')
        if (parts.length !== 3) return null
        const payload = parts[1]
            .replace(/-/g, '+')
            .replace(/_/g, '/')
        const json = atob(payload)
        return JSON.parse(json)
    } catch (e) {
        console.error('Failed to parse JWT', e)
        return null
    }
}

export default function App() {
    const [token, setToken] = useState(null)
    const [authenticated, setAuthenticated] = useState(false)
    const [ready, setReady] = useState(false)
    const [tab, setTab] = useState('shop')
    const [products, setProducts] = useState([])
    const [orders, setOrders] = useState([])           // NEW
    const [message, setMessage] = useState(null)       // { type: 'success' | 'error', text: string }
    const [isAdmin, setIsAdmin] = useState(false)

    // ===== API =====
    async function fetchProducts(tok) {
        try {
            const headers = {}
            if (tok) {
                headers.Authorization = 'Bearer ' + tok
            }

            const res = await fetch(
                (import.meta.env.VITE_API_BASE || 'http://localhost:8080') +
                '/store/api/products',
                {headers},
            )

            if (!res.ok) {
                console.error('Fetch products failed, status =', res.status)
                return
            }
            const data = await res.json()
            setProducts(data)
        } catch (e) {
            console.error('Fetch products error:', e)
        }
    }

    async function fetchOrders(tok) {
        try {
            if (!tok) {
                setOrders([])
                return
            }

            const res = await fetch(
                (import.meta.env.VITE_API_BASE || 'http://localhost:8080') +
                '/order/api/orders?customerId=1',
                {
                    headers: {
                        Authorization: 'Bearer ' + tok,
                    },
                },
            )

            if (!res.ok) {
                console.error('Fetch orders failed, status =', res.status)
                return
            }

            const data = await res.json()
            setOrders(Array.isArray(data) ? data : [])
        } catch (e) {
            console.error('Fetch orders error', e)
        }
    }


    async function orderOne(id) {
        if (!token) return

        try {
            const res = await fetch(
                (import.meta.env.VITE_API_BASE || 'http://localhost:8080') +
                `/order/api/orders`,
                {
                    method: 'POST',
                    headers: {
                        Authorization: 'Bearer ' + token,
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        customerId: 1,
                        items: [{productId: id, quantity: 1, price: 0}],
                    }),
                },
            )

            if (!res.ok) {
                setMessage({type: 'error', text: 'Failed to create order'})
                return
            }

            const created = await res.json()

            // обновляем список заказов: добавим/заменим заказ
            setOrders((prev) => {
                const existing = prev.find((o) => o.id === created.id)
                if (existing) {
                    return prev.map((o) => (o.id === created.id ? created : o))
                }
                return [...prev, created]
            })

            // оптимистично уменьшаем количество на складе
            setProducts((prev) =>
                prev.map((p) =>
                    p.id === id
                        ? {...p, quantity: (p.quantity ?? 0) - 1}
                        : p,
                ),
            )

            setMessage({type: 'success', text: 'Order created successfully'})
        } catch (e) {
            console.error('Order error', e)
            setMessage({type: 'error', text: 'Unexpected error while creating order'})
        }
    }

    async function returnOne(orderId, productId) {
        if (!token) return

        try {
            const res = await fetch(
                (import.meta.env.VITE_API_BASE || 'http://localhost:8080') +
                `/order/api/orders/${orderId}/items/${productId}/return-one`,
                {
                    method: 'POST',
                    headers: {
                        Authorization: 'Bearer ' + token,
                    },
                },
            )

            if (!res.ok) {
                setMessage({type: 'error', text: 'Failed to return item'})
                return
            }

            const updatedOrder = await res.json()

            setOrders((prev) => {
                const replaced = prev.map((o) =>
                    o.id === updatedOrder.id ? updatedOrder : o,
                )
                // удаляем заказы без позиций, если такое возможно
                return replaced.filter((o) => o.items && o.items.length > 0)
            })

            // при возврате можно сразу добавить 1 на склад
            setProducts((prev) =>
                prev.map((p) =>
                    p.id === productId
                        ? {...p, quantity: (p.quantity ?? 0) + 1}
                        : p,
                ),
            )

            setMessage({type: 'success', text: 'Item successfully returned'})
        } catch (e) {
            console.error('Return error', e)
            setMessage({type: 'error', text: 'Unexpected error while returning item'})
        }
    }


    // ===== Ручная обработка возвращённого code из Keycloak =====
    useEffect(() => {
        const doAuthFlow = async () => {
            console.log('APP START URL:', window.location.href)

            const url = new URL(window.location.href)
            let code = url.searchParams.get('code')

            // NEW: если нет code, но есть сохранённый токен – поднимаем сессию
            if (!code) {
                const savedToken = window.sessionStorage.getItem('access_token')
                if (savedToken) {
                    setToken(savedToken)
                    setAuthenticated(true)

                    const payload = parseJwt(savedToken)
                    const realmRoles = payload?.realm_access?.roles || []
                    setIsAdmin(realmRoles.includes('ADMIN'))

                    await fetchProducts(savedToken)
                    await fetchOrders(savedToken)
                    setReady(true)
                    return
                }
            }

            if (!code) {
                const rawHash = window.location.hash.startsWith('#')
                    ? window.location.hash.substring(1)
                    : window.location.hash

                const hashParams = new URLSearchParams(rawHash)
                code = hashParams.get('code')
            }

            if (code) {
                console.log('Found authorization code in URL, exchanging for token...')

                const body = new URLSearchParams()
                body.set('grant_type', 'authorization_code')
                body.set('client_id', CLIENT_ID)
                body.set('code', code)
                body.set('redirect_uri', window.location.origin + '/')

                const verifier = window.sessionStorage.getItem('pkce_verifier')
                if (verifier) {
                    body.set('code_verifier', verifier)
                }

                try {
                    const res = await fetch(TOKEN_URL, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                        },
                        body: body.toString(),
                    })

                    if (!res.ok) {
                        console.error('Token endpoint error, status =', res.status)
                        setAuthenticated(false)
                        setToken(null)
                        setIsAdmin(false)
                    } else {
                        const data = await res.json()
                        console.log('TOKEN RESPONSE', data)

                        const accessToken = data.access_token

                        if (accessToken) {
                            // 1) сохраняем токен и флаг авторизации
                            setToken(accessToken)
                            setAuthenticated(true)
                            window.sessionStorage.setItem('access_token', accessToken)

                            // 2) разбираем JWT и достаём роли
                            const payload = parseJwt(accessToken)
                            const realmRoles =
                                payload?.realm_access?.roles || []

                            setIsAdmin(realmRoles.includes('ADMIN'))

                            // 3) подгружаем данные для авторизованного пользователя
                            await fetchProducts(accessToken)
                            await fetchOrders(accessToken)
                        } else {
                            setAuthenticated(false)
                            setToken(null)
                            setIsAdmin(false)
                        }
                    }
                } catch (e) {
                    console.error('Token request failed:', e)
                    setAuthenticated(false)
                    setToken(null)
                    setIsAdmin(false)
                } finally {
                    window.sessionStorage.removeItem('pkce_verifier')
                    window.sessionStorage.removeItem('oauth_state')

                    window.history.replaceState(
                        null,
                        '',
                        window.location.origin + window.location.pathname,
                    )
                    setReady(true)
                }

                // ВАЖНО: не переходить в гостевой код после успешной обработки code
                return
            }

            // ===== Без code и без сохранённого токена — гость =====
            setAuthenticated(false)
            setToken(null)
            setIsAdmin(false)

            // если backend разрешает анонимный GET /store/api/products – подтянет товары
            await fetchProducts(null)
            setOrders([]) // нет авторизации – нет заказов

            setReady(true)
        }

        doAuthFlow()
    }, [])


    // агрегированное представление "My orders" по продукту
    const aggregatedOrders = React.useMemo(() => {
        if (!orders || orders.length === 0) return []

        const rows = []

        for (const o of orders) {
            for (const item of o.items || []) {
                const productId = item.productId
                if (productId == null) continue

                let row = rows.find((r) => r.productId === productId)
                if (!row) {
                    const product = (products || []).find((p) => p.id === productId)
                    row = {
                        productId,
                        productName: product ? product.name : `Product ${productId}`,
                        quantity: 0,
                        // будем хранить список заказов, где встретился продукт
                        orderIds: [],
                        // "агрегированный" статус – пока пусть будет статус последнего заказа
                        status: o.status || 'CREATED',
                    }
                    rows.push(row)
                }

                row.quantity += item.quantity ?? 0
                row.orderIds.push(o.id)
                // при желании можно усложнить логику:
                // например, если хоть один FAILED — показывать FAILED и т.п.
                row.status = o.status || row.status
            }
        }

        return rows
    }, [orders, products])

    async function returnAggregatedOne(productId) {
        if (!token) return

        // ищем любой Order, где есть этот productId с qty > 0
        const entry = orders
            .flatMap((o) =>
                (o.items || []).map((item) => ({
                    order: o,
                    item,
                })),
            )
            .find(
                (e) =>
                    e.item.productId === productId &&
                    (e.item.quantity ?? 0) > 0,
            )

        if (!entry) {
            return
        }

        await returnOne(entry.order.id, productId)
    }

    // ===== Login / Logout =====

    const doLogin = async () => {
        const state = crypto.randomUUID()
        const redirectUri = encodeURIComponent(window.location.origin + '/')

        const {verifier, challenge} = await createPkcePair()

        window.sessionStorage.setItem('pkce_verifier', verifier)
        window.sessionStorage.setItem('oauth_state', state)

        const url =
            `${AUTH_URL}?client_id=${encodeURIComponent(CLIENT_ID)}` +
            `&response_type=code` +
            `&scope=openid%20profile%20email` +
            `&redirect_uri=${redirectUri}` +
            `&state=${encodeURIComponent(state)}` +
            `&code_challenge=${encodeURIComponent(challenge)}` +
            `&code_challenge_method=S256`

        window.location.href = url
    }

    const doLogout = () => {
        // чистим сохранённые данные сессии
        window.sessionStorage.removeItem('access_token')
        window.sessionStorage.removeItem('pkce_verifier')
        window.sessionStorage.removeItem('oauth_state')

        const redirectUri = encodeURIComponent(window.location.origin + '/')
        const url =
            `${LOGOUT_URL}?client_id=${encodeURIComponent(
                CLIENT_ID,
            )}&post_logout_redirect_uri=${redirectUri}`

        window.location.href = url
    }

    // ===== UI =====

    if (!ready) {
        return (
            <div style={{fontFamily: 'sans-serif', maxWidth: 900, margin: '30px auto'}}>
                <h1>Shop</h1>
                <p>Waiting for authentication...</p>
            </div>
        )
    }

    const hasProducts = products && products.length > 0
    const columnsCount = authenticated ? 5 : 4

    return (
        <div style={{fontFamily: 'sans-serif', maxWidth: 900, margin: '30px auto'}}>
            <h1>Shop</h1>

            {message && (
                <div
                    style={{
                        marginBottom: 12,
                        padding: '8px 12px',
                        borderRadius: 4,
                        border: '0px solid',
                        borderColor:
                            message.type === 'error' ? '#f5a3a3' : '#8dd7a5',
                        backgroundColor:
                            message.type === 'error' ? '#ffe6e6' : '#e6ffed',
                        color: '#333',
                        fontSize: 14,
                    }}
                >
                    {message.text}
                </div>
            )}


            <div style={{display: 'flex', gap: 8, marginBottom: 10}}>
                {authenticated && (
                    <button onClick={doLogout}>
                        Logout
                    </button>
                )}

                <button onClick={() => setTab('shop')}>Shop</button>

                {/* Admin виден только если есть роль ADMIN */}
                {isAdmin && (
                    <button onClick={() => setTab('admin')}>Admin</button>
                )}
            </div>

            {!authenticated && (
                <div style={{marginBottom: 10}}>
                    <b>You are not logged in.</b>{' '}
                    <button onClick={doLogin}>Login</button>
                </div>
            )}

            {tab === 'shop' && (
                <>
                    <h2>Products</h2>
                    <table border="1" cellPadding="6">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Price</th>
                            <th>Qty</th>
                            {authenticated && <th>Action</th>}
                        </tr>
                        </thead>
                        <tbody>
                        {hasProducts ? (
                            products.map((p) => (
                                <tr key={p.id}>
                                    <td>{p.id}</td>
                                    <td>{p.name}</td>
                                    <td>{p.price}</td>
                                    <td>{p.quantity}</td>
                                    {authenticated && (
                                        <td>
                                            <button
                                                onClick={() => orderOne(p.id)}
                                                disabled={!token || p.quantity < 1}
                                            >
                                                Order 1
                                            </button>
                                        </td>
                                    )}
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={columnsCount}>No products available</td>
                            </tr>
                        )}
                        </tbody>
                    </table>

                    {authenticated && (
                        <>
                            <h2 style={{marginTop: 24}}>My orders</h2>
                            <table
                                border="0"
                                cellPadding="6"
                                style={{
                                    width: '80%',
                                    tableLayout: 'fixed',
                                }}
                            >
                                <thead>
                                <tr>
                                    <th style={{width: '50%'}}>Product</th>
                                    <th style={{width: '20%'}}>Status</th>
                                    <th style={{width: '15%'}}>Qty</th>
                                    <th style={{width: '15%'}}>Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                {aggregatedOrders.length > 0 ? (
                                    aggregatedOrders.map((row) => (
                                        <tr key={row.productId}>
                                            <td
                                                style={{
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                }}
                                            >
                                                {row.productName}
                                            </td>
                                            <td>{row.status}</td>
                                            <td>{row.quantity}</td>
                                            <td>
                                                <button
                                                    onClick={() => returnAggregatedOne(row.productId)}
                                                    disabled={!token || row.quantity < 1}
                                                >
                                                    Return 1
                                                </button>
                                            </td>
                                        </tr>
                                    ))
                                ) : (
                                    <tr>
                                        <td colSpan={4}>No orders yet</td>
                                    </tr>
                                )}
                                </tbody>
                            </table>

                        </>
                    )}
                </>
            )}

            {tab === 'admin' && authenticated && isAdmin && (
                <Admin token={token}/>
            )}
        </div>
    )
}
